# Tasks

> Não fazer commits. Ao final, as alterações ficam no working tree para revisão e commit do autor.

## 1. Persistência

- [x] 1.1 Criar a migração Flyway `V2` com `pedido`, `pedido_item`, `pedido_desconto` e `pedido_historico` (D2), com `CHECK` de situação, origem, alvo e tipo de responsável, quantidade > 0 e coluna `versao`. Verificar: teste de integração confirma as tabelas e as restrições; `ddl-auto=validate` passa.

## 2. Domínio: State

- [x] 2.1 Implementar `EstadoPedido`, `EstadoPedidoBase`, os 8 estados concretos, `SituacaoPedido` e `TransicaoInvalidaException` (D1), com Javadoc de pattern e papel (State) e link para `docs/04-design-patterns.md` §3.4. Verificar: teste unitário parametrizado cobre os 56 pares de estados × operações. As 11 transições válidas levam ao estado certo e as demais lançam `TransicaoInvalidaException` com estado e operação.
- [x] 2.2 Implementar o agregado `Pedido`, com a fábrica `criar` validando o snapshot (D2), a delegação ao estado, o histórico e o acúmulo de eventos. Também `Responsavel` e a regra de cancelamento por perfil (D5). Verificar: testes unitários de "Totais inconsistentes", de cancelamento de `PAGO` pelo cliente (rejeitado) e pelo administrador (aceito), de motivo obrigatório, de que uma transição rejeitada não gera histórico nem evento, e de extensibilidade: um estado de teste novo implementando `EstadoPedido` funciona sem alterar `Pedido`.
- [x] 2.3 Criar os eventos de `pedido.dominio.evento` (D4) com Javadoc de Observer. Verificar: cada transição válida do teste 2.1 produz o evento esperado, com `de` e `para`.

## 3. Aplicação

- [x] 3.1 Implementar `CriarPedido` e `ConfirmarReservaDoPedido` (D7) com publicação dos eventos depois de salvar. Verificar: teste de integração de "Pedido criado em AGUARDANDO_PAGAMENTO" (snapshot igual e `PedidoCriado` publicado), "Preço alterado depois da compra" e "Endereço alterado depois da compra".
- [x] 3.2 Implementar os serviços de transição (preparar, enviar, entregar, cancelar, aprovar/recusar pagamento) com lock otimista convertido em 409 (D3). Verificar: testes de integração do "Fluxo completo", do histórico em ordem cronológica e de "Alterações concorrentes" (duas transações a partir do mesmo estado: uma aplicada, a outra 409).
- [x] 3.3 Verificar o Observer: teste de integração com um `@EventListener` de teste, registrado só no teste e sem alterar o módulo `pedido`, que recebe `PedidoCancelado` com `de = PAGO` ("Novo interessado no cancelamento") e não recebe nada quando a transição é rejeitada.

## 4. API

- [x] 4.1 Implementar os endpoints do cliente (D6), com busca por `(id, clienteId)`, listagem paginada e cancelamento com motivo. Verificar: testes de "Listar os próprios pedidos", "Pedido de outro cliente", "Cliente cancela pedido aguardando pagamento", "Cliente tenta cancelar pedido pago" e "Cancelamento sem motivo".
- [x] 4.2 Implementar os endpoints administrativos e as regras de rota por perfil (D6), com `situacaoAtual` e `operacao` no Problem Details de 409. Verificar: testes de "Filtrar por estado", "Estoque avança o andamento", "Perfil sem permissão", "Cliente na área administrativa", "Cancelar pedido enviado", "Pular etapa", "Estados finais" e "Administrador cancela pedido pago".
- [x] 4.3 Implementar `POST /api/admin/pedidos/{id}/pagamento-simulado` (temporário, `@Deprecated`, descrito no Swagger). Verificar: testes de "Aprovação simulada", "Recusa simulada" e "Pagamento recusado não reabre o pedido".

- [x] 4.4 Estender os usuários de demonstração (perfil `dev`, idempotente) com um pedido de exemplo do `cliente@demo.local` em `AGUARDANDO_PAGAMENTO`, criado pelos serviços `CriarPedido` e `ConfirmarReservaDoPedido`, até existir o checkout. Verificar: teste no perfil `dev` mostra o pedido criado uma única vez; fora do `dev`, nenhum pedido é criado.

## 5. Documentação

- [x] 5.1 Atualizar `docs/04-design-patterns.md` §3.4 (State com as classes reais, status implementado) e §3.6 (Observer: eventos do pedido publicados), `docs/06-ciclo-vida-pedido.md` (nomes reais de classes, regra de cancelamento por perfil), `docs/05-regras-negocio.md` (RN-26 com a política decidida), `docs/02-modelo-dominio.md` (tabelas do pedido), `docs/03-arquitetura.md` (`EstadoPedido` implementado, eventos do pedido), `docs/09-testes.md` (transição inválida coberta) e `docs/12-board.md`. Verificar: nomes citados existem no código e os links resolvem.
- [x] 5.2 Atualizar `docs/08-roadmap.md` e `docs/12-board.md` com a divisão da etapa 4 em `pedido-ciclo-vida` (feita) e `checkout` (depois de `carrinho-compras`). Verificar: o roadmap e o board mostram a mesma ordem.

## 6. Verificação final

- [x] 6.1 Rodar `./mvnw verify` (todos os testes, inclusive ArchUnit) e fazer o roteiro manual no perfil `dev`. Usar o pedido de demonstração (4.4): simular a aprovação, avançar até `ENVIADO` com `estoque@demo.local`, tentar cancelar (409) e consultar o histórico como `cliente@demo.local`. Verificar: nenhum commit criado.
