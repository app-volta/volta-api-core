package com.volta.api.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para troca da própria senha")
public record PasswordChangeRequestDTO(

        @Schema(description = "Senha atual", example = "senha12345")
        @NotBlank(message = "Current password is required")
        String currentPassword,

        @Schema(description = "Nova senha (8 a 72 caracteres)", example = "novaSenha12345")
        @NotBlank(message = "New password is required")
        @Size(min = 8, max = 72)
        String newPassword
) {
}
