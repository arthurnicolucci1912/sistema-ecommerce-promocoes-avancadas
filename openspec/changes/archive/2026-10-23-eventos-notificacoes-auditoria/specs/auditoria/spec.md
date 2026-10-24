## Purpose

Permitir que administradores consultem quem realizou operações administrativas relevantes e quais dados comerciais foram alterados, preservando uma trilha somente de leitura.

## ADDED Requirements

### Requirement: Registro atômico das operações administrativas
O sistema SHALL registrar operações administrativas bem-sucedidas de catálogo, ajuste de estoque, campanhas, cupons, gestão de usuários e andamento ou cancelamento administrativo de pedidos. Cada registro MUST conter identificador, usuário responsável, instante, operação, tipo e identificador de recurso e alterações relevantes. Alterações de preço MUST conter valores anterior e novo; ajustes de estoque MUST conter quantidade e motivo; cancelamentos MUST conter estado anterior e motivo. O registro MUST integrar a mesma transação da operação.

#### Scenario: Preço alterado
- **WHEN** um operador altera o preço de uma variação ou combo
- **THEN** a trilha registra responsável, recurso, momento e preços anterior e novo

#### Scenario: Campanha criada
- **WHEN** um usuário de promoções cria uma campanha
- **THEN** a trilha registra usuário, operação e identificador da campanha criada

#### Scenario: Cancelamento administrativo
- **WHEN** um administrador cancela um pedido pago
- **THEN** pedido, efeitos comerciais e auditoria são confirmados juntos e o registro contém responsável, estado anterior e motivo

#### Scenario: Operação rejeitada ou revertida
- **WHEN** uma operação administrativa falha por validação, permissão, concorrência ou rollback posterior
- **THEN** não existe registro que a apresente como concluída

#### Scenario: Dados sensíveis
- **WHEN** um administrador cria um usuário
- **THEN** a trilha identifica a operação e o recurso sem armazenar senha, hash, token ou cabeçalho de autenticação

### Requirement: Consulta restrita e paginada
GET /api/admin/auditoria SHALL permitir exclusivamente a ADMIN consultar a trilha, com filtros opcionais por usuarioId, operacao, inicio e fim, usando instantes ISO-8601 e intervalo inclusivo. A ordenação SHALL ser momento decrescente e identificador decrescente. A paginação SHALL usar page a partir de zero e size entre 1 e 100, com padrão 20; intervalo invertido ou filtros malformados MUST responder 400.

#### Scenario: Filtros combinados
- **WHEN** um ADMIN consulta usuário, operação e período com page e size válidos
- **THEN** recebe somente os registros correspondentes e o total de elementos

#### Scenario: Sem resultados
- **WHEN** nenhum registro corresponde aos filtros
- **THEN** a consulta responde 200 com conteúdo vazio e total zero

#### Scenario: Acesso negado
- **WHEN** alguém sem autenticação ou com perfil diferente de ADMIN consulta a trilha
- **THEN** recebe respectivamente 401 ou 403

#### Scenario: Consulta inválida
- **WHEN** a consulta contém início posterior ao fim, identificador inválido, página negativa ou tamanho fora do limite
- **THEN** recebe 400 sem consultar uma lista ilimitada

### Requirement: Imutabilidade da trilha pela API
Os registros SHALL permanecer somente leitura pela API, inclusive para ADMIN.

#### Scenario: Tentativa de edição ou exclusão
- **WHEN** um ADMIN tenta editar ou excluir um registro pela API
- **THEN** nenhuma alteração ocorre e não existe rota que permita a operação
