package com.volta.api.dto.incident;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para recusar um incidente")
public record IncidentRejectRequestDTO(

        @Schema(description = "Motivo da recusa", example = "Foto não mostra o resíduo informado")
        @NotBlank(message = "Reason is required")
        @Size(max = 200)
        String reason
) {
}
