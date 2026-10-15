# Proposal

## Why

A etapa 4 do roadmap junta duas coisas com dependências diferentes. O **pedido** (snapshot comercial, ciclo de vida, cancelamento, histórico e consultas) não depende de nenhum outro módulo. Já o **checkout** depende do carrinho e do estoque, que ainda não existem. Implementar o pedido agora entrega o ciclo de vida exigido pelo enunciado ("cada pedido deverá possuir um ciclo de vida", "definir quais transições são válidas", "cancelamento deverá permanecer registrado no histórico"). Também deixa pronto o ponto de integração que o checkout, o pagamento e o estoque vão usar.

## What Changes

- Módulo `pedido` com o agregado Pedido e o snapshot imutável dos dados comerciais: itens, preços unitários, descontos aplicados, frete, totais e endereço de entrega.
- Ciclo de vida com 8 estados e as 11 transições válidas de `docs/06-ciclo-vida-pedido.md`. Qualquer outra transição é rejeitada sem efeito.
- Histórico de cada transição com estado de origem, estado de destino, momento, responsável e motivo (obrigatório no cancelamento).
- Serviço de aplicação para criar o pedido a partir de um snapshot, que o checkout vai usar na próxima change, e para levar o pedido a `AGUARDANDO_PAGAMENTO`.
- Consultas do cliente (só os próprios pedidos) e administrativas, avanço do andamento (preparação, envio, entrega) e cancelamento com regras por perfil.
- Eventos de domínio a cada transição (`PedidoCriado`, `PedidoEmPreparacao`, `PedidoEnviado`, `PedidoEntregue`, `PedidoCancelado`), para que estoque, pagamento e notificação reajam sem que o pedido os conheça.
- **Temporário:** endpoint administrativo de simulação do resultado do pagamento, para tornar o fluxo exercitável até a etapa 6 (pagamento), que o substitui.
- Fora de escopo: o checkout (próxima change, depois do carrinho), reserva e baixa de estoque, estorno real e expiração automática do pagamento.

## Design Patterns

| Pattern | Onde | Requisito do PDF que justifica |
|---|---|---|
| **State** (comportamental) | Ciclo de vida do pedido: um objeto de estado por situação, cada um aceitando só as próprias transições | Pedidos: "cada pedido deverá possuir um ciclo de vida… definir quais transições entre essas situações são válidas"; Cancelamento: "definir em quais estados o cancelamento é permitido" |
| **Observer** (comportamental), lado do publicador | O pedido publica eventos de domínio pelo `PublicadorEventos` da fundação; os ouvintes chegam nas etapas de estoque, pagamento e notificação | Eventos do sistema: "evitar que uma única classe conheça diretamente todos os componentes interessados" |

A Facade do checkout fica para a change `checkout`. As consultas e o histórico são CRUD sem ponto de variação e não introduzem pattern próprio.

## Capabilities

### New Capabilities
- `pedido`: snapshot comercial do pedido, ciclo de vida com transições válidas e inválidas, cancelamento por perfil, histórico, consultas do cliente e administrativas, aviso de acontecimentos do pedido a interessados e simulação temporária do pagamento.

### Modified Capabilities

Nenhuma.

## Impact

- **Código novo:** módulo `br.com.fatec.ecommerce.pedido` (domínio, aplicação, persistência e web) e a migração Flyway `V2`.
- **API nova:**
  - `/api/clientes/me/pedidos/**`
  - `/api/admin/pedidos/**`
  - `/api/admin/pedidos/{id}/pagamento-simulado` (temporário)
- **Segurança:** novas regras de rota para os perfis `ADMIN`, `OPERADOR_COMERCIAL` e `ESTOQUE`.
- **Documentação:** `docs/02`, `03`, `04` (State com classes reais), `05`, `06`, `08` (divisão da etapa 4), `09` e `12`.
