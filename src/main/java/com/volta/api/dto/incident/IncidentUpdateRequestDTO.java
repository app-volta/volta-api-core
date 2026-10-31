package com.volta.api.dto.incident;

import com.volta.api.enums.Priority;
import com.volta.api.enums.RiskLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(description = "Dados para edição de um incidente ainda não aprovado. Campos omitidos permanecem como estão")
public record IncidentUpdateRequestDTO(

        @Schema(description = "Título do incidente", example = "Vazamento de óleo na máquina 3")
        @Size(max = 100)
        String title,

        @Schema(description = "IDs dos tipos de resíduo. Quando informado, substitui os tipos atuais", example = "[\"3fa85f64-5717-4562-b3fc-2c963f66afa6\"]")
        @Size(min = 1, message = "At least one waste type is required")
        List<@NotNull UUID> wasteTypeIds,

        @Schema(description = "Descrição gerada/ajustada pela IA", example = "Resíduo oleoso com risco de contaminação do solo")
        String aiDescription,

        @Schema(description = "Nível de contaminação", example = "MEDIUM")
        RiskLevel contaminationLevel,

        @Schema(description = "Quantidade estimada de resíduo, em kg", example = "25.50")
        @PositiveOrZero
        @Digits(integer = 10, fraction = 2)
        BigDecimal estimatedQuantity,

        @Schema(description = "Prioridade. É elevada para HIGH quando o resíduo é perigoso ou a contaminação é HIGH", example = "HIGH")
        Priority priority,

        @Schema(description = "Motivo da alteração", example = "Quantidade corrigida após vistoria")
        @NotBlank(message = "Change reason is required")
        @Size(max = 200)
        String changeReason
) {
}
