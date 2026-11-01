package com.volta.api.dto.goal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@Schema(description = "Dados para definição das metas da empresa")
public record GoalRequestDTO(

        @Schema(description = "Meta mensal de reciclagem, em kg", example = "1200.00")
        @NotNull
        @Positive
        @Digits(integer = 10, fraction = 2)
        BigDecimal monthlyKg
) {
}
