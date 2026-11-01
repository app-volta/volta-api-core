package com.volta.api.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Pedido de código para recuperação de senha")
public record ForgotPasswordRequestDTO(

        @Schema(description = "E-mail da conta", example = "maria.silva@volta.com")
        @NotBlank(message = "Email is required")
        @Email
        String email
) {
}
