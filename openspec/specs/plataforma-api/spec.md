# plataforma-api Specification

## Purpose
Define o comportamento transversal da API, válido para todos os módulos: formato único de erros, documentação navegável e verificação de saúde da aplicação.

## Requirements

### Requirement: Formato padronizado de erros
Toda resposta de erro da API SHALL usar o formato Problem Details (RFC 9457), com `type`, `title`, `status`, `detail` e `instance`. Erros de validação MUST incluir a lista de campos inválidos com a mensagem de cada um. Erros inesperados MUST responder 500 sem expor stack trace nem detalhes internos.

#### Scenario: Erro de validação
- **WHEN** uma requisição tem dois campos inválidos
- **THEN** o sistema responde 400 em Problem Details, com os dois campos e suas mensagens

#### Scenario: Recurso inexistente
- **WHEN** uma requisição referencia um recurso que não existe
- **THEN** o sistema responde 404 em Problem Details

#### Scenario: Erro inesperado
- **WHEN** ocorre uma falha não tratada durante uma requisição
- **THEN** o sistema responde 500 em Problem Details, sem stack trace nem nomes de classes internas

#### Scenario: Erros de segurança no mesmo formato
- **WHEN** uma requisição é rejeitada com 401 ou 403
- **THEN** o corpo da resposta segue o formato Problem Details

### Requirement: Documentação da API
O sistema SHALL publicar a especificação OpenAPI e uma interface navegável (Swagger UI) sem exigir autenticação. A interface MUST permitir informar um token Bearer para chamar endpoints protegidos.

#### Scenario: Especificação disponível
- **WHEN** alguém acessa a especificação OpenAPI sem token
- **THEN** o sistema devolve um documento OpenAPI que lista os endpoints de autenticação, cliente e administração de usuários

#### Scenario: Chamada autenticada pela documentação
- **WHEN** um usuário informa um token válido na interface de documentação e chama um endpoint protegido
- **THEN** a requisição é autenticada

### Requirement: Verificação de saúde
O sistema SHALL expor uma verificação de saúde pública que indique se a aplicação e o banco de dados estão disponíveis, sem expor detalhes de configuração.

#### Scenario: Aplicação saudável
- **WHEN** a aplicação está em execução e o banco responde
- **THEN** a verificação de saúde responde 200 com status `UP`

### Requirement: Esquema do banco versionado
O esquema do banco SHALL ser criado e evoluído somente por migrações versionadas, aplicadas automaticamente na inicialização. A aplicação MUST NOT gerar ou alterar o esquema automaticamente a partir das entidades.

#### Scenario: Banco vazio
- **WHEN** a aplicação inicia contra um banco vazio
- **THEN** as migrações criam todo o esquema necessário e a aplicação fica disponível

#### Scenario: Esquema divergente
- **WHEN** as entidades exigem uma estrutura ausente nas migrações
- **THEN** a inicialização falha na validação do esquema, em vez de alterá-lo
