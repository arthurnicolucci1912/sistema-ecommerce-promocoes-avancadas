# 12 - Board de tarefas

> **Objetivo:** acompanhar o andamento das changes do roadmap por integrante.
> **Etapas do roadmap relacionadas:** E1 a E9. Ver [08-roadmap.md](08-roadmap.md) e [11-divisao-equipe.md](11-divisao-equipe.md).
> **Status:** em uso. Atualize este arquivo no mesmo PR em que a tarefa muda de coluna.

**Regras do board**
- Cada integrante move apenas os próprios cartões e implementa e commita o próprio trabalho.
- Uma change está **Feita** quando todas as tasks do OpenSpec estão marcadas, `./mvnw verify` passa, o PR foi revisado e a change foi arquivada (`/opsx:archive`).
- Revisão: **Arthur (dono do projeto) revisa e aprova todos os PRs** antes do merge em `main`. Os PRs do próprio Arthur recebem revisão de um colega, em rodízio (Lucas, Amanda, Mateus), para manter um segundo olhar e a familiaridade da equipe com a base.

## Visão por coluna

| A fazer | Em andamento | Em revisão | Feito |
|---|---|---|---|
| `carrinho-compras` (Lucas) | | | `fundacao-projeto` (Arthur), arquivada em 2026-10-14; `catalogo-estoque`: implementado por Arthur (atribuição original: Lucas), arquivada em 2026-10-19 |
| `checkout` (Arthur) | | | `estruturar-documentacao` (Arthur), arquivada em 2026-10-14 |
| `promocoes-cupons` (Amanda) | | | `pedido-ciclo-vida` (Arthur), arquivada em 2026-10-14 |
| `pagamento-idempotencia` (Mateus) | | | |
| `entrega-frete` (Mateus) | | | |
| `eventos-notificacoes-auditoria`: eventos e notificações (Mateus) | | | |
| `eventos-notificacoes-auditoria`: auditoria (Amanda) | | | |
| `relatorios-gerenciais` (Lucas) | | | |
| Reunião da onda 0: fechar contratos (todos) | | | |

A ordem sequencial de execução das changes, com as dependências, está em [08-roadmap.md](08-roadmap.md) (seção 7). A etapa 4 do roadmap foi dividida em `pedido-ciclo-vida` (arquivada) e `checkout` (depois de `carrinho-compras`). A `catalogo-estoque` foi verificada e arquivada em 2026-10-19.

## Cartões por integrante

### Arthur
| Cartão | Coluna | Depende de | Entregas | Patterns |
|---|---|---|---|---|
| `estruturar-documentacao` | Feito (arquivada em 2026-10-14) | nenhuma | `docs/` 01 a 11 | nenhum |
| `fundacao-projeto` | Feito (arquivada em 2026-10-14); implementada por Arthur, conforme planejado | nenhuma | Esqueleto, segurança JWT, cliente e endereços, contratos da onda 0, `10-execucao.md` | Contratos de Strategy, Composite, Chain of Responsibility, Adapter e Observer (D13) |
| `pedido-ciclo-vida` | Feito (arquivada em 2026-10-14); implementada por Arthur, conforme planejado | `fundacao-projeto` | Pedido com snapshot, ciclo de vida de 8 estados, histórico, cancelamento por perfil, consultas, API do cliente e administrativa, eventos do pedido, pagamento simulado (temporário), diagrama de estados | State, Observer (publicador) |
| `checkout` | A fazer | `catalogo-estoque` e `carrinho-compras` (e `pedido-ciclo-vida`, já pronta) | Checkout com revalidação, reserva de estoque tudo-ou-nada, uso de `CriarPedido` e `ConfirmarReservaDoPedido`, diagrama de sequência do checkout | Facade |

### Lucas Gomes
| Cartão | Coluna | Depende de | Entregas | Patterns |
|---|---|---|---|---|
| `catalogo-estoque` | Feito (arquivada em 2026-10-19): **implementado por Arthur (atribuição original: Lucas)** | `fundacao-projeto` | Produtos, variações, categorias em árvore, combos, saldo e reserva de estoque, concorrência na última unidade, ouvintes dos eventos do pedido e do catálogo | Composite (combos), Observer (ouvintes do estoque) |
| `carrinho-compras` | A fazer | `catalogo-estoque` | Carrinho, recálculo, primeiras etapas do pipeline de preço | Chain of Responsibility |
| `relatorios-gerenciais` | A fazer | `checkout`, `promocoes-cupons` | Vendas, ticket médio, mais vendidos, desconto por campanha | nenhum |

### Amanda Duara
| Cartão | Coluna | Depende de | Entregas | Patterns |
|---|---|---|---|---|
| `promocoes-cupons` | A fazer | contratos da onda 0. Integra com `carrinho-compras` | Tipos de promoção (no mínimo 4), condições, política de conflito, cupons, diagrama de sequência de promoções | Strategy, Composite (Specification), Factory Method |
| Auditoria (parte de `eventos-notificacoes-auditoria`) | A fazer | eventos de domínio (Mateus) | Registro de operações administrativas com usuário e momento | Observer (como ouvinte) |

### Mateus Rocha
| Cartão | Coluna | Depende de | Entregas | Patterns |
|---|---|---|---|---|
| `pagamento-idempotencia` | A fazer | contratos da onda 0. Integra com `checkout` e `pedido-ciclo-vida`; remove o pagamento simulado | Gateways simulados, confirmação idempotente, pagamento recusado, diagrama de sequência de pagamento | Adapter |
| `entrega-frete` | A fazer | contratos da onda 0 | Provedores de frete simulados, cotação, frete grátis via promoção | Adapter |
| Eventos e notificações (parte de `eventos-notificacoes-auditoria`) | A fazer | `pagamento-idempotencia` | Publicação de eventos de domínio, canais de notificação simulados | Observer |

### Quem implementou (situação real)

A atribuição original (colunas acima e [11-divisao-equipe.md](11-divisao-equipe.md)) não foi alterada. Até agora todas as changes foram implementadas por Arthur:

| Change | Atribuída a | Implementada por |
|---|---|---|
| `fundacao-projeto` | Arthur | Arthur (conforme planejado) |
| `pedido-ciclo-vida` | Arthur | Arthur (conforme planejado) |
| `catalogo-estoque` | Lucas | Arthur (atribuição original: Lucas); arquivada em 2026-10-19 |

As demais changes não foram iniciadas.

## Marcos

| Marco | Critério | Situação |
|---|---|---|
| Onda 0 fechada | `fundacao-projeto` arquivada e contratos revisados por todos | Pendente (`fundacao-projeto` arquivada em 2026-10-14; falta a revisão conjunta dos contratos) |
| Primeira compra ponta a ponta | Carrinho → checkout → pagamento aprovado → pedido `PAGO` | Pendente |
| Demo da defesa | Compra completa, múltiplas promoções e um cenário de erro ([08-roadmap.md](08-roadmap.md)) | Pendente |
