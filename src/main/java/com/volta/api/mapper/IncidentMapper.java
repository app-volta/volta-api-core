package com.volta.api.mapper;

import com.volta.api.database.entity.Area;
import com.volta.api.database.entity.Company;
import com.volta.api.database.entity.Incident;
import com.volta.api.database.entity.Users;
import com.volta.api.database.entity.WasteType;
import com.volta.api.dto.incident.IncidentRequestDTO;
import com.volta.api.dto.incident.IncidentResponseDTO;
import com.volta.api.dto.incident.IncidentUpdateRequestDTO;
import com.volta.api.enums.IncidentStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class IncidentMapper {

    public Incident toEntity(
            IncidentRequestDTO dto,
            Company company,
            Users user,
            Area area,
            Set<WasteType> wasteTypes
    ) {
        Incident incident = new Incident();

        incident.setCompany(company);
        incident.setUser(user);
        incident.setArea(area);
        incident.setWasteTypes(wasteTypes);
        incident.setPhotoUrl(dto.photoUrl());
        incident.setEmployeeDescription(dto.employeeDescription());
        incident.setContaminationLevel(
                dto.contaminationLevel() != null ? dto.contaminationLevel().name() : null
        );
        incident.setEstimatedQuantity(dto.estimatedQuantity());
        incident.setPriority(dto.priority().name());
        incident.setStatus(IncidentStatus.PENDING.name());

        return incident;
    }

    public void updateEntity(Incident incident, IncidentUpdateRequestDTO dto, Set<WasteType> wasteTypes) {
        if (dto.title() != null) {
            incident.setTitle(dto.title());
        }
        if (wasteTypes != null) {
            incident.setWasteTypes(wasteTypes);
        }
        if (dto.aiDescription() != null) {
            incident.setAiDescription(dto.aiDescription());
        }
        if (dto.contaminationLevel() != null) {
            incident.setContaminationLevel(dto.contaminationLevel().name());
        }
        if (dto.estimatedQuantity() != null) {
            incident.setEstimatedQuantity(dto.estimatedQuantity());
        }
        if (dto.priority() != null) {
            incident.setPriority(dto.priority().name());
        }
        incident.setChangeReason(dto.changeReason());
    }

    public IncidentResponseDTO toResponse(Incident incident) {
        List<UUID> wasteTypeIds = new ArrayList<>();

        for (WasteType wasteType : incident.getWasteTypes()) {
            wasteTypeIds.add(wasteType.getId());
        }

        UUID reviewedById = null;
        if (incident.getReviewedBy() != null) {
            reviewedById = incident.getReviewedBy().getId();
        }

        return new IncidentResponseDTO(
                incident.getId(),
                incident.getCompany().getId(),
                incident.getUser().getId(),
                incident.getArea().getId(),
                wasteTypeIds,
                incident.getPhotoUrl(),
                incident.getEmployeeDescription(),
                incident.getContaminationLevel(),
                incident.getEstimatedQuantity(),
                incident.getPriority(),
                incident.getStatus(),
                incident.getRegisteredAt(),
                incident.getTitle(),
                incident.getAiDescription(),
                incident.getChangeReason(),
                reviewedById,
                incident.getReviewedAt()
        );
    }
}