package com.volta.api.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Mensagem informativa")
public record MessageResponseDTO(

        @Schema(description = "Texto da mensagem", example = "If the email exists, a code was sent")
        String message
) {
}
