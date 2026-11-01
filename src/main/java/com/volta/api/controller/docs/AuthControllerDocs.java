package com.volta.api.controller.docs;

import com.volta.api.dto.auth.ForgotPasswordRequestDTO;
import com.volta.api.dto.auth.LoginRequestDTO;
import com.volta.api.dto.auth.MessageResponseDTO;
import com.volta.api.dto.auth.ResetPasswordRequestDTO;
import com.volta.api.dto.auth.TokenResponseDTO;
import com.volta.api.dto.auth.VerifyCodeRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Autenticação", description = "Login, emissão de token JWT e recuperação de senha")
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

    @Operation(
            summary = "Solicita o código de recuperação de senha",
            description = "Envia um código de 4 dígitos por e-mail, que expira em até 10 minutos e deixa de valer após a troca da senha. "
                    + "A resposta é sempre a mesma, exista ou não a conta. **Acesso:** público."
    )
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido recebido"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    MessageResponseDTO forgotPassword(ForgotPasswordRequestDTO dto);

    @Operation(
            summary = "Verifica o código de recuperação de senha",
            description = "Confere o código sem alterar a senha. **Acesso:** público."
    )
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Código válido"),
            @ApiResponse(responseCode = "400", description = "Código inválido, expirado ou dados inválidos")
    })
    void verifyCode(VerifyCodeRequestDTO dto);

    @Operation(
            summary = "Redefine a senha com o código recebido",
            description = "Troca a senha, e o código deixa de valer. O indicador de primeiro login passa a false. **Acesso:** público."
    )
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Senha redefinida"),
            @ApiResponse(responseCode = "400", description = "Código inválido, expirado ou dados inválidos")
    })
    void resetPassword(ResetPasswordRequestDTO dto);
}
