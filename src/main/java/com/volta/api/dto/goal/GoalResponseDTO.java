package com.volta.api.dto.goal;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Metas da empresa")
public record GoalResponseDTO(

        @Schema(description = "Meta mensal de reciclagem, em kg. Nulo quando ainda não foi definida", example = "1200.00")
        BigDecimal monthlyKg
) {
}
