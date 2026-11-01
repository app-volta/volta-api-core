package com.volta.api.controller.docs;

import com.volta.api.dto.user.PasswordChangeRequestDTO;
import com.volta.api.dto.user.UserRequestDTO;
import com.volta.api.dto.user.UserRoleUpdateRequestDTO;
import com.volta.api.dto.user.UserUpdateRequestDTO;
import com.volta.api.dto.user.UserResponseDTO;
import com.volta.api.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

@Tag(name = "Usuários", description = "Cadastro de usuários, perfil do usuário autenticado e gestão de perfis de acesso")
public interface UserControllerDocs {

    @Operation(summary = "Cadastra um usuário", description = "**Acesso:** ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuário cadastrado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Empresa não encontrada"),
            @ApiResponse(responseCode = "409", description = "E-mail já cadastrado")
    })
    UserResponseDTO create(UserRequestDTO dto);

    @Operation(summary = "Lista todos os usuários", description = "**Acesso:** ADMIN.")
    @ApiResponse(responseCode = "200", description = "Lista de usuários")
    List<UserResponseDTO> show();

    @Operation(summary = "Busca um usuário pelo ID", description = "**Acesso:** ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário encontrado"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    UserResponseDTO showById(@Parameter(description = "ID do usuário") UUID id);

    @Operation(
            summary = "Consulta o próprio perfil",
            description = "Retorna os dados do usuário autenticado. **Acesso:** qualquer usuário autenticado."
    )
    @ApiResponse(responseCode = "200", description = "Dados do usuário autenticado")
    UserResponseDTO showMe(@Parameter(hidden = true) AuthenticatedUser author);

    @Operation(
            summary = "Atualiza o próprio perfil",
            description = "Altera o nome e o cargo do usuário autenticado. **Acesso:** qualquer usuário autenticado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil atualizado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    UserResponseDTO update(UserUpdateRequestDTO dto, @Parameter(hidden = true) AuthenticatedUser author);

    @Operation(
            summary = "Altera a própria senha",
            description = "Exige a senha atual. Após a troca, o indicador de primeiro login passa a false. "
                    + "**Acesso:** qualquer usuário autenticado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Senha alterada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou senha atual incorreta"),
            @ApiResponse(responseCode = "422", description = "Nova senha igual à senha atual")
    })
    void changePassword(PasswordChangeRequestDTO dto, @Parameter(hidden = true) AuthenticatedUser author);

    @Operation(
            summary = "Altera o perfil de acesso de um usuário",
            description = "Troca a role do usuário. O administrador não pode alterar a própria role. **Acesso:** ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil de acesso atualizado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado"),
            @ApiResponse(responseCode = "422", description = "Tentativa de alterar a própria role")
    })
    UserResponseDTO updateRole(
            @Parameter(description = "ID do usuário") UUID id,
            UserRoleUpdateRequestDTO dto,
            @Parameter(hidden = true) AuthenticatedUser author
    );
}
