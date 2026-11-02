package com.volta.api.service;

import com.volta.api.database.entity.*;
import com.volta.api.database.procedure.IncidentProcedure;
import com.volta.api.database.repository.*;
import com.volta.api.dto.incident.IncidentFilterDTO;
import com.volta.api.dto.incident.IncidentRejectRequestDTO;
import com.volta.api.dto.incident.IncidentRequestDTO;
import com.volta.api.dto.incident.IncidentUpdateRequestDTO;
import com.volta.api.dto.report.AiReportResponseDTO;
import com.volta.api.dto.incident.IncidentResponseDTO;
import com.volta.api.dto.scoring.IncidentScoringContextDTO;
import com.volta.api.enums.CollectionStatusType;
import com.volta.api.enums.IncidentStatus;
import com.volta.api.enums.Priority;
import com.volta.api.enums.RiskLevel;
import com.volta.api.exception.BusinessRuleException;
import com.volta.api.exception.ResourceNotFoundException;
import com.volta.api.mapper.AiReportMapper;
import com.volta.api.mapper.IncidentMapper;
import com.volta.api.mapper.ScoringMapper;
import com.volta.api.security.AuthenticatedUser;
import com.volta.api.usecase.IncidentUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IncidentService implements IncidentUseCase {

    private final IncidentRepository incidentRepository;

    private final CompanyRepository companyRepository;

    private final UserRepository userRepository;

    private final AreaRepository areaRepository;

    private final WasteTypeRepository wasteTypeRepository;

    private final IncidentMapper incidentMapper;

    private final IncidentProcedure incidentProcedure;

    private final AiReportRepository aiReportRepository;

    private final AiReportMapper aiReportMapper;

    private final CollectionRepository collectionRepository;

    private final ScoringMapper scoringMapper;

    private final NotificationPublisher notificationPublisher;

    @Transactional
    public IncidentResponseDTO register(IncidentRequestDTO dto, AuthenticatedUser author) {

        Company company = companyRepository.findById(author.companyId())
                .orElseThrow(() -> new ResourceNotFoundException("Company"));
        Users user = userRepository.findById(author.id())
                .orElseThrow(() -> new ResourceNotFoundException("User"));
        Area area = areaRepository.findByIdAndCompanyId(dto.areaId(), author.companyId()).
                orElseThrow(() -> new ResourceNotFoundException("Area"));
        Set<WasteType> wasteTypes = findWasteTypes(dto.wasteTypeIds());

        Incident incident = incidentMapper.toEntity(
                dto,
                company,
                user,
                area,
                wasteTypes
        );

        if (incident.getContaminationLevel() == null) {
            incident.setContaminationLevel(highestDefaultRisk(wasteTypes));
        }
        incident.setPriority(resolvePriority(dto.priority(), incident.getContaminationLevel(), wasteTypes).name());

        Incident savedIncident = incidentRepository.saveAndFlush(incident);
        notificationPublisher.incidentRegistered(savedIncident);
        return incidentMapper.toResponse(savedIncident);
    }

    @Transactional(readOnly = true)
    public List<IncidentResponseDTO> getIncidents(IncidentFilterDTO filter, AuthenticatedUser author) {
        List<IncidentResponseDTO> incidents = new ArrayList<>();

        List<Incident> found = incidentRepository.search(
                author.companyId(),
                filter.status() != null ? filter.status().name() : null,
                filter.priority() != null ? filter.priority().name() : null,
                filter.areaId()
        );

        for (Incident incident : found) {
            incidents.add(incidentMapper.toResponse(incident));
        }

        return incidents;
    }

    @Transactional(readOnly = true)
    public IncidentResponseDTO getIncidentById(UUID id, AuthenticatedUser author) {
        Incident incident = incidentRepository.findByIdAndCompanyId(id, author.companyId())
                .orElseThrow(() -> new ResourceNotFoundException("Incident"));
        return incidentMapper.toResponse(incident);
    }

    @Transactional
    public IncidentResponseDTO update(UUID id, IncidentUpdateRequestDTO dto, AuthenticatedUser author) {
        Incident incident = incidentRepository.findByIdAndCompanyId(id, author.companyId())
                .orElseThrow(() -> new ResourceNotFoundException("Incident"));

        if (!IncidentStatus.PENDING.name().equals(incident.getStatus())) {
            throw new BusinessRuleException("Only pending incidents can be edited");
        }

        Set<WasteType> wasteTypes = null;
        if (dto.wasteTypeIds() != null) {
            wasteTypes = findWasteTypes(dto.wasteTypeIds());
        }

        incidentMapper.updateEntity(incident, dto, wasteTypes);

        if (incident.getContaminationLevel() == null) {
            incident.setContaminationLevel(highestDefaultRisk(incident.getWasteTypes()));
        }
        incident.setPriority(
                resolvePriority(Priority.valueOf(incident.getPriority()), incident.getContaminationLevel(), incident.getWasteTypes()).name()
        );

        return incidentMapper.toResponse(incidentRepository.save(incident));
    }

    @Transactional
    public IncidentResponseDTO approve(UUID id, AuthenticatedUser author) {
        Incident incident = incidentRepository.findByIdAndCompanyId(id, author.companyId())
                .orElseThrow(() -> new ResourceNotFoundException("Incident"));

        if (!IncidentStatus.PENDING.name().equals(incident.getStatus())) {
            throw new BusinessRuleException("Only pending incidents can be approved");
        }

        Users reviewer = userRepository.findById(author.id())
                .orElseThrow(() -> new ResourceNotFoundException("User"));

        incident.setStatus(IncidentStatus.APPROVED.name());
        incident.setReviewedBy(reviewer);
        incident.setReviewedAt(LocalDateTime.now());
        Incident savedIncident = incidentRepository.save(incident);
        notificationPublisher.incidentReviewed(savedIncident, true);
        return incidentMapper.toResponse(savedIncident);
    }

    @Transactional
    public IncidentResponseDTO reject(UUID id, IncidentRejectRequestDTO dto, AuthenticatedUser author) {
        Incident incident = incidentRepository.findByIdAndCompanyId(id, author.companyId())
                .orElseThrow(() -> new ResourceNotFoundException("Incident"));

        if (!IncidentStatus.PENDING.name().equals(incident.getStatus())) {
            throw new BusinessRuleException("Only pending incidents can be rejected");
        }

        Users reviewer = userRepository.findById(author.id())
                .orElseThrow(() -> new ResourceNotFoundException("User"));

        incident.setStatus(IncidentStatus.REJECTED.name());
        incident.setChangeReason(dto.reason());
        incident.setReviewedBy(reviewer);
        incident.setReviewedAt(LocalDateTime.now());
        Incident savedIncident = incidentRepository.save(incident);
        notificationPublisher.incidentReviewed(savedIncident, false);
        return incidentMapper.toResponse(savedIncident);
    }

    @Transactional
    public void closeIncident(UUID id, AuthenticatedUser author) {
        Incident incident = incidentRepository.findByIdAndCompanyId(id, author.companyId())
                .orElseThrow(() -> new ResourceNotFoundException("Incident"));

        if (IncidentStatus.CLOSED.name().equals(incident.getStatus())) {
            throw new BusinessRuleException("Incident is already closed");
        }

        if (collectionRepository.existsByIncidentIdAndCurrentStatusIn(id, CollectionStatusType.ACTIVE_STATUSES)) {
            throw new BusinessRuleException("Incident has an active collection and will be closed when it is completed");
        }

        boolean hazardous = hasHazardous(incident.getWasteTypes());
        if (hazardous && !collectionRepository.existsByIncidentIdAndCurrentStatus(id, CollectionStatusType.COMPLETED.name())) {
            throw new BusinessRuleException("Hazardous waste incidents can only be closed after a completed collection");
        }

        incidentProcedure.closeIncident(id);
    }

    private Set<WasteType> findWasteTypes(List<UUID> ids) {
        Set<UUID> distinctIds = new HashSet<>(ids);
        Set<WasteType> wasteTypes = new HashSet<>(wasteTypeRepository.findAllById(distinctIds));

        if (wasteTypes.size() != distinctIds.size()) {
            throw new ResourceNotFoundException("Waste type");
        }

        return wasteTypes;
    }

    private String highestDefaultRisk(Set<WasteType> wasteTypes) {
        List<String> riskLevels = new ArrayList<>();

        for (WasteType wasteType : wasteTypes) {
            riskLevels.add(wasteType.getDefaultRiskLevel());
        }

        return RiskLevel.highest(riskLevels);
    }

    private boolean hasHazardous(Set<WasteType> wasteTypes) {
        for (WasteType wasteType : wasteTypes) {
            if (RiskLevel.isHazardous(wasteType.getDefaultRiskLevel())) {
                return true;
            }
        }

        return false;
    }

    private Priority resolvePriority(Priority informed, String contaminationLevel, Set<WasteType> wasteTypes) {
        boolean highRisk = hasHazardous(wasteTypes)
                || RiskLevel.isHazardous(contaminationLevel);

        if (highRisk && informed.isLowerThan(Priority.HIGH)) {
            return Priority.HIGH;
        }

        return informed;
    }

    public AiReportResponseDTO getAiReport(UUID id, AuthenticatedUser author) {
        incidentRepository.findByIdAndCompanyId(id, author.companyId())
                .orElseThrow(() -> new ResourceNotFoundException("Incident"));

        AiReport aiReport = aiReportRepository.findFirstByIncidentIdOrderByGeneratedAtDesc(id)
                .orElseThrow(() -> new ResourceNotFoundException("AI report"));

        return aiReportMapper.toResponse(aiReport);
    }

    @Transactional(readOnly = true)
    public IncidentScoringContextDTO getScoringContext(UUID id, AuthenticatedUser author) {
        Incident incident = incidentRepository.findByIdAndCompanyId(id, author.companyId())
                .orElseThrow(() -> new ResourceNotFoundException("Incident"));

        AiReport aiReport = aiReportRepository.findFirstByIncidentIdOrderByGeneratedAtDesc(id)
                .orElse(null);

        return scoringMapper.toIncidentContext(incident, aiReport);
    }
}
