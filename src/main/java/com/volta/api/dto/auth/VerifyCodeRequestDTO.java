package com.volta.api.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Verificação do código de recuperação de senha")
public record VerifyCodeRequestDTO(

        @Schema(description = "E-mail da conta", example = "maria.silva@volta.com")
        @NotBlank(message = "Email is required")
        @Email
        String email,

        @Schema(description = "Código de 4 dígitos recebido por e-mail", example = "4821")
        @NotBlank(message = "Code is required")
        @Pattern(regexp = "\\d{4}", message = "Code must have 4 digits")
        String code
) {
}
