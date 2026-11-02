package com.volta.api.dto.cooperative;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Cooperativa")
public record CooperativeResponseDTO(

        @Schema(description = "ID da cooperativa", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,
        @Schema(description = "Nome da cooperativa", example = "Cooperativa Recicla Mais")
        String name,
        @Schema(description = "CNPJ sem máscara", example = "11444777000161")
        String cnpj,
        @Schema(description = "Latitude da sede", example = "-23.561684")
        BigDecimal latitude,
        @Schema(description = "Longitude da sede", example = "-46.655981")
        BigDecimal longitude,
        @Schema(description = "E-mail de contato da cooperativa", example = "contato@reciclamais.com.br")
        String email,
        @Schema(description = "Média das avaliações recebidas (1 a 5), recalculada a cada nova avaliação", example = "4.50")
        BigDecimal averageRating,
        @Schema(description = "Categorias de resíduo (CONAMA 275/2001) que a cooperativa recebe", example = "PAPEL,PLASTICO,METAL")
        String specialties
) {
}
