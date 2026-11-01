package com.volta.api.controller.docs;

import com.volta.api.dto.goal.GoalRequestDTO;
import com.volta.api.dto.goal.GoalResponseDTO;
import com.volta.api.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Metas", description = "Metas de reciclagem da empresa do usuário autenticado")
public interface GoalControllerDocs {

    @Operation(
            summary = "Consulta as metas da empresa",
            description = "Retorna a meta mensal mais recente da empresa, ou nulo se ainda não foi definida. "
                    + "**Acesso:** ADMIN, MANAGER, EMPLOYEE."
    )
    @ApiResponse(responseCode = "200", description = "Metas da empresa")
    GoalResponseDTO show(@Parameter(hidden = true) AuthenticatedUser author);

    @Operation(
            summary = "Define as metas da empresa",
            description = "Grava a meta mensal na métrica ESG mais recente da empresa.\n\n"
                    + "**Regras:** a empresa precisa ter ao menos uma métrica ESG registrada.\n\n"
                    + "**Acesso:** ADMIN, MANAGER."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Metas atualizadas"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "422", description = "Empresa sem métrica ESG registrada")
    })
    GoalResponseDTO update(GoalRequestDTO dto, @Parameter(hidden = true) AuthenticatedUser author);
}
