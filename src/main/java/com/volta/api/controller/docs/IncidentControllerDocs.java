package com.volta.api.controller.docs;

import com.volta.api.dto.incident.IncidentFilterDTO;
import com.volta.api.dto.incident.IncidentRejectRequestDTO;
import com.volta.api.dto.incident.IncidentRequestDTO;
import com.volta.api.dto.incident.IncidentUpdateRequestDTO;
import com.volta.api.dto.report.AiReportResponseDTO;
import com.volta.api.dto.incident.IncidentResponseDTO;
import com.volta.api.dto.scoring.IncidentScoringContextDTO;
import com.volta.api.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;

import java.util.List;
import java.util.UUID;

@Tag(name = "Incidentes", description = "Registro e acompanhamento de incidentes com resíduos")
public interface IncidentControllerDocs {

    @Operation(
            summary = "Registra um incidente",
            description = "Registra um incidente em uma área da empresa do funcionário. O incidente nasce com status PENDING.\n\n"
                    + "**Regras:** sem nível de contaminação informado, assume o risco padrão do tipo de resíduo. "
                    + "Se o resíduo é perigoso ou a contaminação é HIGH, a prioridade é elevada para no mínimo HIGH.\n\n"
                    + "**Acesso:** EMPLOYEE."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Incidente registrado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Área ou tipo de resíduo não encontrados")
    })
    IncidentResponseDTO create(IncidentRequestDTO dto, @Parameter(hidden = true) AuthenticatedUser author);

    @Operation(
            summary = "Lista os incidentes da empresa",
            description = "Retorna os incidentes da empresa do usuário. Todos os filtros são opcionais. "
                    + "**Acesso:** ADMIN, MANAGER, EMPLOYEE."
    )
    @ApiResponse(responseCode = "200", description = "Lista de incidentes")
    List<IncidentResponseDTO> show(
            @ParameterObject IncidentFilterDTO filter,
            @Parameter(hidden = true) AuthenticatedUser author
    );

    @Operation(summary = "Busca um incidente pelo ID", description = "**Acesso:** ADMIN, MANAGER, EMPLOYEE.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Incidente encontrado"),
            @ApiResponse(responseCode = "404", description = "Incidente não encontrado")
    })
    IncidentResponseDTO show(
            @Parameter(description = "ID do incidente") UUID id,
            @Parameter(hidden = true) AuthenticatedUser author
    );

    @Operation(
            summary = "Edita um incidente",
            description = "Altera as informações principais do incidente (título, tipo de resíduo, descrição da IA, contaminação, "
                    + "quantidade e prioridade) e registra o motivo da alteração. Campos omitidos não mudam. "
                    + "Os dados informados pelo funcionário (área, foto e descrição) não são editáveis.\n\n"
                    + "**Regras:** só incidentes PENDING podem ser editados. A prioridade é elevada para no mínimo HIGH "
                    + "se o resíduo é perigoso ou a contaminação é HIGH.\n\n"
                    + "**Acesso:** ADMIN, MANAGER."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Incidente atualizado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Incidente ou tipo de resíduo não encontrados"),
            @ApiResponse(responseCode = "422", description = "Incidente não está pendente")
    })
    IncidentResponseDTO update(
            @Parameter(description = "ID do incidente") UUID id,
            IncidentUpdateRequestDTO dto,
            @Parameter(hidden = true) AuthenticatedUser author
    );

    @Operation(
            summary = "Aprova um incidente",
            description = "Muda o status do incidente de PENDING para APPROVED. Depois de aprovado, ele não pode mais ser editado.\n\n"
                    + "**Acesso:** ADMIN, MANAGER."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Incidente aprovado"),
            @ApiResponse(responseCode = "404", description = "Incidente não encontrado"),
            @ApiResponse(responseCode = "422", description = "Incidente não está pendente")
    })
    IncidentResponseDTO approve(
            @Parameter(description = "ID do incidente") UUID id,
            @Parameter(hidden = true) AuthenticatedUser author
    );

    @Operation(
            summary = "Recusa um incidente",
            description = "Muda o status do incidente de PENDING para REJECTED e registra o motivo da recusa no campo changeReason. "
                    + "Depois de recusado, ele não pode mais ser editado nem aprovado, e não aceita pedido de coleta.\n\n"
                    + "**Acesso:** ADMIN, MANAGER."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Incidente recusado"),
            @ApiResponse(responseCode = "400", description = "Motivo ausente ou inválido"),
            @ApiResponse(responseCode = "404", description = "Incidente não encontrado"),
            @ApiResponse(responseCode = "422", description = "Incidente não está pendente")
    })
    IncidentResponseDTO reject(
            @Parameter(description = "ID do incidente") UUID id,
            IncidentRejectRequestDTO dto,
            @Parameter(hidden = true) AuthenticatedUser author
    );

    @Operation(
            summary = "Encerra um incidente",
            description = "Muda o status do incidente para CLOSED.\n\n"
                    + "**Regras:** não é possível encerrar com coleta em andamento. Incidente com resíduo perigoso só pode ser "
                    + "encerrado depois de uma coleta concluída (PNRS, art. 27, §1º).\n\n"
                    + "**Acesso:** ADMIN, MANAGER."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Incidente encerrado"),
            @ApiResponse(responseCode = "404", description = "Incidente não encontrado"),
            @ApiResponse(responseCode = "422", description = "Incidente já encerrado, com coleta ativa ou resíduo perigoso sem coleta concluída")
    })
    void closeIncident(
            @Parameter(description = "ID do incidente") UUID id,
            @Parameter(hidden = true) AuthenticatedUser author
    );

    @Operation(
            summary = "Consulta o relatório de IA do incidente",
            description = "Retorna o relatório mais recente gerado pela IA para o incidente. **Acesso:** ADMIN, MANAGER, EMPLOYEE."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Relatório encontrado"),
            @ApiResponse(responseCode = "404", description = "Incidente ou relatório não encontrados")
    })
    AiReportResponseDTO showAiReport(
            @Parameter(description = "ID do incidente") UUID id,
            @Parameter(hidden = true) AuthenticatedUser author
    );

    @Operation(
            summary = "Consulta o contexto de score da ocorrência",
            description = "Retorna os dados da ocorrência usados no cálculo de score de cooperativas. "
                    + "Sem nível de contaminação informado, devolve o risco padrão do resíduo. "
                    + "O campo origin traz as coordenadas da empresa e é nulo enquanto ela não as tiver cadastradas. "
                    + "**Acesso:** ADMIN, MANAGER, EMPLOYEE."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contexto encontrado"),
            @ApiResponse(responseCode = "404", description = "Incidente não encontrado")
    })
    IncidentScoringContextDTO showScoringContext(
            @Parameter(description = "ID do incidente") UUID id,
            @Parameter(hidden = true) AuthenticatedUser author
    );
}
