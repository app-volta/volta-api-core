package com.volta.api.dto.scoring;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(description = "Dados da cooperativa usados no cálculo de score")
public record CooperativeScoringDataDTO(
        @Schema(description = "ID da cooperativa", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,
        @Schema(description = "Nome da cooperativa", example = "Cooperativa Recicla Sul")
        String name,
        @Schema(description = "Latitude da sede", example = "-23.561684")
        BigDecimal latitude,
        @Schema(description = "Longitude da sede", example = "-46.655981")
        BigDecimal longitude,
        @Schema(description = "Média das avaliações; 0 ou nulo significa sem avaliação", example = "4.50")
        BigDecimal averageRating,
        @Schema(description = "Categorias aceitas, no formato do enum WasteCategory", example = "[\"PAPEL\", \"PLASTICO\", \"PERIGOSO\"]")
        List<String> specialties,
        @Schema(description = "E-mail de contato da cooperativa", example = "contato@reciclamais.com.br")
        String email
) {
}
