package com.volta.api.controller.docs;

import com.volta.api.dto.notification.NotificationResponseDTO;
import com.volta.api.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

@Tag(name = "Notificações", description = "Notificações do usuário autenticado")
public interface NotificationControllerDocs {

    @Operation(
            summary = "Lista as notificações do usuário",
            description = "Retorna as notificações do usuário autenticado, da mais recente para a mais antiga. "
                    + "Com `unread=true` retorna só as não lidas.\n\n"
                    + "**Acesso:** ADMIN, MANAGER, EMPLOYEE."
    )
    @ApiResponse(responseCode = "200", description = "Notificações do usuário")
    List<NotificationResponseDTO> show(
            @Parameter(description = "Retorna apenas as notificações não lidas", example = "true") boolean unread,
            @Parameter(hidden = true) AuthenticatedUser author
    );

    @Operation(
            summary = "Marca uma notificação como lida",
            description = "**Regras:** a notificação precisa pertencer ao usuário autenticado.\n\n"
                    + "**Acesso:** ADMIN, MANAGER, EMPLOYEE."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Notificação marcada como lida"),
            @ApiResponse(responseCode = "404", description = "Notificação não encontrada")
    })
    void markAsRead(
            @Parameter(description = "ID da notificação") UUID id,
            @Parameter(hidden = true) AuthenticatedUser author
    );

    @Operation(
            summary = "Marca todas as notificações como lidas",
            description = "Marca como lidas todas as notificações do usuário autenticado.\n\n"
                    + "**Acesso:** ADMIN, MANAGER, EMPLOYEE."
    )
    @ApiResponse(responseCode = "204", description = "Notificações marcadas como lidas")
    void markAllAsRead(@Parameter(hidden = true) AuthenticatedUser author);
}
