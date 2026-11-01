package com.volta.api.usecase;

import com.volta.api.dto.report.AiReportResponseDTO;
import com.volta.api.dto.incident.IncidentResponseDTO;
import com.volta.api.dto.incident.IncidentFilterDTO;
import com.volta.api.dto.incident.IncidentRejectRequestDTO;
import com.volta.api.dto.incident.IncidentRequestDTO;
import com.volta.api.dto.incident.IncidentUpdateRequestDTO;
import com.volta.api.dto.scoring.IncidentScoringContextDTO;
import com.volta.api.security.AuthenticatedUser;

import java.util.List;
import java.util.UUID;

public interface IncidentUseCase {
    IncidentResponseDTO register(IncidentRequestDTO request, AuthenticatedUser author);

    List<IncidentResponseDTO> getIncidents(IncidentFilterDTO filter, AuthenticatedUser author);

    IncidentResponseDTO getIncidentById(UUID id, AuthenticatedUser author);

    IncidentResponseDTO update(UUID id, IncidentUpdateRequestDTO dto, AuthenticatedUser author);

    IncidentResponseDTO approve(UUID id, AuthenticatedUser author);

    IncidentResponseDTO reject(UUID id, IncidentRejectRequestDTO dto, AuthenticatedUser author);

    void closeIncident(UUID id, AuthenticatedUser author);

    AiReportResponseDTO getAiReport(UUID id, AuthenticatedUser author);

    IncidentScoringContextDTO getScoringContext(UUID id, AuthenticatedUser author);
}
