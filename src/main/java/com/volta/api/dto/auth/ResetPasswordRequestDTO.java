package com.volta.api.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Redefinição de senha com o código recebido por e-mail")
public record ResetPasswordRequestDTO(

        @Schema(description = "E-mail da conta", example = "maria.silva@volta.com")
        @NotBlank(message = "Email is required")
        @Email
        String email,

        @Schema(description = "Código de 4 dígitos recebido por e-mail", example = "4821")
        @NotBlank(message = "Code is required")
        @Pattern(regexp = "\\d{4}", message = "Code must have 4 digits")
        String code,

        @Schema(description = "Nova senha (8 a 72 caracteres)", example = "novaSenha12345")
        @NotBlank(message = "New password is required")
        @Size(min = 8, max = 72)
        String newPassword
) {
}
