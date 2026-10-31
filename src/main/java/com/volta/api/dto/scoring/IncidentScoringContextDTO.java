package com.volta.api.dto.scoring;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Contexto da ocorrência para o cálculo de score de cooperativas")
public record IncidentScoringContextDTO(
        @Schema(description = "ID da ocorrência", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID incidentId,
        @Schema(description = "Prioridade da ocorrência", example = "HIGH")
        String priority,
        @Schema(description = "Tipos de resíduo da ocorrência")
        List<WasteTypeScoringDTO> wasteTypes,
        @Schema(description = "Nível de contaminação; usa o maior risco padrão entre os resíduos quando não informado", example = "MEDIUM")
        String contaminationLevel,
        @Schema(description = "Ponto de origem da medição de distância; coordenadas da empresa; nulo enquanto ela não as tiver cadastradas")
        CoordinatesScoringDTO origin,
        @Schema(description = "Resumo do relatório de IA; nulo quando a ocorrência não tem relatório")
        AiReportScoringDTO aiReport
) {
}
