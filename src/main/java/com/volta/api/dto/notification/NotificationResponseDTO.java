package com.volta.api.dto.notification;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Notificação do usuário autenticado")
public record NotificationResponseDTO(

        @Schema(description = "ID da notificação", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,
        @Schema(description = "Tipo da notificação", example = "COLLECTION", allowableValues = {"INCIDENT", "COLLECTION"})
        String type,
        @Schema(description = "Título", example = "Coleta solicitada")
        String title,
        @Schema(description = "Mensagem", example = "A coleta da ocorrência \"Papel misturado\" foi solicitada.")
        String message,
        @Schema(description = "Indica se a notificação já foi lida", example = "false")
        boolean read,
        @Schema(description = "Data e hora de criação", example = "2026-11-02T09:30:00")
        LocalDateTime createdAt
) {
}
