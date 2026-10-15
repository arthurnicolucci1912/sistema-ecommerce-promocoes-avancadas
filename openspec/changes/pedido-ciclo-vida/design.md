# Design

## Context

A fundação está pronta, com segurança JWT, `cliente`, `compartilhado` (`Dinheiro`, exceções, Problem Details, `EventoDominio`/`PublicadorEventos`) e ArchUnit. O motivo da change e o recorte da etapa 4 estão em `proposal.md`, e o comportamento em `specs/pedido/spec.md`. Os estados, transições e efeitos são os de `docs/06-ciclo-vida-pedido.md`. O contrato `EstadoPedido` foi previsto em `docs/03-arquitetura.md` §5.9 e ainda não existe.

## Goals / Non-Goals

**Goals:**
- Máquina de estados em que adicionar ou alterar um estado mexe em uma classe, não em um `switch` espalhado.
- Pedido pronto para o checkout (criação a partir de snapshot) e para o pagamento (aprovar ou recusar por um serviço de aplicação).
- Efeitos sobre outros módulos apenas por eventos.

**Non-Goals:**
- Checkout, reserva, baixa ou reposição de estoque e estorno: são os ouvintes dos eventos, nas próximas changes.
- Expiração automática do pagamento (T8 pelo sistema), que entra na etapa 6.
- Devolução, que é opcional no enunciado.

## Decisions

### D1. State com objetos de estado sem dados
A interface `EstadoPedido` (pacote `pedido.dominio.estado`) declara as operações `aguardarPagamento`, `confirmarPagamento`, `recusarPagamento`, `iniciarPreparacao`, `enviar`, `entregar` e `cancelar(Responsavel)`.
- Uma classe abstrata `EstadoPedidoBase` implementa todas as operações lançando `TransicaoInvalidaException` (estado atual e operação na mensagem).
- Cada estado concreto (`Criado`, `AguardandoPagamento`, `Pago`, `PagamentoRecusado`, `EmPreparacao`, `Enviado`, `Entregue`, `Cancelado`) sobrescreve só as próprias transições e devolve o próximo estado.
- Os estados finais não sobrescrevem nada.
- O enum `SituacaoPedido` (persistido como texto) conhece a instância de estado correspondente, `situacao.estado()`.

O agregado `Pedido` delega ao estado. Ao aplicar a transição, `Pedido.transicionar(...)` registra a entrada de histórico e acumula o evento a publicar.

A regra de cancelamento por perfil fica no estado: `Pago.cancelar(responsavel)` e `EmPreparacao.cancelar(responsavel)` exigem um responsável administrativo. Assim a política mora com o estado, não no controller.

Alternativas descartadas:
- `switch(situacao)` em cada operação do `Pedido`. Rejeitada porque cada estado novo exigiria editar todas as operações, que é exatamente a cadeia de condicionais que o enunciado pede para evitar.
- Biblioteca de máquina de estados (Spring Statemachine). Rejeitada porque é pesada para 8 estados e esconde o pattern que precisa ser defendido na apresentação.

### D2. Agregado e persistência (Flyway `V2`)
- `pedido`: `id`, `cliente_id` (FK), `situacao` (texto com `CHECK`), endereço de entrega copiado (`entrega_*`), `subtotal`, `total_descontos`, `frete`, `total` (`numeric(12,2)`), `criado_em`, `versao`.
- `pedido_item`: `pedido_id`, `sku`, `nome_produto`, `descricao_variacao`, `quantidade` (> 0), `preco_unitario`, `total_item`.
- `pedido_desconto`: `pedido_id`, `descricao`, `origem` (`PROMOCAO`/`CUPOM`), `alvo` (`ITENS`/`FRETE`), `valor`.
- `pedido_historico`: `pedido_id`, `de`, `para`, `ocorrido_em`, `responsavel_tipo` (`CLIENTE`/`ADMINISTRATIVO`/`SISTEMA`), `responsavel_usuario_id`, `motivo`.

Itens, descontos e histórico são coleções do agregado (`@ElementCollection` ou entidades filhas com cascade). O snapshot não tem setters.

A validação `total = subtotal - descontos + frete` e `subtotal = soma dos itens` fica na fábrica estática `Pedido.criar(DadosNovoPedido, momento)`. Isso é validação do agregado, não o Factory Method do enunciado, que pertence às promoções.

### D3. Concorrência
O `@Version` em `pedido` (lock otimista) resolve transições simultâneas. Na disputa, a `ObjectOptimisticLockingFailureException` vira `ConflitoException` (409) no adaptador ou no tratador. O lock pessimista foi rejeitado porque as transições são curtas e o conflito é raro.

### D4. Eventos (lado publicador do Observer)
Os records ficam em `pedido.dominio.evento`: `PedidoCriado` (emitido na transição para `AGUARDANDO_PAGAMENTO`, conforme o efeito de T1), `PedidoPago`, `PedidoPagamentoRecusado`, `PedidoEmPreparacao`, `PedidoEnviado`, `PedidoEntregue` e `PedidoCancelado`. Todos implementam `EventoDominio` e carregam `pedidoId`, `clienteId`, `de`, `para` e `ocorridoEm`. O `PedidoCancelado` carrega também o responsável e o motivo.

O serviço de aplicação publica os eventos acumulados pelo agregado só depois de salvar, na mesma transação, via `PublicadorEventos`. Os ouvintes futuros usam `@TransactionalEventListener(AFTER_COMMIT)` quando o efeito não puder ser desfeito. O `de` permite que o estoque decida entre liberar a reserva e repor a baixa, e que o pagamento decida estornar.

Os nomes `PedidoPago` e `PedidoPagamentoRecusado` não conflitam com `PagamentoConfirmado`/`PagamentoRecusado`, que são do módulo de pagamento na etapa 6.

### D5. Responsável
O record `Responsavel(tipo, usuarioId)` é montado na camada web a partir do JWT: perfil `CLIENTE` dá `CLIENTE`; qualquer perfil administrativo dá `ADMINISTRATIVO`. A simulação usa `SISTEMA` com motivo "pagamento simulado".

### D6. API
**Cliente** (`CLIENTE`):
- `GET /api/clientes/me/pedidos`, paginado, do mais recente para o mais antigo.
- `GET /api/clientes/me/pedidos/{id}`.
- `POST /api/clientes/me/pedidos/{id}/cancelamento` com `{motivo}`.
- A busca é sempre por `(id, clienteId)`, e o que não pertence ao cliente dá 404.

**Administração:**
- `GET /api/admin/pedidos?situacao=` e `GET /{id}`: `ADMIN` e `OPERADOR_COMERCIAL`.
- `POST /{id}/preparacao`, `/envio` e `/entrega`: `ADMIN` e `ESTOQUE`.
- `POST /{id}/cancelamento` com `{motivo}`: `ADMIN` e `OPERADOR_COMERCIAL`.
- `POST /{id}/pagamento-simulado` com `{aprovado: boolean}`: `ADMIN`, marcado `@Deprecated` e "TEMPORÁRIO até a etapa 6" no Swagger.

A `TransicaoInvalidaException` estende `ConflitoException` e acrescenta as propriedades `situacaoAtual` e `operacao` ao Problem Details.

### D7. Criação para o checkout
O serviço `CriarPedido.executar(DadosNovoPedido)` cria o pedido em `CRIADO`. O serviço `ConfirmarReservaDoPedido` leva a `AGUARDANDO_PAGAMENTO` e emite `PedidoCriado`. O checkout vai chamar os dois na mesma transação, depois de reservar o estoque. Nesta change não há endpoint HTTP de criação: os testes usam os serviços, e um helper de teste monta snapshots.

### D8. Design Patterns

| Pattern | Papel | Problema do enunciado | Classes participantes | O que fica mais fácil |
|---|---|---|---|---|
| State | Contexto | Ciclo de vida com transições válidas | `Pedido` | Nenhuma operação do pedido tem condicional por estado |
| State | Estado | idem | `EstadoPedido`, `EstadoPedidoBase` | Um estado novo é uma nova classe mais o ajuste das transições que levam a ele |
| State | Estados concretos | idem, e "em quais estados o cancelamento é permitido" | `Criado`, `AguardandoPagamento`, `Pago`, `PagamentoRecusado`, `EmPreparacao`, `Enviado`, `Entregue`, `Cancelado` | Regras de cancelamento ficam em um lugar só |
| Observer | Sujeito/publicador | Vários componentes reagem a acontecimentos do pedido | serviços de aplicação do `pedido` + `PublicadorEventos` | Estoque, pagamento, notificação e auditoria se inscrevem sem alterar o pedido |
| Observer | Eventos | idem | `PedidoCriado`, `PedidoCancelado` e os demais | idem |

Sem pattern próprio: consultas, histórico (registro simples) e a simulação de pagamento.

## Risks / Trade-offs

- [Coleções JPA do agregado com N+1 nas listagens] → A listagem devolve um resumo (id, situação, total, data) sem carregar as coleções. O detalhe usa fetch explícito.
- [Simulação de pagamento esquecida em produção] → `@Deprecated`, documentação "temporário" e tarefa explícita na change `pagamento-idempotencia` para removê-la.
- [Regra de cancelamento do cliente em `PAGO` ser contestada] → É uma decisão de política registrada em `docs/05` (RN-26). Mudar exige ajustar só `Pago.cancelar` e `EmPreparacao.cancelar`.
