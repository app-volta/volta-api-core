package com.volta.api.dto.incident;

import com.volta.api.enums.IncidentStatus;
import com.volta.api.enums.Priority;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Filtros opcionais da listagem de incidentes")
public record IncidentFilterDTO(
        @Schema(description = "Filtra pelo status do incidente", example = "PENDING", allowableValues = {"PENDING", "APPROVED", "REJECTED", "CLOSED"})
        IncidentStatus status,
        @Schema(description = "Filtra pela prioridade", example = "HIGH", allowableValues = {"LOW", "MEDIUM", "HIGH", "CRITICAL"})
        Priority priority,
        @Schema(description = "Filtra pela área", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID areaId
) {
}
