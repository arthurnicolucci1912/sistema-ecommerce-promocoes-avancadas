# Tasks

## 1. Contratos comerciais e prévia revalidada

- [x] 1.1 Expor descrição comercial de variação pelo contrato polimórfico de item vendável e por `ItemVendavelInfo`, preservando consumidores atuais; verificar descrição de atributos em ordem estável, combo sem descrição e regressão de catálogo/carrinho, sem condicionais por tipo no consumidor.
- [x] 1.2 Implementar `RevalidarCheckout`, `CompraRevalidada`, `PreviaCheckout` e `ConfirmacaoCheckout` com identidade autenticada, endereço ativo próprio, versão e seleção corrente; verificar testes de carrinho vazio, endereço ausente/inativo/alheio, SKU ausente/inativo e prévia sem escrita de carrinho, pedido ou estoque.
- [x] 1.3 Usar `PipelinePreco` e agregar componentes com aritmética verificada antes da conversão para reserva; verificar preço próprio de kit aninhado, componentes compartilhados, saldo ausente, estouro de produto/soma e novo tipo vendável de teste sem alteração no checkout. Demonstrar execução de uma etapa de preço adicional de teste sem modificar a revalidação.
- [x] 1.4 Documentar contratos e prévia em docs/02-modelo-dominio.md, docs/03-arquitetura.md e rastreabilidade T-CH em docs/09-testes.md; verificar correspondência de campos e comportamento com os testes entregues e manter explícitos os limites das etapas 5–7.

## 2. Confirmação dos dados comerciais

- [x] 2.1 Comparar confirmação com dados recalculados por linha, versão, descontos, frete, totais e endereço, normalizando ordem e escala monetária; verificar preço alterado, mudanças compensadas no total, quantidade/seleção alterada, endereço alterado, equivalência de ordem/escala e nova confirmação bem-sucedida.
- [x] 2.2 Implementar conflito com `divergencias` e `previaAtualizada` pelo mecanismo existente de `ConflitoException`; verificar conteúdo estruturado do erro e ausência de pedido/reserva/alteração de carrinho após rejeição, inclusive confirmação manipulada que nunca define o preço da compra.
- [x] 2.3 Atualizar RN-22 em docs/05-regras-negocio.md, UC-07 A1 em docs/07-casos-de-uso.md e T-CH em docs/09-testes.md; verificar que todos exigem nova confirmação e discriminam os cenários implementados dos futuros.

## 3. Fachada transacional e consumo do carrinho

- [x] 3.1 Implementar `CheckoutFacade` coordenando revalidação, comparação, `CriarPedido`, `ReservarEstoque`, `ConfirmarReservaDoPedido` e limpeza do carrinho na mesma transação; verificar integração PostgreSQL de compra AGUARDANDO_PAGAMENTO com snapshot comercial/endereço e reserva correta, além de imutabilidade após mudanças posteriores no catálogo/endereço.
- [x] 3.2 Adicionar esvaziamento com incremento de revisão ao agregado do carrinho e persistência por seu adaptador existente; verificar unitários de limpeza e integração de rollback de pedido, histórico, reservas, saldos e seleção por insuficiência parcial, falha na confirmação do pedido e falha na persistência da limpeza.
- [x] 3.3 Verificar concorrência com threads e transações independentes em PostgreSQL: dois clientes pela última unidade, duas conclusões da mesma versão e conclusão versus alteração do carrinho; exigir no máximo um consumo da seleção, nenhum estoque negativo, 409 da perdedora e rollback completo sem perda silenciosa de alteração.
- [x] 3.4 Exercitar cancelamento, aprovação e recusa simulados em pedidos originados pelo checkout; verificar liberação/baixa por ouvintes existentes, repetição sem efeito adicional e novo ouvinte de teste recebendo o evento sem alterações no módulo de pedidos.
- [x] 3.5 Atualizar docs/03-arquitetura.md, docs/04-design-patterns.md, RN-18 a RN-23 em docs/05-regras-negocio.md e T-CH em docs/09-testes.md; incluir Javadoc dos participantes com Facade e seus papéis, conferir classes reais e executar ArchUnit preservando as fronteiras entre módulos.

## 4. API, autorização e uso reproduzível

- [x] 4.1 Expor prévia e conclusão em `/api/clientes/me/checkout` com DTOs validados, 200/201, Location e `PedidoResponse`; verificar MockMvc de fluxo completo, confirmação desatualizada com Problem Details, carrinho vazio, dados ausentes/inválidos e quantidades fracionárias.
- [x] 4.2 Exigir CLIENTE na rota e na fachada sem clienteId fornecido pelo chamador; verificar 401 sem token, 403 para todos os perfis sem CLIENTE, isolamento de dois clientes e 404 para endereço alheio/inativo sem vazamento nem alteração de dados.
- [x] 4.3 Atualizar UC-07 e diagrama de sequência em docs/07-casos-de-uso.md, exemplos completos em docs/10-execucao.md e rastreabilidade em docs/09-testes.md; verificar os exemplos contra API/Swagger e schemas OpenAPI de prévia, confirmação, pedido e erros, documentando frete zero e pagamento simulado separado.

## 5. Validação integrada e entrega

- [x] 5.1 Executar `./mvnw -o verify` com PostgreSQL/Testcontainers e fluxo autenticado variação + kit → prévia → divergência de preço → nova confirmação → pedido reservado → cancelamento; verificar regressão completa e demonstrar rollback/concorrência com evidências reais.
- [x] 5.2 Atualizar docs/08-roadmap.md e docs/12-board.md com situação e evidências da E4b, preservando atribuições e revisão conjunta pendente da onda 0; verificar `openspec validate checkout --strict` e entregar para revisão do autor, sem git add, commit ou arquivamento automático.


## Evidências da implementação

- 2026-10-22: `./mvnw -o verify` — BUILD SUCCESS, 507 testes, zero falhas, erros ou ignorados; JAR em `target/ecommerce-0.0.1-SNAPSHOT.jar`.
- 27 testes novos: `RevalidarCheckoutTests` (8), `ConfirmacaoCheckoutTests` (3), `CheckoutIntegracaoTests` (16). Integração HTTP com PostgreSQL/Testcontainers, tokens reais e concorrência em transações independentes.
- Cobertura: kits aninhados/componentes compartilhados, nova confirmação de preços, identidade/endereço, snapshot, reserva, limpeza, rollback após flush e disputa da última unidade/mesmo carrinho/CRUD nos dois sentidos; cancelamento, aprovação e recusa simulados e novo ouvinte de PedidoCriado.
- `openspec validate checkout --strict` aprovado; `git diff --check` sem problemas. Regressão de catálogo, carrinho, pedido, estoque e ArchUnit aprovada.
- Documentação 02–05 e 07–10, roadmap e board atualizados; checkout entregue para revisão do autor, sem git add, commit, PR ou arquivamento automático. Revisão conjunta da onda 0 continua pendente; gateway, promoções/cupons e frete seguem nas E5–E7.
