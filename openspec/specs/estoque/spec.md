# estoque Specification

## Purpose
Controla a disponibilidade de cada variação, impede vender o que não existe (inclusive sob concorrência) e mantém o estoque consistente com o ciclo de vida dos pedidos.

## Requirements

### Requirement: Saldo por variação
Cada variação SHALL ter um saldo com quantidade disponível e quantidade reservada, ambas nunca negativas. Toda variação cadastrada SHALL passar a ter saldo zero automaticamente.

#### Scenario: Variação nova
- **WHEN** uma variação é cadastrada no catálogo
- **THEN** o estoque dela existe com 0 disponível e 0 reservado

### Requirement: Ajuste administrativo de estoque
Um usuário `ADMIN` ou `ESTOQUE` SHALL poder ajustar o disponível de uma variação somando ou subtraindo uma quantidade, com motivo obrigatório. O ajuste MUST ser rejeitado com 409 se deixar o disponível negativo. Outros perfis MUST receber 403.

#### Scenario: Entrada de mercadoria
- **WHEN** um usuário `ESTOQUE` ajusta +10 a variação M com o motivo "compra do fornecedor"
- **THEN** o disponível da variação M aumenta em 10

#### Scenario: Ajuste que deixaria negativo
- **WHEN** a variação M tem 3 disponíveis e alguém ajusta -5
- **THEN** o sistema responde 409 e o disponível continua 3

#### Scenario: Perfil comercial ajusta estoque
- **WHEN** um `OPERADOR_COMERCIAL` tenta ajustar o estoque
- **THEN** o sistema responde 403

### Requirement: Reserva tudo-ou-nada por pedido
O sistema SHALL reservar, para um pedido, as quantidades de um conjunto de componentes de estoque. Para cada componente, a quantidade passa do disponível para o reservado.
- Se algum componente não tiver disponível suficiente, nenhuma quantidade MUST ser reservada.
- Nesse caso, o erro MUST identificar o componente insuficiente e a quantidade disponível.
- Reservar novamente para um pedido que já tem reserva MUST ser rejeitado.

#### Scenario: Reserva bem-sucedida
- **WHEN** um pedido reserva 2 da variação M, que tem 5 disponíveis
- **THEN** a variação M fica com 3 disponíveis e 2 reservados

#### Scenario: Estoque insuficiente
- **WHEN** um pedido reserva 1 caneca (10 disponíveis) e 4 cafés (3 disponíveis)
- **THEN** a reserva é rejeitada informando o café com 3 disponíveis, e caneca e café permanecem com os saldos originais

### Requirement: Última unidade sob concorrência
Reservas simultâneas da mesma variação MUST NOT reservar, somadas, mais do que o disponível. O disponível MUST NOT ficar negativo.

#### Scenario: Dez pedidos pela última unidade
- **WHEN** dez pedidos tentam reservar ao mesmo tempo a única unidade disponível de uma variação
- **THEN** exatamente um consegue, os outros nove recebem estoque insuficiente, e a variação termina com 0 disponível e 1 reservado

### Requirement: Liberação, baixa e reposição idempotentes
O sistema SHALL tratar a reserva de um pedido em três operações:
- **liberação:** o reservado volta ao disponível;
- **baixa:** o reservado sai do estoque;
- **reposição:** uma reserva já baixada volta ao disponível.

Cada operação MUST ter efeito uma única vez por pedido. Repetições e operações sem reserva aplicável MUST ser ignoradas sem erro.

#### Scenario: Liberar duas vezes
- **WHEN** a reserva de 2 unidades de um pedido é liberada duas vezes
- **THEN** o disponível aumenta 2 uma única vez

#### Scenario: Baixa
- **WHEN** a reserva de 2 unidades de um pedido sofre baixa
- **THEN** o reservado diminui 2 e o disponível não muda

#### Scenario: Pedido sem reserva
- **WHEN** a liberação é pedida para um pedido que nunca reservou
- **THEN** nada muda e nenhum erro é lançado

### Requirement: Estoque acompanha o ciclo de vida do pedido
O estoque SHALL reagir aos acontecimentos do pedido sem que o módulo de pedidos o conheça:
- pagamento aprovado: baixa da reserva;
- pagamento recusado: liberação;
- cancelamento a partir de `AGUARDANDO_PAGAMENTO`: liberação;
- cancelamento a partir de `PAGO` ou `EM_PREPARACAO`: reposição.

O efeito sobre o estoque MUST acontecer na mesma transação da mudança do pedido.

#### Scenario: Cancelamento antes do pagamento
- **WHEN** um pedido em `AGUARDANDO_PAGAMENTO` com 2 unidades reservadas é cancelado
- **THEN** as 2 unidades voltam ao disponível

#### Scenario: Pagamento aprovado
- **WHEN** o pagamento de um pedido com 2 unidades reservadas é aprovado
- **THEN** o reservado diminui 2

#### Scenario: Cancelamento depois do pagamento
- **WHEN** um pedido `PAGO` com 2 unidades baixadas é cancelado pelo administrador
- **THEN** as 2 unidades voltam ao disponível

#### Scenario: Novo interessado sem alterar o pedido
- **WHEN** o estoque passa a reagir a um acontecimento do pedido
- **THEN** nenhuma alteração no módulo de pedidos é necessária

### Requirement: Consulta de estoque administrativa
Um usuário `ADMIN` ou `ESTOQUE` SHALL poder consultar o disponível e o reservado de uma variação e listar as variações com disponível abaixo de um limite informado.

#### Scenario: Estoque baixo
- **WHEN** um usuário `ESTOQUE` lista as variações com disponível abaixo de 5
- **THEN** o sistema devolve apenas as variações com menos de 5 disponíveis
