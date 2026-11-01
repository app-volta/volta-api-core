package com.volta.api.controller.docs;

import com.volta.api.dto.auth.LoginRequestDTO;
import com.volta.api.dto.auth.TokenResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Autenticação", description = "Login e emissão de token JWT")
public interface AuthControllerDocs {

    @Operation(
            summary = "Realiza login",
            description = "Autentica o usuário por e-mail e senha e retorna um token JWT. "
                    + "Use o token no botão **Authorize** para acessar os demais endpoints. "
                    + "O campo firstLogin continua true até o usuário trocar a senha em PUT /users/me/password. **Acesso:** público."
    )
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login realizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados de login inválidos"),
            @ApiResponse(responseCode = "401", description = "E-mail ou senha incorretos")
    })
    TokenResponseDTO login(LoginRequestDTO loginRequestDto);
}
