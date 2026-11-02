package com.volta.api.service;

import com.volta.api.database.entity.Collection;
import com.volta.api.database.entity.Cooperative;
import com.volta.api.database.entity.Incident;
import com.volta.api.database.function.CollectionFunction;
import com.volta.api.database.procedure.CollectionProcedure;
import com.volta.api.database.procedure.IncidentProcedure;
import com.volta.api.database.repository.CollectionRepository;
import com.volta.api.database.repository.CollectionStatusRepository;
import com.volta.api.database.repository.CooperativeRepository;
import com.volta.api.database.repository.IncidentRepository;
import com.volta.api.dto.collection.CollectionRequestDTO;
import com.volta.api.dto.collection.CollectionScheduleRequestDTO;
import com.volta.api.dto.collection.CollectionStatusRequestDTO;
import com.volta.api.dto.collection.CollectionCompletionTimeResponseDTO;
import com.volta.api.dto.collection.CollectionResponseDTO;
import com.volta.api.enums.CollectionStatusType;
import com.volta.api.enums.IncidentStatus;
import com.volta.api.enums.Priority;
import com.volta.api.enums.RiskLevel;
import com.volta.api.enums.WasteCategory;
import com.volta.api.exception.BusinessRuleException;
import com.volta.api.exception.ConflictException;
import com.volta.api.exception.InvalidRequestException;
import com.volta.api.exception.ResourceNotFoundException;
import com.volta.api.mapper.CollectionMapper;
import com.volta.api.mapper.CollectionStatusMapper;
import com.volta.api.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.volta.api.support.CollectionFixtures.collection;
import static com.volta.api.support.CollectionFixtures.cooperative;
import static com.volta.api.support.CollectionFixtures.incident;
import static com.volta.api.support.CollectionFixtures.wasteType;
import static com.volta.api.support.UserFixtures.authenticatedUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import com.volta.api.database.entity.CollectionStatus;
import com.volta.api.database.entity.WasteType;
import com.volta.api.dto.collection.CollectionStatusResponseDTO;

@ExtendWith(MockitoExtension.class)
@DisplayName("CollectionService")
class CollectionServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 10, 12, 0);

    @Mock
    private CollectionRepository collectionRepository;

    @Mock
    private CollectionProcedure collectionProcedure;

    @Mock
    private CollectionFunction collectionFunction;

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private IncidentProcedure incidentProcedure;

    @Mock
    private CooperativeRepository cooperativeRepository;

    @Mock
    private CollectionStatusRepository collectionStatusRepository;

    @Mock
    private NotificationPublisher notificationPublisher;

    private CollectionService service;

    private final UUID companyId = UUID.randomUUID();

    private final AuthenticatedUser author = authenticatedUser(UUID.randomUUID(), companyId, "OPERATOR");

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        service = new CollectionService(
                collectionRepository,
                collectionProcedure,
                collectionFunction,
                incidentRepository,
                incidentProcedure,
                cooperativeRepository,
                new CollectionMapper(),
                collectionStatusRepository,
                new CollectionStatusMapper(),
                fixedClock,
                notificationPublisher);
    }

    private Incident pendingIncident(WasteCategory category, RiskLevel riskLevel, Priority priority) {
        return incident(wasteType(category, riskLevel), priority, IncidentStatus.PENDING);
    }

    private void givenOwnedCollection(Collection collection) {
        when(collectionRepository.findByIdAndIncidentCompanyId(collection.getId(), companyId))
                .thenReturn(Optional.of(collection));
    }

    private CollectionRequestDTO requestFor(Incident incident, Cooperative cooperative, boolean urgent) {
        return new CollectionRequestDTO(incident.getId(), cooperative.getId(), "Recyclable", urgent);
    }

    private void givenIncidentAndCooperative(Incident incident, Cooperative cooperative) {
        when(incidentRepository.findByIdAndCompanyId(incident.getId(), companyId)).thenReturn(Optional.of(incident));
        when(cooperativeRepository.findById(cooperative.getId())).thenReturn(Optional.of(cooperative));
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("incidente inexistente lança not found")
        void register_missingIncident_throwsNotFound() {
            UUID incidentId = UUID.randomUUID();
            when(incidentRepository.findByIdAndCompanyId(incidentId, companyId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.register(
                    new CollectionRequestDTO(incidentId, UUID.randomUUID(), "Recyclable", false), author))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Incident not found");

            verify(collectionRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("cooperativa inexistente lança not found")
        void register_missingCooperative_throwsNotFound() {
            Incident incident = pendingIncident(WasteCategory.PLASTICO, RiskLevel.LOW, Priority.MEDIUM);
            UUID cooperativeId = UUID.randomUUID();
            when(incidentRepository.findByIdAndCompanyId(incident.getId(), companyId)).thenReturn(Optional.of(incident));
            when(cooperativeRepository.findById(cooperativeId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.register(
                    new CollectionRequestDTO(incident.getId(), cooperativeId, "Recyclable", false), author))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Cooperative not found");
        }

        @Test
        @DisplayName("incidente fechado lança regra de negócio")
        void register_closedIncident_throwsBusinessRule() {
            Incident incident = incident(wasteType(WasteCategory.PLASTICO, RiskLevel.LOW), Priority.MEDIUM, IncidentStatus.CLOSED);
            Cooperative cooperative = cooperative("PLASTICO");
            givenIncidentAndCooperative(incident, cooperative);

            assertThatThrownBy(() -> service.register(requestFor(incident, cooperative, false), author))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Cannot request a collection for a closed incident");

            verify(collectionRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("incidente recusado lança regra de negócio")
        void register_rejectedIncident_throwsBusinessRule() {
            Incident incident = incident(wasteType(WasteCategory.PLASTICO, RiskLevel.LOW), Priority.MEDIUM, IncidentStatus.REJECTED);
            Cooperative cooperative = cooperative("PLASTICO");
            givenIncidentAndCooperative(incident, cooperative);

            assertThatThrownBy(() -> service.register(requestFor(incident, cooperative, false), author))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Cannot request a collection for a rejected incident");

            verify(collectionRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("incidente com coleta ativa lança conflito")
        void register_incidentWithActiveCollection_throwsConflict() {
            Incident incident = pendingIncident(WasteCategory.PLASTICO, RiskLevel.LOW, Priority.MEDIUM);
            Cooperative cooperative = cooperative("PLASTICO");
            givenIncidentAndCooperative(incident, cooperative);
            when(collectionRepository.existsByIncidentIdAndCurrentStatusIn(
                    incident.getId(), CollectionStatusType.ACTIVE_STATUSES)).thenReturn(true);

            assertThatThrownBy(() -> service.register(requestFor(incident, cooperative, false), author))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Incident already has an active collection");

            verify(collectionRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("incidente sem tipo de resíduo lança regra de negócio")
        void register_incidentWithoutWasteType_throwsBusinessRule() {
            Incident incident = incident(null, Priority.MEDIUM, IncidentStatus.PENDING);
            Cooperative cooperative = cooperative("PLASTICO");
            givenIncidentAndCooperative(incident, cooperative);

            assertThatThrownBy(() -> service.register(requestFor(incident, cooperative, false), author))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Incident has no waste type to be collected");
        }

        @Test
        @DisplayName("resíduo radioativo não pode ir para cooperativa")
        void register_radioactiveWaste_throwsBusinessRule() {
            Incident incident = pendingIncident(WasteCategory.RADIOATIVO, RiskLevel.HIGH, Priority.HIGH);
            Cooperative cooperative = cooperative("RADIOATIVO,PERIGOSO");
            givenIncidentAndCooperative(incident, cooperative);

            assertThatThrownBy(() -> service.register(requestFor(incident, cooperative, false), author))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("CNEN");
        }

        @Test
        @DisplayName("cooperativa que não atende a categoria lança regra de negócio")
        void register_cooperativeWithoutSpecialty_throwsBusinessRule() {
            Incident incident = pendingIncident(WasteCategory.PLASTICO, RiskLevel.LOW, Priority.MEDIUM);
            Cooperative cooperative = cooperative("PAPEL,VIDRO");
            givenIncidentAndCooperative(incident, cooperative);

            assertThatThrownBy(() -> service.register(requestFor(incident, cooperative, false), author))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Cooperative does not handle PLASTICO waste");
        }

        @Test
        @DisplayName("resíduo de risco HIGH exige cooperativa habilitada para PERIGOSO")
        void register_highRiskWasteWithoutHazardousSpecialty_throwsBusinessRule() {
            Incident incident = pendingIncident(WasteCategory.METAL, RiskLevel.HIGH, Priority.HIGH);
            Cooperative cooperative = cooperative("METAL");
            givenIncidentAndCooperative(incident, cooperative);

            assertThatThrownBy(() -> service.register(requestFor(incident, cooperative, false), author))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("requires a cooperative qualified for PERIGOSO waste");
        }

        @Test
        @DisplayName("dados válidos salvam a coleta como REQUESTED e registram o status")
        void register_validData_savesRequestedCollectionAndRecordsStatus() {
            Incident incident = pendingIncident(WasteCategory.PLASTICO, RiskLevel.LOW, Priority.MEDIUM);
            Cooperative cooperative = cooperative("PLASTICO,PAPEL");
            givenIncidentAndCooperative(incident, cooperative);
            UUID savedId = UUID.randomUUID();
            when(collectionRepository.saveAndFlush(any(Collection.class))).thenAnswer(invocation -> {
                Collection saved = invocation.getArgument(0);
                saved.setId(savedId);
                return saved;
            });

            CollectionResponseDTO response = service.register(requestFor(incident, cooperative, false), author);

            assertThat(response.id()).isEqualTo(savedId);
            assertThat(response.currentStatus()).isEqualTo("REQUESTED");
            assertThat(response.urgent()).isFalse();
            verify(collectionProcedure).updateCollectionStatus(savedId, "REQUESTED", "Collection Requested");
        }

        @Test
        @DisplayName("incidente CRITICAL sempre gera coleta urgente")
        void register_criticalIncident_forcesUrgentCollection() {
            Incident incident = pendingIncident(WasteCategory.PLASTICO, RiskLevel.LOW, Priority.CRITICAL);
            Cooperative cooperative = cooperative("PLASTICO");
            givenIncidentAndCooperative(incident, cooperative);
            when(collectionRepository.saveAndFlush(any(Collection.class))).thenAnswer(invocation -> invocation.getArgument(0));

            service.register(requestFor(incident, cooperative, false), author);

            ArgumentCaptor<Collection> captor = ArgumentCaptor.forClass(Collection.class);
            verify(collectionRepository).saveAndFlush(captor.capture());
            assertThat(captor.getValue().isUrgent()).isTrue();
        }

        @Test
        @DisplayName("incidente não crítico respeita o urgente informado")
        void register_nonCriticalIncident_keepsInformedUrgency() {
            Incident incident = pendingIncident(WasteCategory.PLASTICO, RiskLevel.LOW, Priority.LOW);
            Cooperative cooperative = cooperative("PLASTICO");
            givenIncidentAndCooperative(incident, cooperative);
            when(collectionRepository.saveAndFlush(any(Collection.class))).thenAnswer(invocation -> invocation.getArgument(0));

            service.register(requestFor(incident, cooperative, true), author);

            ArgumentCaptor<Collection> captor = ArgumentCaptor.forClass(Collection.class);
            verify(collectionRepository).saveAndFlush(captor.capture());
            assertThat(captor.getValue().isUrgent()).isTrue();
        }
    }

    @Nested
    @DisplayName("getCollections e getCollectionById")
    class Queries {

        @Test
        @DisplayName("lista somente as coletas da empresa do autor")
        void getCollections_returnsCollectionsOfAuthorCompany() {
            Incident incident = pendingIncident(WasteCategory.PLASTICO, RiskLevel.LOW, Priority.MEDIUM);
            Collection first = collection(incident, cooperative("PLASTICO"), CollectionStatusType.REQUESTED, NOW, false);
            Collection second = collection(incident, cooperative("PLASTICO"), CollectionStatusType.SCHEDULED, NOW, false);
            when(collectionRepository.findByIncidentCompanyId(companyId)).thenReturn(List.of(first, second));

            List<CollectionResponseDTO> response = service.getCollections(author);

            assertThat(response).extracting(CollectionResponseDTO::id)
                    .containsExactly(first.getId(), second.getId());
        }

        @Test
        @DisplayName("coleta de outra empresa lança not found")
        void getCollectionById_ofAnotherCompany_throwsNotFound() {
            UUID id = UUID.randomUUID();
            when(collectionRepository.findByIdAndIncidentCompanyId(id, companyId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getCollectionById(id, author))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Collection not found");
        }

        @Test
        @DisplayName("histórico de status de coleta de outra empresa lança not found sem consultar status")
        void getCollectionStatus_ofAnotherCompany_throwsNotFoundWithoutQueryingStatuses() {
            UUID id = UUID.randomUUID();
            when(collectionRepository.findByIdAndIncidentCompanyId(id, companyId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getCollectionStatus(id, author))
                    .isInstanceOf(ResourceNotFoundException.class);

            verifyNoInteractions(collectionStatusRepository);
        }
    }

    @Nested
    @DisplayName("schedule")
    class Schedule {

        private Collection requestedCollection(Priority priority, LocalDateTime requestedAt, boolean urgent) {
            Incident incident = pendingIncident(WasteCategory.PLASTICO, RiskLevel.LOW, priority);
            Collection collection = collection(
                    incident, cooperative("PLASTICO"), CollectionStatusType.REQUESTED, requestedAt, urgent);
            givenOwnedCollection(collection);
            return collection;
        }

        @Test
        @DisplayName("coleta inexistente lança not found")
        void schedule_missingCollection_throwsNotFound() {
            UUID id = UUID.randomUUID();
            when(collectionRepository.findByIdAndIncidentCompanyId(id, companyId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.schedule(id, new CollectionScheduleRequestDTO(NOW.plusDays(1)), author))
                    .isInstanceOf(ResourceNotFoundException.class);

            verifyNoInteractions(collectionProcedure);
        }

        @Test
        @DisplayName("coleta concluída não pode ser agendada")
        void schedule_completedCollection_throwsInvalidTransition() {
            Collection collection = collection(
                    pendingIncident(WasteCategory.PLASTICO, RiskLevel.LOW, Priority.MEDIUM),
                    cooperative("PLASTICO"), CollectionStatusType.COMPLETED, NOW.minusDays(1), false);
            givenOwnedCollection(collection);

            assertThatThrownBy(() -> service.schedule(
                    collection.getId(), new CollectionScheduleRequestDTO(NOW.plusDays(1)), author))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Invalid status transition from COMPLETED to SCHEDULED");

            verifyNoInteractions(collectionProcedure);
        }

        @Test
        @DisplayName("dentro do prazo da prioridade agenda pela procedure")
        void schedule_withinPriorityDeadline_schedulesThroughProcedure() {
            Collection collection = requestedCollection(Priority.MEDIUM, NOW.minusHours(1), false);
            LocalDateTime scheduledAt = NOW.plusDays(2);

            service.schedule(collection.getId(), new CollectionScheduleRequestDTO(scheduledAt), author);

            verify(collectionProcedure).scheduleCollection(collection.getId(), scheduledAt);
        }

        @Test
        @DisplayName("exatamente no limite do prazo ainda é aceito")
        void schedule_exactlyOnDeadline_isAccepted() {
            Collection collection = requestedCollection(Priority.HIGH, NOW, false);
            LocalDateTime scheduledAt = NOW.plusHours(72);

            service.schedule(collection.getId(), new CollectionScheduleRequestDTO(scheduledAt), author);

            verify(collectionProcedure).scheduleCollection(collection.getId(), scheduledAt);
        }

        @Test
        @DisplayName("depois do prazo da prioridade lança regra de negócio informando o limite")
        void schedule_afterPriorityDeadline_throwsBusinessRuleWithLimit() {
            Collection collection = requestedCollection(Priority.HIGH, NOW, false);

            assertThatThrownBy(() -> service.schedule(
                    collection.getId(), new CollectionScheduleRequestDTO(NOW.plusHours(73)), author))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Collection must be scheduled until 2026-10-13T12:00");

            verifyNoInteractions(collectionProcedure);
        }

        @Test
        @DisplayName("coleta urgente usa o prazo de 24 horas mesmo com prioridade baixa")
        void schedule_urgentCollection_usesUrgentDeadline() {
            Collection collection = requestedCollection(Priority.LOW, NOW, true);

            assertThatThrownBy(() -> service.schedule(
                    collection.getId(), new CollectionScheduleRequestDTO(NOW.plusHours(25)), author))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Collection must be scheduled until 2026-10-11T12:00");
        }

        @Test
        @DisplayName("prazo vencido abre regime de urgência de 24 horas a partir de agora")
        void schedule_expiredDeadline_allowsUrgentWindowFromNow() {
            Collection collection = requestedCollection(Priority.HIGH, NOW.minusDays(10), false);
            LocalDateTime scheduledAt = NOW.plusHours(23);

            service.schedule(collection.getId(), new CollectionScheduleRequestDTO(scheduledAt), author);

            verify(collectionProcedure).scheduleCollection(collection.getId(), scheduledAt);
        }

        @Test
        @DisplayName("prazo vencido rejeita agendamento além das 24 horas de urgência")
        void schedule_expiredDeadline_rejectsBeyondUrgentWindow() {
            Collection collection = requestedCollection(Priority.HIGH, NOW.minusDays(10), false);

            assertThatThrownBy(() -> service.schedule(
                    collection.getId(), new CollectionScheduleRequestDTO(NOW.plusHours(25)), author))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Collection must be scheduled until 2026-10-11T12:00");
        }
    }

    @Nested
    @DisplayName("updateStatus")
    class UpdateStatus {

        private Collection collectionIn(CollectionStatusType status, IncidentStatus incidentStatus) {
            Incident incident = incident(
                    wasteType(WasteCategory.PLASTICO, RiskLevel.LOW), Priority.MEDIUM, incidentStatus);
            Collection collection = collection(incident, cooperative("PLASTICO"), status, NOW.minusDays(1), false);
            givenOwnedCollection(collection);
            return collection;
        }

        @Test
        @DisplayName("SCHEDULED deve usar o endpoint de agendamento")
        void updateStatus_scheduled_throwsUseScheduleEndpoint() {
            Collection collection = collectionIn(CollectionStatusType.REQUESTED, IncidentStatus.PENDING);

            assertThatThrownBy(() -> service.updateStatus(
                    collection.getId(), new CollectionStatusRequestDTO(CollectionStatusType.SCHEDULED, null), author))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Use the schedule endpoint to schedule a collection");

            verifyNoInteractions(collectionProcedure, incidentProcedure);
        }

        @Test
        @DisplayName("transição inválida lança regra de negócio")
        void updateStatus_invalidTransition_throwsBusinessRule() {
            Collection collection = collectionIn(CollectionStatusType.REQUESTED, IncidentStatus.PENDING);

            assertThatThrownBy(() -> service.updateStatus(
                    collection.getId(), new CollectionStatusRequestDTO(CollectionStatusType.COMPLETED, null), author))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Invalid status transition from REQUESTED to COMPLETED");

            verifyNoInteractions(collectionProcedure, incidentProcedure);
        }

        @ParameterizedTest(name = "observação \"{0}\" é rejeitada")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("cancelar sem observação lança requisição inválida")
        void updateStatus_cancelWithoutObservation_throwsInvalidRequest(String observation) {
            Collection collection = collectionIn(CollectionStatusType.REQUESTED, IncidentStatus.PENDING);

            assertThatThrownBy(() -> service.updateStatus(
                    collection.getId(), new CollectionStatusRequestDTO(CollectionStatusType.CANCELED, observation), author))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessage("Observation is required to cancel a collection");

            verifyNoInteractions(collectionProcedure);
        }

        @Test
        @DisplayName("cancelar com observação atualiza o status sem fechar o incidente")
        void updateStatus_cancelWithObservation_updatesStatusWithoutClosingIncident() {
            Collection collection = collectionIn(CollectionStatusType.REQUESTED, IncidentStatus.PENDING);

            service.updateStatus(
                    collection.getId(), new CollectionStatusRequestDTO(CollectionStatusType.CANCELED, "Requested by client"), author);

            verify(collectionProcedure).updateCollectionStatus(collection.getId(), "CANCELED", "Requested by client");
            verifyNoInteractions(incidentProcedure);
        }

        @Test
        @DisplayName("iniciar a coleta atualiza o status sem fechar o incidente")
        void updateStatus_inProgress_updatesStatusWithoutClosingIncident() {
            Collection collection = collectionIn(CollectionStatusType.SCHEDULED, IncidentStatus.PENDING);

            service.updateStatus(
                    collection.getId(), new CollectionStatusRequestDTO(CollectionStatusType.IN_PROGRESS, null), author);

            verify(collectionProcedure).updateCollectionStatus(collection.getId(), "IN_PROGRESS", null);
            verifyNoInteractions(incidentProcedure);
        }

        @Test
        @DisplayName("concluir a coleta fecha o incidente que a originou")
        void updateStatus_completed_closesOriginatingIncident() {
            Collection collection = collectionIn(CollectionStatusType.IN_PROGRESS, IncidentStatus.PENDING);

            service.updateStatus(
                    collection.getId(), new CollectionStatusRequestDTO(CollectionStatusType.COMPLETED, null), author);

            verify(collectionProcedure).updateCollectionStatus(collection.getId(), "COMPLETED", null);
            verify(incidentProcedure).closeIncident(collection.getIncident().getId());
        }

        @Test
        @DisplayName("concluir com incidente já fechado não fecha de novo")
        void updateStatus_completedWithClosedIncident_doesNotCloseAgain() {
            Collection collection = collectionIn(CollectionStatusType.IN_PROGRESS, IncidentStatus.CLOSED);

            service.updateStatus(
                    collection.getId(), new CollectionStatusRequestDTO(CollectionStatusType.COMPLETED, null), author);

            verify(collectionProcedure).updateCollectionStatus(collection.getId(), "COMPLETED", null);
            verifyNoInteractions(incidentProcedure);
        }
    }

    @Nested
    @DisplayName("getCompletionTime")
    class GetCompletionTime {

        @Test
        @DisplayName("coleta não concluída lança regra de negócio")
        void getCompletionTime_notCompleted_throwsBusinessRule() {
            Collection collection = collection(
                    pendingIncident(WasteCategory.PLASTICO, RiskLevel.LOW, Priority.MEDIUM),
                    cooperative("PLASTICO"), CollectionStatusType.IN_PROGRESS, NOW.minusDays(1), false);
            givenOwnedCollection(collection);

            assertThatThrownBy(() -> service.getCompletionTime(collection.getId(), author))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Collection is not completed yet");

            verifyNoInteractions(collectionFunction);
        }

        @Test
        @DisplayName("coleta concluída devolve as horas calculadas pelo banco")
        void getCompletionTime_completed_returnsHoursFromDatabaseFunction() {
            Collection collection = collection(
                    pendingIncident(WasteCategory.PLASTICO, RiskLevel.LOW, Priority.MEDIUM),
                    cooperative("PLASTICO"), CollectionStatusType.COMPLETED, NOW.minusDays(2), false);
            givenOwnedCollection(collection);
            when(collectionFunction.calculateCollectionCompletionHours(collection.getId()))
                    .thenReturn(new BigDecimal("36.50"));

            CollectionCompletionTimeResponseDTO response = service.getCompletionTime(collection.getId(), author);

            assertThat(response.collectionId()).isEqualTo(collection.getId());
            assertThat(response.completionHours()).isEqualByComparingTo("36.50");
        }
    }

    @Nested
    @DisplayName("register (casos adicionais)")
    class RegisterExtra {

        @Test
        @DisplayName("categoria fora do padrão CONAMA 275 lança regra de negócio")
        void register_nonConamaCategory_throwsBusinessRule() {
            WasteType unknown = new WasteType();
            unknown.setId(UUID.randomUUID());
            unknown.setCategory("UNKNOWN_CATEGORY");
            unknown.setDefaultRiskLevel(RiskLevel.LOW.name());
            Incident incident = incident(unknown, Priority.MEDIUM, IncidentStatus.PENDING);
            Cooperative cooperative = cooperative("PLASTICO");
            givenIncidentAndCooperative(incident, cooperative);

            assertThatThrownBy(() -> service.register(requestFor(incident, cooperative, false), author))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Waste type category 'UNKNOWN_CATEGORY' is not a CONAMA 275 category");

            verify(collectionRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("resíduo de risco HIGH com cooperativa habilitada em PERIGOSO é aceito")
        void register_highRiskWasteWithHazardousSpecialty_isAccepted() {
            Incident incident = pendingIncident(WasteCategory.METAL, RiskLevel.HIGH, Priority.HIGH);
            Cooperative cooperative = cooperative("METAL,PERIGOSO");
            givenIncidentAndCooperative(incident, cooperative);
            UUID savedId = UUID.randomUUID();
            when(collectionRepository.saveAndFlush(any(Collection.class))).thenAnswer(invocation -> {
                Collection saved = invocation.getArgument(0);
                saved.setId(savedId);
                return saved;
            });

            CollectionResponseDTO response = service.register(requestFor(incident, cooperative, false), author);

            assertThat(response.id()).isEqualTo(savedId);
            verify(collectionProcedure).updateCollectionStatus(savedId, "REQUESTED", "Collection Requested");
        }
    }

    @Nested
    @DisplayName("consultas com resultado")
    class QueriesWithResults {

        private Collection ownedCollection() {
            Incident incident = pendingIncident(WasteCategory.PLASTICO, RiskLevel.LOW, Priority.MEDIUM);
            Collection collection = collection(
                    incident, cooperative("PLASTICO"), CollectionStatusType.SCHEDULED, NOW.minusDays(1), false);
            givenOwnedCollection(collection);
            return collection;
        }

        @Test
        @DisplayName("getCollectionById devolve a coleta convertida")
        void getCollectionById_ownedCollection_returnsConvertedCollection() {
            Collection collection = ownedCollection();

            CollectionResponseDTO response = service.getCollectionById(collection.getId(), author);

            assertThat(response.id()).isEqualTo(collection.getId());
            assertThat(response.currentStatus()).isEqualTo("SCHEDULED");
        }

        @Test
        @DisplayName("getCollectionStatus devolve o histórico convertido")
        void getCollectionStatus_ownedCollection_returnsConvertedHistory() {
            Collection collection = ownedCollection();
            CollectionStatus first = new CollectionStatus();
            first.setId(UUID.randomUUID());
            first.setCollection(collection);
            first.setStatus("REQUESTED");
            CollectionStatus second = new CollectionStatus();
            second.setId(UUID.randomUUID());
            second.setCollection(collection);
            second.setStatus("SCHEDULED");
            second.setObservation("Scheduled for Monday");
            when(collectionStatusRepository.findByCollectionIdOrderByChangedAtAsc(collection.getId()))
                    .thenReturn(List.of(first, second));

            List<CollectionStatusResponseDTO> response = service.getCollectionStatus(collection.getId(), author);

            assertThat(response).extracting(CollectionStatusResponseDTO::status)
                    .containsExactly("REQUESTED", "SCHEDULED");
            assertThat(response.get(1).collectionId()).isEqualTo(collection.getId());
            assertThat(response.get(1).observation()).isEqualTo("Scheduled for Monday");
        }
    }

    @Nested
    @DisplayName("register (urgente opcional)")
    class RegisterUrgentOptional {

        @Test
        @DisplayName("urgente omitido assume false para incidente não crítico")
        void register_nullUrgent_defaultsToFalse() {
            Incident incident = pendingIncident(WasteCategory.PLASTICO, RiskLevel.LOW, Priority.MEDIUM);
            Cooperative cooperative = cooperative("PLASTICO");
            givenIncidentAndCooperative(incident, cooperative);
            when(collectionRepository.saveAndFlush(any(Collection.class))).thenAnswer(invocation -> invocation.getArgument(0));
            CollectionRequestDTO request = new CollectionRequestDTO(incident.getId(), cooperative.getId(), "Recyclable", null);

            CollectionResponseDTO response = service.register(request, author);

            assertThat(response.urgent()).isFalse();
        }
    }
}
