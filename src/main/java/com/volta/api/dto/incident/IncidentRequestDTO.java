package com.volta.api.dto.incident;


import com.volta.api.enums.Priority;
import com.volta.api.enums.RiskLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.URL;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(description = "Dados para registro de incidente")
public record IncidentRequestDTO(

        @Schema(description = "ID da área onde o incidente ocorreu", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        @NotNull
        UUID areaId,

        @Schema(description = "IDs dos tipos de resíduo identificados", example = "[\"3fa85f64-5717-4562-b3fc-2c963f66afa6\"]")
        @NotEmpty(message = "At least one waste type is required")
        List<@NotNull UUID> wasteTypeIds,

        @Schema(description = "URL da foto do incidente", example = "https://storage.volta.com/incidentes/foto.jpg")
        @URL
        @Size(max = 500)
        String photoUrl,

        @Schema(description = "Descrição do incidente feita pelo funcionário", example = "Vazamento de óleo próximo à máquina 3")
        @NotBlank(message = "Description is required")
        String employeeDescription,

        @Schema(description = "Nível de contaminação percebido. Se omitido, assume o risco padrão do tipo de resíduo", example = "MEDIUM")
        RiskLevel contaminationLevel,

        @Schema(description = "Quantidade estimada de resíduo, em kg", example = "25.50")
        @PositiveOrZero
        @Digits(integer = 10, fraction = 2)
        BigDecimal estimatedQuantity,

        @Schema(description = "Prioridade do incidente. É elevada para HIGH quando o resíduo é perigoso ou a contaminação é HIGH", example = "HIGH")
        @NotNull(message = "Priority is required")
        Priority priority
) {
}
