package com.volta.api.service;

import com.volta.api.database.entity.Area;
import com.volta.api.database.entity.Company;
import com.volta.api.database.entity.Incident;
import com.volta.api.database.entity.Users;
import com.volta.api.database.entity.WasteType;
import com.volta.api.database.procedure.IncidentProcedure;
import com.volta.api.database.repository.AiReportRepository;
import com.volta.api.database.repository.AreaRepository;
import com.volta.api.database.repository.CollectionRepository;
import com.volta.api.database.repository.CompanyRepository;
import com.volta.api.database.repository.IncidentRepository;
import com.volta.api.database.repository.UserRepository;
import com.volta.api.database.repository.WasteTypeRepository;
import com.volta.api.dto.incident.IncidentFilterDTO;
import com.volta.api.dto.incident.IncidentRejectRequestDTO;
import com.volta.api.dto.incident.IncidentRequestDTO;
import com.volta.api.dto.incident.IncidentResponseDTO;
import com.volta.api.dto.incident.IncidentUpdateRequestDTO;
import com.volta.api.dto.report.AiReportResponseDTO;
import com.volta.api.enums.CollectionStatusType;
import com.volta.api.enums.IncidentStatus;
import com.volta.api.enums.Priority;
import com.volta.api.enums.RiskLevel;
import com.volta.api.enums.WasteCategory;
import com.volta.api.exception.BusinessRuleException;
import com.volta.api.exception.ResourceNotFoundException;
import com.volta.api.mapper.AiReportMapper;
import com.volta.api.mapper.IncidentMapper;
import com.volta.api.mapper.ScoringMapper;
import com.volta.api.dto.scoring.IncidentScoringContextDTO;
import com.volta.api.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.volta.api.support.CollectionFixtures.incident;
import static com.volta.api.support.CollectionFixtures.wasteType;
import static com.volta.api.support.UserFixtures.authenticatedUser;
import static com.volta.api.support.UserFixtures.company;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import com.volta.api.database.entity.AiReport;

@ExtendWith(MockitoExtension.class)
@DisplayName("IncidentService")
class IncidentServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AreaRepository areaRepository;

    @Mock
    private WasteTypeRepository wasteTypeRepository;

    @Mock
    private IncidentProcedure incidentProcedure;

    @Mock
    private AiReportRepository aiReportRepository;

    @Mock
    private CollectionRepository collectionRepository;

    private IncidentService service;

    private final UUID companyId = UUID.randomUUID();

    private final AuthenticatedUser author = authenticatedUser(UUID.randomUUID(), companyId, "EMPLOYEE");

    @BeforeEach
    void setUp() {
        service = new IncidentService(
                incidentRepository,
                companyRepository,
                userRepository,
                areaRepository,
                wasteTypeRepository,
                new IncidentMapper(),
                incidentProcedure,
                aiReportRepository,
                new AiReportMapper(),
                collectionRepository,
                new ScoringMapper());
    }

    @Nested
    @DisplayName("register")
    class Register {

        private Company company;
        private Users user;
        private Area area;

        @BeforeEach
        void givenAuthorData() {
            company = company(companyId);
            user = new Users();
            user.setId(author.id());
            area = new Area();
            area.setId(UUID.randomUUID());
            area.setCompany(company);
        }

        private void givenRegistrationData(WasteType wasteType) {
            when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
            when(userRepository.findById(author.id())).thenReturn(Optional.of(user));
            when(areaRepository.findByIdAndCompanyId(area.getId(), companyId)).thenReturn(Optional.of(area));
            when(wasteTypeRepository.findAllById(Set.of(wasteType.getId()))).thenReturn(List.of(wasteType));
            when(incidentRepository.saveAndFlush(any(Incident.class))).thenAnswer(invocation -> invocation.getArgument(0));
        }

        private IncidentRequestDTO request(WasteType wasteType, RiskLevel contamination, Priority priority) {
            return new IncidentRequestDTO(
                    area.getId(),
                    List.of(wasteType.getId()),
                    null,
                    "Oil leak next to machine 3",
                    contamination,
                    new BigDecimal("10.00"),
                    priority);
        }

        @Test
        @DisplayName("área de outra empresa lança not found e não salva")
        void register_areaOfAnotherCompany_throwsNotFound() {
            when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
            when(userRepository.findById(author.id())).thenReturn(Optional.of(user));
            when(areaRepository.findByIdAndCompanyId(area.getId(), companyId)).thenReturn(Optional.empty());
            WasteType wasteType = wasteType(WasteCategory.PLASTICO, RiskLevel.LOW);

            assertThatThrownBy(() -> service.register(request(wasteType, null, Priority.LOW), author))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Area not found");

            verify(incidentRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("sem contaminação informada assume o risco padrão do tipo de resíduo")
        void register_withoutContamination_usesWasteTypeDefaultRisk() {
            WasteType wasteType = wasteType(WasteCategory.PLASTICO, RiskLevel.MEDIUM);
            givenRegistrationData(wasteType);

            IncidentResponseDTO response = service.register(request(wasteType, null, Priority.LOW), author);

            assertThat(response.contaminationLevel()).isEqualTo("MEDIUM");
            assertThat(response.priority()).isEqualTo("LOW");
            assertThat(response.status()).isEqualTo(IncidentStatus.PENDING.name());
        }

        @Test
        @DisplayName("resíduo perigoso eleva a prioridade para HIGH")
        void register_hazardousWaste_raisesPriorityToHigh() {
            WasteType wasteType = wasteType(WasteCategory.PERIGOSO, RiskLevel.HIGH);
            givenRegistrationData(wasteType);

            IncidentResponseDTO response = service.register(request(wasteType, RiskLevel.LOW, Priority.LOW), author);

            assertThat(response.priority()).isEqualTo("HIGH");
        }

        @Test
        @DisplayName("vários tipos de resíduo assumem o maior risco padrão e elevam a prioridade se algum for perigoso")
        void register_multipleWasteTypes_usesHighestRiskAndRaisesPriority() {
            WasteType common = wasteType(WasteCategory.PLASTICO, RiskLevel.LOW);
            WasteType hazardous = wasteType(WasteCategory.PERIGOSO, RiskLevel.HIGH);
            when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
            when(userRepository.findById(author.id())).thenReturn(Optional.of(user));
            when(areaRepository.findByIdAndCompanyId(area.getId(), companyId)).thenReturn(Optional.of(area));
            when(wasteTypeRepository.findAllById(Set.of(common.getId(), hazardous.getId())))
                    .thenReturn(List.of(common, hazardous));
            when(incidentRepository.saveAndFlush(any(Incident.class))).thenAnswer(invocation -> invocation.getArgument(0));
            IncidentRequestDTO request = new IncidentRequestDTO(
                    area.getId(),
                    List.of(common.getId(), hazardous.getId()),
                    null,
                    "Oil leak next to machine 3",
                    null,
                    new BigDecimal("10.00"),
                    Priority.LOW);

            IncidentResponseDTO response = service.register(request, author);

            assertThat(response.contaminationLevel()).isEqualTo("HIGH");
            assertThat(response.priority()).isEqualTo("HIGH");
            assertThat(response.wasteTypeIds()).containsExactlyInAnyOrder(common.getId(), hazardous.getId());
        }

        @Test
        @DisplayName("contaminação HIGH eleva a prioridade para HIGH")
        void register_highContamination_raisesPriorityToHigh() {
            WasteType wasteType = wasteType(WasteCategory.PLASTICO, RiskLevel.LOW);
            givenRegistrationData(wasteType);

            IncidentResponseDTO response = service.register(request(wasteType, RiskLevel.HIGH, Priority.MEDIUM), author);

            assertThat(response.priority()).isEqualTo("HIGH");
        }

        @Test
        @DisplayName("prioridade CRITICAL não é rebaixada")
        void register_criticalPriority_isKept() {
            WasteType wasteType = wasteType(WasteCategory.PERIGOSO, RiskLevel.HIGH);
            givenRegistrationData(wasteType);

            IncidentResponseDTO response = service.register(request(wasteType, null, Priority.CRITICAL), author);

            assertThat(response.priority()).isEqualTo("CRITICAL");
        }

        @Test
        @DisplayName("salva o incidente vinculado à empresa e ao usuário autenticado")
        void register_validData_savesWithAuthorCompanyAndUser() {
            WasteType wasteType = wasteType(WasteCategory.PLASTICO, RiskLevel.LOW);
            givenRegistrationData(wasteType);

            service.register(request(wasteType, null, Priority.LOW), author);

            ArgumentCaptor<Incident> captor = ArgumentCaptor.forClass(Incident.class);
            verify(incidentRepository).saveAndFlush(captor.capture());
            assertThat(captor.getValue().getCompany()).isSameAs(company);
            assertThat(captor.getValue().getUser()).isSameAs(user);
        }
    }

    @Nested
    @DisplayName("getIncidents e getIncidentById")
    class Queries {

        @Test
        @DisplayName("busca sempre filtrando pela empresa do autor")
        void getIncidents_filtersByAuthorCompany() {
            when(incidentRepository.search(companyId, "PENDING", null, null)).thenReturn(List.of());

            List<IncidentResponseDTO> response = service.getIncidents(
                    new IncidentFilterDTO(IncidentStatus.PENDING, null, null), author);

            assertThat(response).isEmpty();
            verify(incidentRepository).search(companyId, "PENDING", null, null);
        }

        @Test
        @DisplayName("incidente de outra empresa lança not found")
        void getIncidentById_ofAnotherCompany_throwsNotFound() {
            UUID id = UUID.randomUUID();
            when(incidentRepository.findByIdAndCompanyId(id, companyId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getIncidentById(id, author))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Incident not found");
        }

        @Test
        @DisplayName("relatório de IA de incidente de outra empresa lança not found sem consultar relatório")
        void getAiReport_ofAnotherCompany_throwsNotFoundWithoutQueryingReport() {
            UUID id = UUID.randomUUID();
            when(incidentRepository.findByIdAndCompanyId(id, companyId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getAiReport(id, author))
                    .isInstanceOf(ResourceNotFoundException.class);

            verifyNoInteractions(aiReportRepository);
        }
    }

    @Nested
    @DisplayName("update e approve")
    class UpdateAndApprove {

        private Incident givenOwnedIncident(WasteType wasteType, IncidentStatus status) {
            Incident incident = incident(wasteType, Priority.LOW, status);
            incident.setCompany(company(companyId));
            Users user = new Users();
            user.setId(UUID.randomUUID());
            incident.setUser(user);
            Area area = new Area();
            area.setId(UUID.randomUUID());
            incident.setArea(area);
            when(incidentRepository.findByIdAndCompanyId(incident.getId(), companyId)).thenReturn(Optional.of(incident));
            return incident;
        }

        private void givenSaveReturnsArgument() {
            when(incidentRepository.save(any(Incident.class))).thenAnswer(invocation -> invocation.getArgument(0));
        }

        private IncidentUpdateRequestDTO updateRequest(UUID wasteTypeId, Priority priority) {
            return new IncidentUpdateRequestDTO(
                    "New title",
                    wasteTypeId != null ? List.of(wasteTypeId) : null,
                    "AI description",
                    null,
                    new BigDecimal("12.50"),
                    priority,
                    "Quantity fixed");
        }

        @Test
        @DisplayName("update altera os campos e registra o motivo")
        void update_pendingIncident_appliesChangesAndReason() {
            WasteType wasteType = wasteType(WasteCategory.PLASTICO, RiskLevel.LOW);
            Incident incident = givenOwnedIncident(wasteType, IncidentStatus.PENDING);
            incident.setContaminationLevel(RiskLevel.LOW.name());

            givenSaveReturnsArgument();

            IncidentResponseDTO response = service.update(incident.getId(), updateRequest(null, Priority.MEDIUM), author);

            assertThat(response.title()).isEqualTo("New title");
            assertThat(response.aiDescription()).isEqualTo("AI description");
            assertThat(response.changeReason()).isEqualTo("Quantity fixed");
            assertThat(response.estimatedQuantity()).isEqualByComparingTo("12.50");
            assertThat(response.priority()).isEqualTo(Priority.MEDIUM.name());
        }

        @Test
        @DisplayName("update com resíduo perigoso eleva a prioridade para HIGH")
        void update_hazardousWasteType_raisesPriorityToHigh() {
            WasteType common = wasteType(WasteCategory.PLASTICO, RiskLevel.LOW);
            WasteType hazardous = wasteType(WasteCategory.PERIGOSO, RiskLevel.HIGH);
            Incident incident = givenOwnedIncident(common, IncidentStatus.PENDING);
            when(wasteTypeRepository.findAllById(Set.of(hazardous.getId()))).thenReturn(List.of(hazardous));

            givenSaveReturnsArgument();

            IncidentResponseDTO response = service.update(incident.getId(), updateRequest(hazardous.getId(), Priority.LOW), author);

            assertThat(response.priority()).isEqualTo(Priority.HIGH.name());
        }

        @Test
        @DisplayName("update de incidente aprovado lança business rule")
        void update_approvedIncident_throwsBusinessRule() {
            Incident incident = givenOwnedIncident(wasteType(WasteCategory.PLASTICO, RiskLevel.LOW), IncidentStatus.APPROVED);

            assertThatThrownBy(() -> service.update(incident.getId(), updateRequest(null, null), author))
                    .isInstanceOf(BusinessRuleException.class);

            verify(incidentRepository, never()).save(any());
        }

        @Test
        @DisplayName("update de incidente inexistente lança not found")
        void update_missingIncident_throwsNotFound() {
            UUID id = UUID.randomUUID();
            when(incidentRepository.findByIdAndCompanyId(id, companyId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(id, updateRequest(null, null), author))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("approve muda o status de PENDING para APPROVED")
        void approve_pendingIncident_setsApproved() {
            Incident incident = givenOwnedIncident(wasteType(WasteCategory.PLASTICO, RiskLevel.LOW), IncidentStatus.PENDING);

            givenSaveReturnsArgument();

            IncidentResponseDTO response = service.approve(incident.getId(), author);

            assertThat(response.status()).isEqualTo(IncidentStatus.APPROVED.name());
        }

        @Test
        @DisplayName("approve de incidente que não está pendente lança business rule")
        void approve_closedIncident_throwsBusinessRule() {
            Incident incident = givenOwnedIncident(wasteType(WasteCategory.PLASTICO, RiskLevel.LOW), IncidentStatus.CLOSED);

            assertThatThrownBy(() -> service.approve(incident.getId(), author))
                    .isInstanceOf(BusinessRuleException.class);

            verify(incidentRepository, never()).save(any());
        }

        @Test
        @DisplayName("reject muda o status de PENDING para REJECTED e registra o motivo")
        void reject_pendingIncident_setsRejectedAndReason() {
            Incident incident = givenOwnedIncident(wasteType(WasteCategory.PLASTICO, RiskLevel.LOW), IncidentStatus.PENDING);

            givenSaveReturnsArgument();

            IncidentResponseDTO response = service.reject(
                    incident.getId(), new IncidentRejectRequestDTO("Photo does not show the waste"), author);

            assertThat(response.status()).isEqualTo(IncidentStatus.REJECTED.name());
            assertThat(response.changeReason()).isEqualTo("Photo does not show the waste");
        }

        @Test
        @DisplayName("reject de incidente que não está pendente lança business rule")
        void reject_approvedIncident_throwsBusinessRule() {
            Incident incident = givenOwnedIncident(wasteType(WasteCategory.PLASTICO, RiskLevel.LOW), IncidentStatus.APPROVED);

            assertThatThrownBy(() -> service.reject(incident.getId(), new IncidentRejectRequestDTO("Reason"), author))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Only pending incidents can be rejected");

            verify(incidentRepository, never()).save(any());
        }

        @Test
        @DisplayName("reject de incidente inexistente lança not found")
        void reject_missingIncident_throwsNotFound() {
            UUID id = UUID.randomUUID();
            when(incidentRepository.findByIdAndCompanyId(id, companyId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.reject(id, new IncidentRejectRequestDTO("Reason"), author))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("update de incidente recusado lança business rule")
        void update_rejectedIncident_throwsBusinessRule() {
            Incident incident = givenOwnedIncident(wasteType(WasteCategory.PLASTICO, RiskLevel.LOW), IncidentStatus.REJECTED);

            assertThatThrownBy(() -> service.update(incident.getId(), updateRequest(null, null), author))
                    .isInstanceOf(BusinessRuleException.class);

            verify(incidentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("closeIncident")
    class CloseIncident {

        private Incident givenOwnedIncident(WasteType wasteType, IncidentStatus status) {
            Incident incident = incident(wasteType, Priority.MEDIUM, status);
            when(incidentRepository.findByIdAndCompanyId(incident.getId(), companyId)).thenReturn(Optional.of(incident));
            return incident;
        }

        @Test
        @DisplayName("incidente inexistente lança not found")
        void closeIncident_missingIncident_throwsNotFound() {
            UUID id = UUID.randomUUID();
            when(incidentRepository.findByIdAndCompanyId(id, companyId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.closeIncident(id, author))
                    .isInstanceOf(ResourceNotFoundException.class);

            verifyNoInteractions(incidentProcedure);
        }

        @Test
        @DisplayName("incidente já fechado lança regra de negócio")
        void closeIncident_alreadyClosed_throwsBusinessRule() {
            Incident incident = givenOwnedIncident(wasteType(WasteCategory.PLASTICO, RiskLevel.LOW), IncidentStatus.CLOSED);

            assertThatThrownBy(() -> service.closeIncident(incident.getId(), author))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Incident is already closed");

            verifyNoInteractions(incidentProcedure);
        }

        @Test
        @DisplayName("incidente com coleta ativa lança regra de negócio")
        void closeIncident_withActiveCollection_throwsBusinessRule() {
            Incident incident = givenOwnedIncident(wasteType(WasteCategory.PLASTICO, RiskLevel.LOW), IncidentStatus.PENDING);
            when(collectionRepository.existsByIncidentIdAndCurrentStatusIn(
                    incident.getId(), CollectionStatusType.ACTIVE_STATUSES)).thenReturn(true);

            assertThatThrownBy(() -> service.closeIncident(incident.getId(), author))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Incident has an active collection and will be closed when it is completed");

            verifyNoInteractions(incidentProcedure);
        }

        @Test
        @DisplayName("resíduo perigoso sem coleta concluída lança regra de negócio")
        void closeIncident_hazardousWithoutCompletedCollection_throwsBusinessRule() {
            Incident incident = givenOwnedIncident(wasteType(WasteCategory.PERIGOSO, RiskLevel.HIGH), IncidentStatus.PENDING);
            when(collectionRepository.existsByIncidentIdAndCurrentStatus(
                    incident.getId(), CollectionStatusType.COMPLETED.name())).thenReturn(false);

            assertThatThrownBy(() -> service.closeIncident(incident.getId(), author))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Hazardous waste incidents can only be closed after a completed collection");

            verifyNoInteractions(incidentProcedure);
        }

        @Test
        @DisplayName("resíduo perigoso com coleta concluída fecha pela procedure")
        void closeIncident_hazardousWithCompletedCollection_closesThroughProcedure() {
            Incident incident = givenOwnedIncident(wasteType(WasteCategory.PERIGOSO, RiskLevel.HIGH), IncidentStatus.PENDING);
            when(collectionRepository.existsByIncidentIdAndCurrentStatus(
                    incident.getId(), CollectionStatusType.COMPLETED.name())).thenReturn(true);

            service.closeIncident(incident.getId(), author);

            verify(incidentProcedure).closeIncident(incident.getId());
        }

        @Test
        @DisplayName("resíduo comum sem coleta ativa fecha pela procedure")
        void closeIncident_commonWasteWithoutActiveCollection_closesThroughProcedure() {
            Incident incident = givenOwnedIncident(wasteType(WasteCategory.PLASTICO, RiskLevel.LOW), IncidentStatus.PENDING);

            service.closeIncident(incident.getId(), author);

            verify(incidentProcedure).closeIncident(incident.getId());
        }
    }

    private Incident fullIncident(WasteType wasteType) {
        Incident incident = incident(wasteType, Priority.MEDIUM, IncidentStatus.PENDING);
        Company company = company(companyId);
        Users user = new Users();
        user.setId(author.id());
        Area area = new Area();
        area.setId(UUID.randomUUID());
        incident.setCompany(company);
        incident.setUser(user);
        incident.setArea(area);
        return incident;
    }

    @Nested
    @DisplayName("register (consultas obrigatórias)")
    class RegisterLookups {

        private IncidentRequestDTO request() {
            return new IncidentRequestDTO(
                    UUID.randomUUID(), List.of(UUID.randomUUID()), null, "Oil leak", null, null, Priority.LOW);
        }

        @Test
        @DisplayName("empresa inexistente lança not found")
        void register_missingCompany_throwsNotFound() {
            when(companyRepository.findById(companyId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.register(request(), author))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Company not found");

            verify(incidentRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("usuário inexistente lança not found")
        void register_missingUser_throwsNotFound() {
            when(companyRepository.findById(companyId)).thenReturn(Optional.of(company(companyId)));
            when(userRepository.findById(author.id())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.register(request(), author))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found");

            verify(incidentRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("tipo de resíduo inexistente lança not found")
        void register_missingWasteType_throwsNotFound() {
            IncidentRequestDTO request = request();
            Area area = new Area();
            area.setId(request.areaId());
            when(companyRepository.findById(companyId)).thenReturn(Optional.of(company(companyId)));
            when(userRepository.findById(author.id())).thenReturn(Optional.of(new Users()));
            when(areaRepository.findByIdAndCompanyId(request.areaId(), companyId)).thenReturn(Optional.of(area));
            when(wasteTypeRepository.findAllById(Set.copyOf(request.wasteTypeIds()))).thenReturn(List.of());

            assertThatThrownBy(() -> service.register(request, author))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Waste type not found");

            verify(incidentRepository, never()).saveAndFlush(any());
        }
    }

    @Nested
    @DisplayName("consultas com resultado")
    class QueriesWithResults {

        @Test
        @DisplayName("getIncidents converte os incidentes encontrados")
        void getIncidents_foundIncidents_convertsAll() {
            Incident first = fullIncident(wasteType(WasteCategory.PLASTICO, RiskLevel.LOW));
            Incident second = fullIncident(wasteType(WasteCategory.PAPEL, RiskLevel.LOW));
            when(incidentRepository.search(companyId, null, null, null)).thenReturn(List.of(first, second));

            List<IncidentResponseDTO> response = service.getIncidents(new IncidentFilterDTO(null, null, null), author);

            assertThat(response).extracting(IncidentResponseDTO::id)
                    .containsExactly(first.getId(), second.getId());
        }

        @Test
        @DisplayName("getIncidentById existente devolve o incidente convertido")
        void getIncidentById_existing_returnsConvertedIncident() {
            Incident incident = fullIncident(wasteType(WasteCategory.PLASTICO, RiskLevel.LOW));
            when(incidentRepository.findByIdAndCompanyId(incident.getId(), companyId)).thenReturn(Optional.of(incident));

            IncidentResponseDTO response = service.getIncidentById(incident.getId(), author);

            assertThat(response.id()).isEqualTo(incident.getId());
            assertThat(response.companyId()).isEqualTo(companyId);
            assertThat(response.wasteTypeIds()).containsExactly(incident.getWasteTypes().iterator().next().getId());
        }

        @Test
        @DisplayName("incidente sem tipo de resíduo devolve wasteTypeIds vazio")
        void getIncidentById_withoutWasteType_returnsEmptyWasteTypeIds() {
            Incident incident = fullIncident(null);
            when(incidentRepository.findByIdAndCompanyId(incident.getId(), companyId)).thenReturn(Optional.of(incident));

            assertThat(service.getIncidentById(incident.getId(), author).wasteTypeIds()).isEmpty();
        }

        @Test
        @DisplayName("relatório de IA existente é devolvido convertido")
        void getAiReport_existingReport_returnsConvertedReport() {
            Incident incident = fullIncident(wasteType(WasteCategory.PLASTICO, RiskLevel.LOW));
            AiReport report = new AiReport();
            report.setId(UUID.randomUUID());
            report.setIncident(incident);
            report.setDetectedWasteType("Lubricant oil");
            report.setAiContaminationLevel("HIGH");
            when(incidentRepository.findByIdAndCompanyId(incident.getId(), companyId)).thenReturn(Optional.of(incident));
            when(aiReportRepository.findFirstByIncidentIdOrderByGeneratedAtDesc(incident.getId()))
                    .thenReturn(Optional.of(report));

            AiReportResponseDTO response = service.getAiReport(incident.getId(), author);

            assertThat(response.id()).isEqualTo(report.getId());
            assertThat(response.incidentId()).isEqualTo(incident.getId());
            assertThat(response.detectedWasteType()).isEqualTo("Lubricant oil");
            assertThat(response.aiContaminationLevel()).isEqualTo("HIGH");
        }

        @Test
        @DisplayName("incidente sem relatório de IA lança not found")
        void getAiReport_withoutReport_throwsNotFound() {
            Incident incident = fullIncident(wasteType(WasteCategory.PLASTICO, RiskLevel.LOW));
            when(incidentRepository.findByIdAndCompanyId(incident.getId(), companyId)).thenReturn(Optional.of(incident));
            when(aiReportRepository.findFirstByIncidentIdOrderByGeneratedAtDesc(incident.getId()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getAiReport(incident.getId(), author))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("AI report not found");
        }
    }

    @Nested
    @DisplayName("getScoringContext")
    class GetScoringContext {

        @Test
        @DisplayName("incidente de outra empresa lança not found sem consultar relatório")
        void getScoringContext_ofAnotherCompany_throwsNotFoundWithoutQueryingReport() {
            UUID id = UUID.randomUUID();
            when(incidentRepository.findByIdAndCompanyId(id, companyId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getScoringContext(id, author))
                    .isInstanceOf(ResourceNotFoundException.class);

            verifyNoInteractions(aiReportRepository);
        }

        @Test
        @DisplayName("com relatório de IA devolve contexto completo e origem nula")
        void getScoringContext_withAiReport_returnsFullContext() {
            Incident incident = fullIncident(wasteType(WasteCategory.PERIGOSO, RiskLevel.HIGH));
            incident.setContaminationLevel("MEDIUM");
            AiReport report = new AiReport();
            report.setDetectedWasteType("Lubricant oil");
            report.setAiContaminationLevel("HIGH");
            report.setRecommendations("Isolate the area");
            when(incidentRepository.findByIdAndCompanyId(incident.getId(), companyId)).thenReturn(Optional.of(incident));
            when(aiReportRepository.findFirstByIncidentIdOrderByGeneratedAtDesc(incident.getId()))
                    .thenReturn(Optional.of(report));

            IncidentScoringContextDTO context = service.getScoringContext(incident.getId(), author);

            assertThat(context.incidentId()).isEqualTo(incident.getId());
            assertThat(context.priority()).isEqualTo("MEDIUM");
            assertThat(context.wasteTypes()).hasSize(1);
            assertThat(context.wasteTypes().get(0).category()).isEqualTo("PERIGOSO");
            assertThat(context.wasteTypes().get(0).defaultRiskLevel()).isEqualTo("HIGH");
            assertThat(context.contaminationLevel()).isEqualTo("MEDIUM");
            assertThat(context.origin()).isNull();
            assertThat(context.aiReport().detectedWasteType()).isEqualTo("Lubricant oil");
            assertThat(context.aiReport().aiContaminationLevel()).isEqualTo("HIGH");
            assertThat(context.aiReport().recommendations()).isEqualTo("Isolate the area");
        }

        @Test
        @DisplayName("empresa com coordenadas devolve a origem")
        void getScoringContext_companyWithCoordinates_returnsOrigin() {
            Incident incident = fullIncident(wasteType(WasteCategory.PLASTICO, RiskLevel.LOW));
            incident.getCompany().setLatitude(-23.561684);
            incident.getCompany().setLongitude(-46.655981);
            when(incidentRepository.findByIdAndCompanyId(incident.getId(), companyId)).thenReturn(Optional.of(incident));
            when(aiReportRepository.findFirstByIncidentIdOrderByGeneratedAtDesc(incident.getId()))
                    .thenReturn(Optional.empty());

            IncidentScoringContextDTO context = service.getScoringContext(incident.getId(), author);

            assertThat(context.origin().latitude()).isEqualTo(-23.561684);
            assertThat(context.origin().longitude()).isEqualTo(-46.655981);
        }

        @Test
        @DisplayName("empresa sem coordenadas devolve origem nula")
        void getScoringContext_companyWithoutCoordinates_returnsNullOrigin() {
            Incident incident = fullIncident(wasteType(WasteCategory.PLASTICO, RiskLevel.LOW));
            when(incidentRepository.findByIdAndCompanyId(incident.getId(), companyId)).thenReturn(Optional.of(incident));
            when(aiReportRepository.findFirstByIncidentIdOrderByGeneratedAtDesc(incident.getId()))
                    .thenReturn(Optional.empty());

            assertThat(service.getScoringContext(incident.getId(), author).origin()).isNull();
        }

        @Test
        @DisplayName("incidente sem empresa devolve origem nula")
        void getScoringContext_incidentWithoutCompany_returnsNullOrigin() {
            Incident incident = fullIncident(wasteType(WasteCategory.PLASTICO, RiskLevel.LOW));
            incident.setCompany(null);
            when(incidentRepository.findByIdAndCompanyId(incident.getId(), companyId)).thenReturn(Optional.of(incident));
            when(aiReportRepository.findFirstByIncidentIdOrderByGeneratedAtDesc(incident.getId()))
                    .thenReturn(Optional.empty());

            assertThat(service.getScoringContext(incident.getId(), author).origin()).isNull();
        }

        @Test
        @DisplayName("sem relatório de IA e sem contaminação usa o risco padrão e relatório nulo")
        void getScoringContext_withoutReportAndContamination_usesDefaultRisk() {
            Incident incident = fullIncident(wasteType(WasteCategory.PLASTICO, RiskLevel.LOW));
            when(incidentRepository.findByIdAndCompanyId(incident.getId(), companyId)).thenReturn(Optional.of(incident));
            when(aiReportRepository.findFirstByIncidentIdOrderByGeneratedAtDesc(incident.getId()))
                    .thenReturn(Optional.empty());

            IncidentScoringContextDTO context = service.getScoringContext(incident.getId(), author);

            assertThat(context.contaminationLevel()).isEqualTo("LOW");
            assertThat(context.aiReport()).isNull();
        }

        @Test
        @DisplayName("categoria fora do formato do enum é normalizada")
        void getScoringContext_unnormalizedCategory_isNormalized() {
            WasteType wasteType = wasteType(WasteCategory.NAO_RECICLAVEL, RiskLevel.LOW);
            wasteType.setCategory("Não reciclável");
            Incident incident = fullIncident(wasteType);
            when(incidentRepository.findByIdAndCompanyId(incident.getId(), companyId)).thenReturn(Optional.of(incident));
            when(aiReportRepository.findFirstByIncidentIdOrderByGeneratedAtDesc(incident.getId()))
                    .thenReturn(Optional.empty());

            IncidentScoringContextDTO context = service.getScoringContext(incident.getId(), author);

            assertThat(context.wasteTypes().get(0).category()).isEqualTo("NAO_RECICLAVEL");
        }
    }

    @Nested
    @DisplayName("closeIncident (sem tipo de resíduo)")
    class CloseWithoutWasteType {

        @Test
        @DisplayName("incidente sem tipo de resíduo não é tratado como perigoso e fecha pela procedure")
        void closeIncident_withoutWasteType_closesThroughProcedure() {
            Incident incident = incident(null, Priority.LOW, IncidentStatus.PENDING);
            when(incidentRepository.findByIdAndCompanyId(incident.getId(), companyId)).thenReturn(Optional.of(incident));

            service.closeIncident(incident.getId(), author);

            verify(incidentProcedure).closeIncident(incident.getId());
        }
    }
}
