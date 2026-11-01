# Volta API

API REST do **Volta**, plataforma de gestão de resíduos que conecta empresas geradoras a cooperativas de reciclagem. A API cobre o registro de incidentes com resíduos nas áreas da empresa, a solicitação e o acompanhamento de coletas, a avaliação das cooperativas e os indicadores ambientais (ESG).

As regras de negócio seguem a legislação ambiental brasileira: Política Nacional de Resíduos Sólidos (Lei 12.305/2010), Resolução CONAMA 275/2001 e ABNT NBR 10004.

---

## Sumário

- [Tecnologias](#tecnologias)
- [Arquitetura](#arquitetura)
- [Estrutura de pastas](#estrutura-de-pastas)
- [Variáveis de ambiente](#variáveis-de-ambiente)
- [Documentação da API (Swagger)](#documentação-da-api-swagger)
- [Autenticação e perfis de acesso](#autenticação-e-perfis-de-acesso)
- [Regras de negócio](#regras-de-negócio)
- [Banco de dados](#banco-de-dados)
- [Tratamento de erros](#tratamento-de-erros)
- [CI/CD](#cicd)
- [Licença](#licença)

---

## Tecnologias

| Tecnologia | Versão | Uso |
|---|---|---|
| Java | 21 | Linguagem |
| Spring Boot | 4.0.7 | Framework base |
| Spring Web MVC | — | API REST |
| Spring Data JPA / Hibernate | — | Persistência |
| Spring Security + JJWT | 0.12.6 | Autenticação stateless com JWT |
| Bean Validation | — | Validação dos dados de entrada |
| PostgreSQL | 16 | Banco de dados (Neon em nuvem) |
| Flyway | — | Versionamento do banco |
| springdoc-openapi | 3.0.2 | Documentação Swagger / OpenAPI |
| Spring Boot Actuator | — | Health check |
| Lombok | — | Redução de código repetitivo |
| JUnit 5, Mockito e AssertJ | — | Testes unitários e de segurança |
| JaCoCo | 0.8.13 | Cobertura de testes, com limite mínimo no build |
| Docker | — | Empacotamento da aplicação |
| GitHub Actions | — | Integração e entrega contínuas |

---

## Arquitetura

A aplicação segue uma arquitetura em camadas. Cada requisição percorre o fluxo abaixo:

```
Controller  →  UseCase (interface)  →  Service  →  Repository / Procedure / Function  →  PostgreSQL
    │                                     │
    └── DTO de request/response           └── Mapper (DTO ⇄ entidade)
```

| Camada | Responsabilidade |
|---|---|
| **Controller** | Recebe a requisição HTTP, valida o DTO (`@Valid`), checa o perfil (`@PreAuthorize`) e devolve a resposta |
| **Controller Docs** | Interfaces com as anotações do Swagger, para manter os controllers limpos |
| **UseCase** | Contrato (interface) dos casos de uso que o controller consome |
| **Service** | Implementa os casos de uso e aplica as regras de negócio |
| **Mapper** | Converte entre DTOs e entidades |
| **Repository** | Acesso a dados via Spring Data JPA |
| **Procedure / Function** | Chamadas às procedures e functions PL/pgSQL do banco |
| **Enums** | Valores de domínio e regras associadas a eles (transições de status, prazos, categorias de resíduo) |
| **Utils** | Utilitários de normalização (CNPJ) |
| **Handler** | Tratamento global de exceções no formato `ProblemDetail` |

---

## Estrutura de pastas

```
src/main/java/com/volta/api
├── config/              # Segurança, Swagger, propriedades e bootstrap do administrador
├── controller/          # Endpoints REST
│   └── docs/            # Interfaces de documentação do Swagger
├── database/
│   ├── entity/          # Entidades JPA
│   ├── repository/      # Repositórios Spring Data
│   ├── procedure/       # Chamadas às procedures do banco
│   └── function/        # Chamadas às functions do banco
├── dto/                 # DTOs de entrada e saída, agrupados por domínio
│   ├── area/
│   ├── auth/
│   ├── collection/
│   ├── company/
│   ├── cooperative/
│   ├── esg/
│   ├── incident/
│   ├── report/
│   ├── review/
│   ├── scoring/
│   ├── summary/
│   ├── user/
│   └── wastetype/
├── enums/               # Enums de domínio e regras associadas
├── exception/           # Exceções de negócio
├── handler/             # Tratamento global de erros e de autenticação
├── mapper/              # Conversão DTO ⇄ entidade
├── security/            # Filtro JWT, geração de token e usuário autenticado
├── service/             # Regras de negócio
├── usecase/             # Interfaces dos casos de uso
└── utils/               # Utilitários (normalização de CNPJ)

src/main/resources
├── application.yaml
└── db/migration/        # Scripts Flyway (V1 a V6)
```

---

## Variáveis de ambiente

| Variável | Obrigatória | Descrição | Exemplo |
|---|---|---|---|
| `DATABASE_HOST` | Sim | Host do banco usado na URL de conexão | `ep-xxx.sa-east-1.aws.neon.tech` |
| `DATABASE_NAME` | Sim | Nome do banco usado na URL de conexão | `neondb` |
| `DATABASE_USERNAME` | Sim | Usuário do banco | `volta` |
| `DATABASE_PASSWORD` | Sim | Senha do banco | — |
| `DATABASE_POOL_SIZE` | Não | Tamanho máximo do pool de conexões (padrão `5`) | `5` |
| `JWT_KEY` | Sim | Chave de assinatura do JWT (mínimo de 32 bytes; a aplicação não sobe com chave menor) | — |
| `JWT_EXPIRATION` | Não | Validade do token, em milissegundos (padrão `3600000`, 1 h) | `86400000` (24 h) |
| `SWAGGER_USERNAME` | Não | Usuário de acesso à documentação Swagger | `dev` |
| `SWAGGER_PASSWORD` | Não | Senha de acesso à documentação Swagger | — |
| `ADMIN_EMAIL` | Não | E-mail do administrador criado na inicialização | `admin@volta.com` |
| `ADMIN_PASSWORD` | Não | Senha do administrador criado na inicialização | — |
| `ADMIN_COMPANY_ID` | Não | ID da empresa do administrador | UUID |

> `SWAGGER_USERNAME` e `SWAGGER_PASSWORD` não impedem a aplicação de subir. Se estiverem ausentes ou vazias, o Swagger fica trancado: o navegador pede usuário e senha, mas nenhuma credencial é aceita até as duas variáveis serem definidas. O arquivo `.env.example` lista todas as variáveis.

> Por padrão, a URL do banco aponta para um Postgres com SSL obrigatório (Neon): `jdbc:postgresql://${DATABASE_HOST}/${DATABASE_NAME}?sslmode=require&channel_binding=require`.

### Administrador inicial

Na inicialização, o `AdminBootstrap` cria um usuário `ADMIN` se `ADMIN_EMAIL`,
`ADMIN_PASSWORD` e `ADMIN_COMPANY_ID` estiverem preenchidos **e** não existir
usuário com esse e-mail.

- **Roda a cada start e é idempotente**: se o e-mail já existe, não faz nada.
  Mudar `ADMIN_PASSWORD` depois **não** altera a senha do usuário existente.
- **A empresa precisa existir antes.** Como só um ADMIN cria empresas, a primeira
  empresa tem de ser inserida direto no banco (SQL) antes do primeiro start.
- **O perfil `ADMIN` vem da migração `V2__seed_roles.sql`.**
- **Erro na configuração derruba a aplicação.** `ADMIN_COMPANY_ID` com UUID inválido
  ou de empresa inexistente interrompe o start.
- **Depois de criar o admin, remova `ADMIN_PASSWORD` do ambiente.**
- Se alguma das três variáveis faltar, nada é criado (a aplicação registra um aviso).

---

## Documentação da API (Swagger)

A documentação é gerada automaticamente pelo springdoc-openapi e é a referência oficial dos endpoints: rotas, perfis de acesso, parâmetros, corpos de requisição, respostas e códigos de erro, em português.

| Recurso | URL |
|---|---|
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI (JSON) | `http://localhost:8080/api-docs` |

A documentação é **protegida por usuário e senha** (HTTP Basic), definidos em `SWAGGER_USERNAME` e `SWAGGER_PASSWORD`. Ao abrir o Swagger UI, o navegador pede essas credenciais.

**Para testar os endpoints pelo Swagger:**

1. Abra o Swagger UI e informe o usuário e a senha da documentação.
2. Em **Autenticação**, execute `POST /auth/login` e copie o `token` retornado.
3. Clique em **Authorize**, cole o token e confirme.
4. Os endpoints protegidos passam a enviar `Authorization: Bearer <token>`.

---

## Autenticação e perfis de acesso

A autenticação é **stateless** com JWT. O token é obtido em `POST /auth/login` e deve ser enviado em todas as requisições protegidas:

```
Authorization: Bearer <token>
```

A resposta do login traz `token`, a validade e o campo `firstLogin`, que é `true` apenas no primeiro login do usuário. Esse campo é controlado pela API e não pode ser alterado pelos endpoints de usuário.

O token carrega o ID do usuário, a empresa e o perfil. Incidentes, coletas, avaliações, métricas ESG e a listagem `/areas/mine` são restritos aos dados da **empresa do usuário autenticado**; um recurso de outra empresa retorna `404`. Os endpoints administrativos (empresas, cooperativas, usuários, tipos de resíduo e `/areas`) são exclusivos do `ADMIN` e enxergam toda a plataforma.

| Perfil | Descrição |
|---|---|
| `ADMIN` | Administra a plataforma: empresas, cooperativas, áreas, usuários e tipos de resíduo |
| `MANAGER` | Gestor da empresa: coletas, edição, aprovação e encerramento de incidentes, avaliações, ESG e consulta das áreas da própria empresa |
| `EMPLOYEE` | Funcionário: registra incidentes, consulta incidentes e áreas da própria empresa e atualiza o próprio perfil |

---

## Regras de negócio

### 1. Ciclo de vida da coleta

A coleta segue uma máquina de estados. Transições fora do fluxo são recusadas.

```
REQUESTED ──► SCHEDULED ──► IN_PROGRESS ──► COMPLETED
    │             │  ▲
    │             └──┘ (reagendamento)
    ▼             ▼
 CANCELED ◄───────┘
```

- `COMPLETED` e `CANCELED` são estados finais.
- Uma coleta só pode ser cancelada antes de começar, e o cancelamento exige observação.
- O agendamento é feito pelo endpoint `/schedule`, não pelo `/status`.

**Base:** rastreabilidade do resíduo prevista na PNRS (Lei 12.305/2010).

### 2. Ciclo de vida do incidente e responsabilidade do gerador

O incidente é registrado como `PENDING` por um funcionário e segue o fluxo `PENDING → APPROVED → CLOSED`.

- Só incidentes `PENDING` podem ser editados (`ADMIN` e `MANAGER`), e toda edição exige o motivo da alteração.
- Só incidentes `PENDING` podem ser aprovados (`ADMIN` e `MANAGER`).
- Um incidente pode ter **um ou mais tipos de resíduo**, informados em `wasteTypeIds` e guardados na tabela `incident_waste_type`. Na edição, a lista enviada substitui a anterior.
- Não é possível solicitar coleta para incidente encerrado.
- Um incidente só pode ter uma coleta ativa por vez.
- Um incidente com coleta em andamento não pode ser encerrado manualmente.
- Incidente com algum resíduo perigoso só pode ser encerrado depois de uma coleta concluída.
- Concluir a coleta encerra o incidente automaticamente e notifica o funcionário que o registrou.

**Base:** PNRS, art. 27, §1º. A contratação de terceiros não isenta o gerador da responsabilidade pelo resíduo.

### 3. Classificação do resíduo e compatibilidade com a cooperativa

Os tipos de resíduo usam as categorias da coleta seletiva da **CONAMA 275/2001**:

| Categoria | Cor | Perigosa |
|---|---|---|
| `PAPEL` | Azul | Não |
| `PLASTICO` | Vermelho | Não |
| `VIDRO` | Verde | Não |
| `METAL` | Amarelo | Não |
| `MADEIRA` | Preto | Não |
| `PERIGOSO` | Laranja | Sim |
| `SAUDE` | Branco | Sim |
| `RADIOATIVO` | Roxo | Sim |
| `ORGANICO` | Marrom | Não |
| `NAO_RECICLAVEL` | Cinza | Não |

- Resíduo com risco `HIGH` é tratado como **Classe I (perigoso)** da NBR 10004; os demais como Classe II.
- As categorias perigosas exigem nível de risco `HIGH`.
- Quando o incidente tem vários tipos de resíduo, **todos** precisam ser compatíveis com a cooperativa escolhida.
- A cooperativa só recebe a coleta se a categoria de cada resíduo estiver nas suas especialidades.
- Resíduo perigoso exige cooperativa com a especialidade `PERIGOSO`.
- Resíduo `RADIOATIVO` não pode ser destinado a cooperativas, pois é atribuição de empresa autorizada pela CNEN.

**Base:** CONAMA 275/2001, ABNT NBR 10004 e PNRS, arts. 37 e 38.

### 4. Prioridade e prazo de agendamento

- Se o nível de contaminação não for informado, assume-se o **maior** risco padrão entre os tipos de resíduo do incidente.
- Qualquer resíduo perigoso ou contaminação `HIGH` elevam a prioridade do incidente para no mínimo `HIGH`.
- Incidente `CRITICAL` gera coleta urgente.
- A coleta deve ser agendada dentro do prazo da prioridade, contado a partir da solicitação:

| Prioridade | Prazo máximo para agendamento |
|---|---|
| Urgente / `CRITICAL` | 24 horas |
| `HIGH` | 72 horas |
| `MEDIUM` | 7 dias |
| `LOW` | 15 dias |

Se o prazo já tiver vencido, a coleta deve ser agendada em até 24 horas.

**Base:** política interna da plataforma.

### 5. Avaliação das cooperativas

- Só coletas concluídas podem ser avaliadas, e cada coleta uma única vez.
- A nota média da cooperativa é recalculada a cada nova avaliação.

### 6. Dados para o cálculo de score

Dois endpoints entregam o contexto usado pelo serviço de recomendação de cooperativas:

- `GET /incidents/{id}/scoring-context`: prioridade, **lista** de tipos de resíduo (`wasteTypes`, com categoria normalizada e risco padrão), nível de contaminação, coordenadas da empresa e resumo do relatório de IA.
- `GET /cooperatives/scoring-data`: dados das cooperativas (localização, nota média e especialidades normalizadas).

### 7. Validação de CNPJ

- Os dígitos verificadores do CNPJ de empresas e cooperativas são validados.
- O CNPJ pode ser enviado com ou sem máscara, e o formato **alfanumérico** também é aceito.

**Base:** algoritmo de dígitos verificadores da Receita Federal e IN RFB 2.229/2024.

### 8. Métricas ESG

- O total reciclado não pode ser maior que o total de resíduos.
- Só é permitida uma métrica por empresa em cada período (`YYYY-MM`).
- O score ESG considera o percentual de reciclagem mais recente da empresa.

---

## Banco de dados

O schema é versionado com **Flyway** em `src/main/resources/db/migration`:

| Migration | Conteúdo |
|---|---|
| `V1__create_tables.sql` | Tabelas, chaves estrangeiras e constraints, incluindo as tabelas de junção `cooperative_waste_type` e `incident_waste_type` |
| `V2__seed_roles.sql` | Perfis de acesso (`ADMIN`, `EMPLOYEE`, `OPERATOR`, `MANAGER`) |
| `V3__create_procedure.sql` | Procedures de negócio |
| `V4__create_functions.sql` | Functions de cálculo |
| `V5__create_triggers.sql` | Tabela `audit_log`, função `audit_changes()` e triggers de auditoria |
| `V6__create_indexes.sql` | Índices de desempenho |

### Procedures

| Procedure | Descrição |
|---|---|
| `update_collection_status` | Atualiza o status da coleta e registra a mudança no histórico |
| `schedule_collection` | Agenda a coleta, muda o status para `SCHEDULED` e registra no histórico |
| `close_incident` | Encerra o incidente e cria uma notificação para o funcionário |

### Functions

| Function | Descrição |
|---|---|
| `calculate_recycling_percentage` | Calcula o percentual reciclado |
| `calculate_collection_completion_hours` | Calcula as horas entre a solicitação e a conclusão da coleta |
| `calculate_company_esg_score` | Retorna o score ESG da empresa |

### Entidades

O projeto mapeia como entidades JPA somente as tabelas usadas pela API: `Company`, `Users`, `Role`, `Area`, `WasteType`, `Incident`, `AiReport`, `Cooperative`, `Collection`, `CollectionStatus`, `Review` e `EsgMetric`. As tabelas `attachment`, `conversation`, `message`, `message_attachment` e `notification` existem no banco, mas ainda não têm entidade na aplicação. A tabela `notification` é preenchida pela procedure `close_incident`.

A relação entre incidente e tipos de resíduo (N:N) é mapeada com `@ManyToMany` em `Incident`, usando a tabela `incident_waste_type` (sem entidade própria; o `ON DELETE CASCADE` remove os vínculos junto com o incidente). A tabela `cooperative_waste_type` existe na V1, mas ainda não é mapeada: as especialidades da cooperativa continuam na coluna `specialties`.

---

## Tratamento de erros

Todos os erros, inclusive 401 e 403, seguem o formato **ProblemDetail** (RFC 9457) e são devolvidos com `Content-Type: application/problem+json`:

```json
{
  "type": "about:blank",
  "title": "Unprocessable Content",
  "status": 422,
  "detail": "Invalid status transition from REQUESTED to COMPLETED",
  "instance": "/collections/3fa85f64-5717-4562-b3fc-2c963f66afa6/status"
}
```

Erros de validação (400) incluem o campo `errors`, com a mensagem de cada campo:

```json
{
  "title": "Bad Request",
  "status": 400,
  "detail": "Validation error",
  "errors": {
    "cnpj": "Invalid CNPJ"
  }
}
```

| Código | Quando ocorre |
|---|---|
| `400` | Dados inválidos |
| `401` | Token ausente, inválido ou expirado, ou credenciais incorretas |
| `403` | Usuário sem permissão para o recurso |
| `404` | Recurso não encontrado |
| `409` | Conflito com dados existentes (CNPJ ou e-mail duplicado, coleta ativa) |
| `422` | Violação de regra de negócio, incluindo regras recusadas por procedures do banco (`RAISE EXCEPTION`) |
| `500` | Erro interno, incluindo falhas de acesso ao banco |

Mensagens de erro e de validação são retornadas em inglês.

### Datas e horários

A aplicação opera em **UTC**. Todos os campos de data e hora, enviados e retornados, estão em UTC no formato `yyyy-MM-ddTHH:mm:ss`, sem offset. O cliente é responsável por converter para o horário local (por exemplo, `America/Sao_Paulo`, UTC-3). Os prazos de agendamento também são calculados em UTC.

---

## CI/CD

Os pipelines ficam em `.github/workflows`:

| Workflow | Gatilho | Etapas |
|---|---|---|
| **CI** (`ci.yaml`) | Pull request para `develop` e `main` | Valida o PR, sobe um PostgreSQL 16 de teste, roda `mvn verify` e publica o relatório de testes |
| **CD** (`cd.yaml`) | Push em `develop` (QA) e `main` (produção), ou execução manual | Build e testes, depois gera e publica a imagem Docker pelo workflow reutilizável do `volta-devops` |

---

## Licença

Distribuído sob a licença MIT. Veja o arquivo [LICENSE](LICENSE).
