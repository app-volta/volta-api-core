package com.volta.api.controller.docs;

import com.volta.api.dto.company.CompanyRequestDTO;
import com.volta.api.dto.company.CompanyResponseDTO;
import com.volta.api.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

@Tag(name = "Empresas", description = "Empresas clientes que geram resíduos")
public interface CompanyControllerDocs {

    @Operation(summary = "Cadastra uma empresa", description = "**Acesso:** ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Empresa cadastrada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "409", description = "CNPJ já cadastrado")
    })
    CompanyResponseDTO create(CompanyRequestDTO dto);

    @Operation(summary = "Lista todas as empresas", description = "**Acesso:** ADMIN.")
    @ApiResponse(responseCode = "200", description = "Lista de empresas")
    List<CompanyResponseDTO> show();

    @Operation(summary = "Busca a empresa do usuário autenticado", description = "**Acesso:** ADMIN e MANAGER.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Empresa encontrada"),
            @ApiResponse(responseCode = "404", description = "Usuário sem empresa vinculada")
    })
    CompanyResponseDTO showMe(AuthenticatedUser author);

    @Operation(summary = "Busca uma empresa pelo ID", description = "**Acesso:** ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Empresa encontrada"),
            @ApiResponse(responseCode = "404", description = "Empresa não encontrada")
    })
    CompanyResponseDTO show(@Parameter(description = "ID da empresa") UUID id);

    @Operation(summary = "Atualiza uma empresa", description = "**Acesso:** ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Empresa atualizada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Empresa não encontrada"),
            @ApiResponse(responseCode = "409", description = "CNPJ já cadastrado em outra empresa")
    })
    CompanyResponseDTO update(@Parameter(description = "ID da empresa") UUID id, CompanyRequestDTO dto);

    @Operation(summary = "Remove uma empresa", description = "**Acesso:** ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Empresa removida"),
            @ApiResponse(responseCode = "404", description = "Empresa não encontrada"),
            @ApiResponse(responseCode = "409", description = "Empresa possui registros vinculados")
    })
    void remove(@Parameter(description = "ID da empresa") UUID id);
}
