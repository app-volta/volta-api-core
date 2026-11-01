package com.volta.api.service;

import com.volta.api.database.entity.Collection;
import com.volta.api.database.entity.CollectionStatus;
import com.volta.api.database.entity.Cooperative;
import com.volta.api.database.entity.Incident;
import com.volta.api.database.entity.WasteType;
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
import com.volta.api.dto.collection.CollectionStatusResponseDTO;
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
import com.volta.api.usecase.CollectionUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CollectionService implements CollectionUseCase {

    private static final DateTimeFormatter DEADLINE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private final CollectionRepository collectionRepository;
    private final CollectionProcedure collectionProcedure;
    private final CollectionFunction collectionFunction;
    private final IncidentRepository incidentRepository;
    private final IncidentProcedure incidentProcedure;
    private final CooperativeRepository cooperativeRepository;
    private final CollectionMapper collectionMapper;
    private final CollectionStatusRepository collectionStatusRepository;
    private final CollectionStatusMapper collectionStatusMapper;
    private final Clock clock;

    @Transactional
    public CollectionResponseDTO register(CollectionRequestDTO dto, AuthenticatedUser author) {
        Incident incident = incidentRepository.findByIdAndCompanyId(dto.incidentId(), author.companyId())
                .orElseThrow(() -> new ResourceNotFoundException("Incident"));
        Cooperative cooperative = cooperativeRepository.findById(dto.cooperativeId())
                .orElseThrow(() -> new ResourceNotFoundException("Cooperative"));

        if (IncidentStatus.CLOSED.name().equals(incident.getStatus())) {
            throw new BusinessRuleException("Cannot request a collection for a closed incident");
        }

        if (collectionRepository.existsByIncidentIdAndCurrentStatusIn(incident.getId(), CollectionStatusType.ACTIVE_STATUSES)) {
            throw new ConflictException("Incident already has an active collection");
        }

        validateCooperativeCompatibility(incident.getWasteTypes(), cooperative);

        Collection collection = collectionMapper.toEntity(dto, incident, cooperative);

        if (Priority.CRITICAL.name().equals(incident.getPriority())) {
            collection.setUrgent(true);
        }

        Collection saved = collectionRepository.saveAndFlush(collection);

        collectionProcedure.updateCollectionStatus(
                saved.getId(),
                CollectionStatusType.REQUESTED.name(),
                "Collection Requested"
        );

        return collectionMapper.toResponse(saved);
    }

    public List<CollectionResponseDTO> getCollections(AuthenticatedUser author) {
        List<CollectionResponseDTO> collections = new ArrayList<>();

        for (Collection collection : collectionRepository.findByIncidentCompanyId(author.companyId())) {
            collections.add(collectionMapper.toResponse(collection));
        }

        return collections;
    }

    public CollectionResponseDTO getCollectionById(UUID id, AuthenticatedUser author) {
        Collection collection = findOwnedCollection(id, author);
        return collectionMapper.toResponse(collection);
    }

    public List<CollectionStatusResponseDTO> getCollectionStatus(UUID collectionId, AuthenticatedUser author) {
        findOwnedCollection(collectionId, author);

        List<CollectionStatusResponseDTO> collectionStatuses = new ArrayList<>();

        for (CollectionStatus collectionStatus : collectionStatusRepository.findByCollectionIdOrderByChangedAtAsc(collectionId)) {
            collectionStatuses.add(collectionStatusMapper.toResponse(collectionStatus));
        }

        return collectionStatuses;
    }

    @Transactional
    public void schedule(UUID id, CollectionScheduleRequestDTO dto, AuthenticatedUser author) {
        Collection collection = findOwnedCollection(id, author);
        validateTransition(collection, CollectionStatusType.SCHEDULED);
        validateSchedulingDeadline(collection, dto.scheduledAt());
        collectionProcedure.scheduleCollection(id, dto.scheduledAt());
    }

    @Transactional
    public void updateStatus(UUID id, CollectionStatusRequestDTO dto, AuthenticatedUser author) {
        Collection collection = findOwnedCollection(id, author);

        if (dto.status() == CollectionStatusType.SCHEDULED) {
            throw new BusinessRuleException("Use the schedule endpoint to schedule a collection");
        }

        validateTransition(collection, dto.status());

        if (dto.status() == CollectionStatusType.CANCELED && (dto.observation() == null || dto.observation().isBlank())) {
            throw new InvalidRequestException("Observation is required to cancel a collection");
        }

        collectionProcedure.updateCollectionStatus(id, dto.status().name(), dto.observation());

        Incident incident = collection.getIncident();
        if (dto.status() == CollectionStatusType.COMPLETED && !IncidentStatus.CLOSED.name().equals(incident.getStatus())) {
            incidentProcedure.closeIncident(incident.getId());
        }
    }

    public CollectionCompletionTimeResponseDTO getCompletionTime(UUID id, AuthenticatedUser author) {
        Collection collection = findOwnedCollection(id, author);

        if (!CollectionStatusType.COMPLETED.name().equals(collection.getCurrentStatus())) {
            throw new BusinessRuleException("Collection is not completed yet");
        }

        return new CollectionCompletionTimeResponseDTO(
                id,
                collectionFunction.calculateCollectionCompletionHours(id)
        );
    }

    private Collection findOwnedCollection(UUID id, AuthenticatedUser author) {
        return collectionRepository.findByIdAndIncidentCompanyId(id, author.companyId())
                .orElseThrow(() -> new ResourceNotFoundException("Collection"));
    }

    private void validateTransition(Collection collection, CollectionStatusType next) {
        CollectionStatusType current = CollectionStatusType.valueOf(collection.getCurrentStatus());

        if (!current.canTransitionTo(next)) {
            throw new BusinessRuleException("Invalid status transition from " + current.name() + " to " + next.name());
        }
    }
    private void validateCooperativeCompatibility(Set<WasteType> wasteTypes, Cooperative cooperative) {
        if (wasteTypes == null || wasteTypes.isEmpty()) {
            throw new BusinessRuleException("Incident has no waste type to be collected");
        }

        Set<WasteCategory> specialties = WasteCategory.fromList(cooperative.getSpecialties());

        for (WasteType wasteType : wasteTypes) {
            WasteCategory category = WasteCategory.from(wasteType.getCategory())
                    .orElseThrow(() -> new BusinessRuleException(
                            "Waste type category '" + wasteType.getCategory() + "' is not a CONAMA 275 category"
                    ));

            if (category == WasteCategory.RADIOATIVO) {
                throw new BusinessRuleException("Radioactive waste must be handled by a CNEN authorized company, not by a cooperative");
            }

            if (!specialties.contains(category)) {
                throw new BusinessRuleException("Cooperative does not handle " + category.name() + " waste");
            }

            if (RiskLevel.isHazardous(wasteType.getDefaultRiskLevel()) && !specialties.contains(WasteCategory.PERIGOSO)) {
                throw new BusinessRuleException("Hazardous waste (NBR 10004 Class I) requires a cooperative qualified for PERIGOSO waste");
            }
        }
    }

    private void validateSchedulingDeadline(Collection collection, LocalDateTime scheduledAt) {
        Duration deadline;
        if (collection.isUrgent()){
            deadline = Priority.URGENT_SCHEDULING_DEADLINE;
        } else {
            deadline = Priority.valueOf(collection.getIncident().getPriority()).getSchedulingDeadline();
        }

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime limit = collection.getRequestedAt().plus(deadline);

        if (limit.isBefore(now)) {
            limit = now.plus(Priority.URGENT_SCHEDULING_DEADLINE);
        }

        if (scheduledAt.isAfter(limit)) {
            throw new BusinessRuleException(
                    "Collection must be scheduled until " + DEADLINE_FORMAT.format(limit)
            );
        }
    }
}
