# pedido Specification

## Purpose
Registra cada compra como um pedido que preserva os dados comerciais do momento da compra e percorre um ciclo de vida com transições controladas, cancelamento regrado e histórico consultável.

## Requirements

### Requirement: Snapshot comercial do pedido
Ao ser criado, o pedido SHALL guardar uma cópia dos dados comerciais: para cada item, identificação, nome, descrição da variação, quantidade e preço unitário; os descontos aplicados, com nome e valor; o frete; o subtotal, o total de descontos e o total; e o endereço de entrega. Esses dados MUST NOT mudar depois da criação, mesmo que preços, promoções ou o endereço do cliente mudem.

#### Scenario: Pedido criado em AGUARDANDO_PAGAMENTO
- **WHEN** um pedido é criado a partir de um snapshot válido e a reserva é confirmada
- **THEN** o pedido fica em `AGUARDANDO_PAGAMENTO` com itens, descontos, frete e totais iguais aos do snapshot

#### Scenario: Preço alterado depois da compra
- **WHEN** um item foi comprado a R$ 220,00 e o snapshot de um novo pedido do mesmo item informa R$ 250,00
- **THEN** o pedido antigo continua mostrando R$ 220,00

#### Scenario: Endereço alterado depois da compra
- **WHEN** o cliente altera ou remove o endereço usado em um pedido
- **THEN** o pedido continua mostrando o endereço do momento da compra

#### Scenario: Totais inconsistentes
- **WHEN** o snapshot informa um total diferente de subtotal menos descontos mais frete
- **THEN** o pedido não é criado e o sistema rejeita o snapshot

### Requirement: Transições válidas do ciclo de vida
O pedido SHALL ter os estados `CRIADO`, `AGUARDANDO_PAGAMENTO`, `PAGO`, `PAGAMENTO_RECUSADO`, `EM_PREPARACAO`, `ENVIADO`, `ENTREGUE` e `CANCELADO`. As únicas transições válidas são:
- `CRIADO` para `AGUARDANDO_PAGAMENTO`;
- `AGUARDANDO_PAGAMENTO` para `PAGO` ou para `PAGAMENTO_RECUSADO`;
- `PAGO` para `EM_PREPARACAO`;
- `EM_PREPARACAO` para `ENVIADO`;
- `ENVIADO` para `ENTREGUE`;
- `CANCELADO` a partir de `CRIADO`, `AGUARDANDO_PAGAMENTO`, `PAGAMENTO_RECUSADO`, `PAGO` ou `EM_PREPARACAO`.

#### Scenario: Fluxo completo
- **WHEN** um pedido em `AGUARDANDO_PAGAMENTO` tem o pagamento aprovado, entra em preparação, é enviado e é entregue
- **THEN** o pedido termina em `ENTREGUE`, com uma entrada de histórico por transição

### Requirement: Transições inválidas são rejeitadas
Toda transição fora da lista válida MUST ser rejeitada com 409, informando o estado atual e a operação tentada. O pedido, o histórico e os eventos MUST permanecer inalterados.

#### Scenario: Cancelar pedido enviado
- **WHEN** alguém tenta cancelar um pedido em `ENVIADO`
- **THEN** o sistema responde 409 informando o estado `ENVIADO` e a operação de cancelamento, e o pedido continua `ENVIADO`

#### Scenario: Pular etapa
- **WHEN** alguém tenta enviar um pedido em `AGUARDANDO_PAGAMENTO`
- **THEN** o sistema responde 409 e o pedido continua `AGUARDANDO_PAGAMENTO`

#### Scenario: Estados finais
- **WHEN** alguém tenta qualquer transição em um pedido `ENTREGUE` ou `CANCELADO`
- **THEN** o sistema responde 409 e o pedido não muda

#### Scenario: Pagamento recusado não reabre o pedido
- **WHEN** alguém tenta aprovar o pagamento de um pedido em `PAGAMENTO_RECUSADO`
- **THEN** o sistema responde 409 e o pedido continua `PAGAMENTO_RECUSADO`

### Requirement: Cancelamento por perfil e com motivo
O cancelamento MUST exigir um motivo não vazio.
- O cliente SHALL poder cancelar o próprio pedido em `CRIADO`, `AGUARDANDO_PAGAMENTO` ou `PAGAMENTO_RECUSADO`.
- Os perfis `ADMIN` e `OPERADOR_COMERCIAL` SHALL poder cancelar qualquer pedido em qualquer estado que admite cancelamento, inclusive `PAGO` e `EM_PREPARACAO`.
- Uma tentativa do cliente de cancelar um pedido em `PAGO` ou `EM_PREPARACAO` MUST responder 409 e deixar o pedido inalterado.

#### Scenario: Cliente cancela pedido aguardando pagamento
- **WHEN** o cliente cancela o próprio pedido em `AGUARDANDO_PAGAMENTO`, informando um motivo
- **THEN** o pedido vai para `CANCELADO` e o histórico registra o cliente como responsável, junto com o motivo

#### Scenario: Cliente tenta cancelar pedido pago
- **WHEN** o cliente tenta cancelar o próprio pedido em `PAGO`
- **THEN** o sistema responde 409 e o pedido continua `PAGO`

#### Scenario: Administrador cancela pedido pago
- **WHEN** um `ADMIN` cancela um pedido em `PAGO` informando um motivo
- **THEN** o pedido vai para `CANCELADO` e o histórico registra o administrador como responsável, o estado anterior `PAGO` e o motivo

#### Scenario: Cancelamento sem motivo
- **WHEN** alguém cancela um pedido sem informar motivo
- **THEN** o sistema responde 400 e o pedido não muda

### Requirement: Histórico de transições
Cada transição bem-sucedida SHALL registrar uma entrada de histórico com estado de origem, estado de destino, momento, responsável (cliente, usuário administrativo ou sistema) e motivo, quando houver. O histórico MUST ser devolvido em ordem cronológica junto com o pedido.

#### Scenario: Histórico após envio
- **WHEN** um pedido passa por `AGUARDANDO_PAGAMENTO`, `PAGO`, `EM_PREPARACAO` e `ENVIADO`
- **THEN** a consulta do pedido devolve as entradas de histórico dessas transições em ordem cronológica, cada uma com o responsável

### Requirement: Consulta de pedidos pelo cliente
Um cliente autenticado SHALL poder listar os próprios pedidos e consultar o detalhe de cada um: itens, descontos, frete, totais, endereço, estado atual e histórico. Um pedido de outro cliente MUST responder 404.

#### Scenario: Listar os próprios pedidos
- **WHEN** um cliente com dois pedidos lista seus pedidos
- **THEN** o sistema devolve os dois, do mais recente para o mais antigo, e nenhum pedido de outro cliente

#### Scenario: Pedido de outro cliente
- **WHEN** o cliente A consulta ou cancela um pedido do cliente B
- **THEN** o sistema responde 404 e o pedido do cliente B não muda

### Requirement: Gestão administrativa de pedidos
Os perfis `ADMIN` e `OPERADOR_COMERCIAL` SHALL poder listar pedidos de qualquer cliente, com filtro opcional por estado, e consultar o detalhe. Os perfis `ADMIN` e `ESTOQUE` SHALL poder iniciar a preparação, registrar o envio e registrar a entrega. Qualquer outro perfil MUST receber 403.

#### Scenario: Filtrar por estado
- **WHEN** um `OPERADOR_COMERCIAL` lista os pedidos com o filtro `PAGO`
- **THEN** o sistema devolve apenas pedidos em `PAGO`

#### Scenario: Estoque avança o andamento
- **WHEN** um usuário `ESTOQUE` inicia a preparação de um pedido `PAGO`
- **THEN** o pedido vai para `EM_PREPARACAO`

#### Scenario: Perfil sem permissão
- **WHEN** um usuário `PROMOCOES` tenta registrar o envio de um pedido
- **THEN** o sistema responde 403

#### Scenario: Cliente na área administrativa
- **WHEN** um `CLIENTE` lista `/api/admin/pedidos`
- **THEN** o sistema responde 403

### Requirement: Aviso de acontecimentos do pedido
Cada transição bem-sucedida SHALL emitir um evento do pedido com identificador do pedido, cliente, estado anterior, novo estado e momento. Um novo interessado nesses eventos MUST poder reagir a eles sem nenhuma alteração no módulo de pedidos. Uma transição rejeitada MUST NOT emitir evento.

#### Scenario: Novo interessado no cancelamento
- **WHEN** um novo componente interessado em cancelamentos é registrado, sem alteração no módulo de pedidos, e um pedido `PAGO` é cancelado
- **THEN** o componente recebe o evento de cancelamento com estado anterior `PAGO`

#### Scenario: Transição rejeitada não emite evento
- **WHEN** uma tentativa de cancelar um pedido `ENTREGUE` é rejeitada
- **THEN** nenhum evento é emitido

### Requirement: Simulação temporária do resultado do pagamento
Até a integração real de pagamento, um `ADMIN` SHALL poder simular a aprovação ou a recusa do pagamento de um pedido em `AGUARDANDO_PAGAMENTO`. O pedido vai para `PAGO` ou `PAGAMENTO_RECUSADO`, e o sistema registra como responsável o sistema, com a indicação de simulação. O recurso MUST ser documentado como temporário.

#### Scenario: Aprovação simulada
- **WHEN** um `ADMIN` simula a aprovação de um pedido em `AGUARDANDO_PAGAMENTO`
- **THEN** o pedido vai para `PAGO` e o histórico indica simulação

#### Scenario: Recusa simulada
- **WHEN** um `ADMIN` simula a recusa de um pedido em `AGUARDANDO_PAGAMENTO`
- **THEN** o pedido vai para `PAGAMENTO_RECUSADO`

### Requirement: Alterações concorrentes
Duas transições simultâneas sobre o mesmo pedido MUST NOT ser ambas aplicadas a partir do mesmo estado. A segunda MUST ser rejeitada com 409, sem efeito.

#### Scenario: Cancelamento e envio simultâneos
- **WHEN** um pedido `EM_PREPARACAO` recebe ao mesmo tempo um envio e um cancelamento
- **THEN** apenas uma das operações é aplicada e a outra responde 409
