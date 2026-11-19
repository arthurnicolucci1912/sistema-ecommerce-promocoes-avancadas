# 12 - Board de tarefas

> **Objetivo:** acompanhar o andamento das changes do roadmap por integrante.
> **Etapas do roadmap relacionadas:** E1 a E9. Ver [08-roadmap.md](08-roadmap.md) e [11-divisao-equipe.md](11-divisao-equipe.md).
> **Status:** implementado (E1–E9): todas as changes do roadmap estão na coluna Feito (arquivadas até 2026-10-23). Atualize este arquivo no mesmo PR em que a tarefa muda de coluna.

**Regras do board**
- Cada integrante move apenas os próprios cartões e implementa e commita o próprio trabalho.
- Uma change está **Feita** quando todas as tasks do OpenSpec estão marcadas, `./mvnw verify` passa, o PR foi revisado e a change foi arquivada (`/opsx:archive`).
- Revisão: **Arthur (dono do projeto) revisa e aprova todos os PRs** antes do merge em `main`. Os PRs do próprio Arthur recebem revisão de um colega, em rodízio (Lucas, Amanda, Mateus), para manter um segundo olhar e a familiaridade da equipe com a base.

## Visão por coluna

| Feito (change, responsável, arquivamento) |
|---|
| `estruturar-documentacao` (Arthur), arquivada em 2026-10-14 |
| `fundacao-projeto` (Arthur), arquivada em 2026-10-14 |
| `pedido-ciclo-vida` (Arthur), arquivada em 2026-10-14 |
| `catalogo-estoque`: implementado por Arthur (atribuição original: Lucas), arquivada em 2026-10-19 |
| `carrinho-compras`: implementado por Arthur em sessão assistida (atribuição original: Lucas), arquivada em 2026-10-23 |
| `checkout` (Arthur), arquivada em 2026-10-23 |
| `promocoes-cupons`: implementado por Arthur em sessão assistida (atribuição original: Amanda), arquivada em 2026-10-23 |
| `pagamento-idempotencia`: implementado por Arthur em sessão assistida (atribuição original: Mateus), arquivada em 2026-10-23 |
| `entrega-frete`: implementado por Arthur em sessão assistida (atribuição original: Mateus), arquivada em 2026-10-23 |
| `eventos-notificacoes-auditoria`: eventos e notificações (atribuição original: Mateus) e auditoria (atribuição original: Amanda), implementados por Arthur em sessão assistida, arquivada em 2026-10-23 |
| `relatorios-gerenciais`: implementado por Arthur em sessão assistida (atribuição original: Lucas), arquivada em 2026-10-23 |

Não há cartões em A fazer, Em andamento ou Em revisão. A reunião da onda 0 (fechar contratos, todos) está registrada nos marcos abaixo.

A ordem sequencial de execução das changes, com as dependências, está em [08-roadmap.md](08-roadmap.md) (seção 7). A etapa 4 do roadmap foi dividida em `pedido-ciclo-vida` e `checkout` (depois de `carrinho-compras`). A `catalogo-estoque` foi verificada e arquivada em 2026-10-19; as demais changes (E3 a E9) foram arquivadas em 2026-10-23.

## Cartões por integrante

### Arthur
| Cartão | Coluna | Depende de | Entregas | Patterns |
|---|---|---|---|---|
| `estruturar-documentacao` | Feito (arquivada em 2026-10-14) | nenhuma | `docs/` 01 a 11 | nenhum |
| `fundacao-projeto` | Feito (arquivada em 2026-10-14); implementada por Arthur, conforme planejado | nenhuma | Esqueleto, segurança JWT, cliente e endereços, contratos da onda 0, `10-execucao.md` | Contratos de Strategy, Composite, Chain of Responsibility, Adapter e Observer (D13) |
| `pedido-ciclo-vida` | Feito (arquivada em 2026-10-14); implementada por Arthur, conforme planejado | `fundacao-projeto` | Pedido com snapshot, ciclo de vida de 8 estados, histórico, cancelamento por perfil, consultas, API do cliente e administrativa, eventos do pedido, pagamento simulado (temporário), diagrama de estados | State, Observer (publicador) |
| `checkout` | Feito (arquivada em 2026-10-23); implementada por Arthur, conforme planejado | `catalogo-estoque` e `carrinho-compras` (e `pedido-ciclo-vida`, já pronta) | Checkout com revalidação, reserva de estoque tudo-ou-nada, uso de `CriarPedido` e `ConfirmarReservaDoPedido`, diagrama de sequência do checkout | Facade |

### Lucas Gomes
| Cartão | Coluna | Depende de | Entregas | Patterns |
|---|---|---|---|---|
| `catalogo-estoque` | Feito (arquivada em 2026-10-19): **implementado por Arthur (atribuição original: Lucas)** | `fundacao-projeto` | Produtos, variações, categorias em árvore, combos, saldo e reserva de estoque, concorrência na última unidade, ouvintes dos eventos do pedido e do catálogo | Composite (combos), Observer (ouvintes do estoque) |
| `carrinho-compras` | Feito (arquivada em 2026-10-23): **implementado por Arthur em sessão assistida (atribuição original: Lucas)** | `catalogo-estoque` | Carrinho, recálculo, primeiras etapas do pipeline de preço | Chain of Responsibility |
| `relatorios-gerenciais` | Feito (arquivada em 2026-10-23): **implementado por Arthur (sessão assistida) · atribuição original: Lucas** | `checkout`, `promocoes-cupons` | Vendas, ticket médio, mais vendidos, desconto por campanha (três `GET` somente leitura, sem migração) | nenhum (sem ponto de variação) |

### Amanda Duara
| Cartão | Coluna | Depende de | Entregas | Patterns |
|---|---|---|---|---|
| `promocoes-cupons` | Feito (arquivada em 2026-10-23): **implementado por Arthur (sessão assistida) · atribuição original: Amanda** | contratos da onda 0. Integra com `carrinho-compras` | Tipos de promoção (no mínimo 4), condições, política de conflito, cupons, diagrama de sequência de promoções | Strategy, Composite (Specification), Factory Method |
| Auditoria (parte de `eventos-notificacoes-auditoria`) | Feito (arquivada em 2026-10-23): **implementada por Arthur (sessão assistida) · atribuição original: Amanda** | eventos de domínio (Mateus) | Registro de operações administrativas com usuário e momento | Observer (como ouvinte) |

### Mateus Rocha
| Cartão | Coluna | Depende de | Entregas | Patterns |
|---|---|---|---|---|
| `pagamento-idempotencia` | Feito (arquivada em 2026-10-23): **implementado por Arthur (sessão assistida) · atribuição original: Mateus** | contratos da onda 0. Integra com `checkout` e `pedido-ciclo-vida`; substitui o atalho temporário | Gateways simulados, confirmação idempotente, pagamento recusado, diagrama de sequência de pagamento | Adapter |
| `entrega-frete` | Feito (arquivada em 2026-10-23): **implementado por Arthur (sessão assistida) · atribuição original: Mateus** | contratos da onda 0 | Provedores de frete simulados, cotação, frete grátis via promoção | Adapter |
| Eventos e notificações (parte de `eventos-notificacoes-auditoria`) | Feito (arquivada em 2026-10-23): **implementados por Arthur (sessão assistida) · atribuição original: Mateus** | `pagamento-idempotencia` | Publicação de eventos de domínio, canais de notificação simulados | Observer |

### Quem implementou (situação real)

A atribuição original (colunas acima e [11-divisao-equipe.md](11-divisao-equipe.md)) não foi alterada. Todas as changes foram implementadas por Arthur, as de Lucas, Amanda e Mateus em sessão assistida (Claude Code), e estão arquivadas:

| Change | Atribuída a | Implementada por | Arquivada em |
|---|---|---|---|
| `fundacao-projeto` | Arthur | Arthur (conforme planejado) | 2026-10-14 |
| `pedido-ciclo-vida` | Arthur | Arthur (conforme planejado) | 2026-10-14 |
| `catalogo-estoque` | Lucas | Arthur (atribuição original: Lucas) | 2026-10-19 |
| `carrinho-compras` | Lucas | Arthur (sessão assistida; atribuição original: Lucas) | 2026-10-23 |
| `checkout` | Arthur | Arthur (sessão assistida; conforme planejado) | 2026-10-23 |
| `promocoes-cupons` | Amanda | Arthur (sessão assistida; atribuição original: Amanda) | 2026-10-23 |
| `pagamento-idempotencia` | Mateus | Arthur (sessão assistida; atribuição original: Mateus) | 2026-10-23 |
| `entrega-frete` | Mateus | Arthur (sessão assistida; atribuição original: Mateus) | 2026-10-23 |
| `eventos-notificacoes-auditoria` (eventos e notificações) | Mateus | Arthur (sessão assistida; atribuição original: Mateus) | 2026-10-23 |
| `eventos-notificacoes-auditoria` (auditoria) | Amanda | Arthur (sessão assistida; atribuição original: Amanda) | 2026-10-23 |
| `relatorios-gerenciais` | Lucas | Arthur (sessão assistida; atribuição original: Lucas) | 2026-10-23 |

A autoria dos commits é registrada pelo autor (ver o histórico do Git).

**Evidência final (2026-10-23):** `./mvnw -o clean verify` com BUILD SUCCESS, **649 testes, zero falhas/erros/ignorados** e JAR gerado; `openspec validate --specs --strict` aprovado nas 16 specs principais; todas as changes do roadmap (E1–E9) movidas para `openspec/changes/archive/`. O histórico de evidências por etapa está abaixo.

### Evidências por etapa

Evidência da E3 em 2026-10-21: `./mvnw -o verify` aprovado (480 testes, zero falhas/erros/ignorados), JAR gerado e `openspec validate carrinho-compras --strict` aprovado. O checklist da change está concluído; o autor aprovou a entrega local do carrinho em 2026-10-22. A change foi arquivada em 2026-10-23.

Evidência da E4b em 2026-10-22: `./mvnw -o verify` aprovado (507 testes, zero falhas/erros/ignorados), incluindo 27 testes novos do checkout e JAR gerado; `openspec validate checkout --strict` aprovado. Compra com variação/kit, nova confirmação de preço, snapshot, reserva, cancelamento/recusa/aprovação simulados, rollback após flush e concorrência verificados com PostgreSQL/Testcontainers. A change `checkout` foi arquivada em 2026-10-23.


Evidência E5 em 2026-10-22: `./mvnw -o verify` — BUILD SUCCESS, 540 testes, zero falhas/erros/ignorados; JAR gerado. `openspec validate promocoes-cupons --strict` e `git diff --check` aprovados. Foram verificados cadastro/autorizações, extensão por tipo/condição/política/benefício/handler, primeira compra por histórico, recuperação de cupom inválido, snapshot, limites concorrentes, rollback após consumo/flush, devolução idempotente, migração V4→V5 e carga dev idempotente. Os 27 itens da change foram concluídos. A change `promocoes-cupons` foi arquivada em 2026-10-23.

Evidência E9 em 2026-10-23: `./mvnw -o clean verify` — BUILD SUCCESS, 649 testes, zero falhas/erros/ignorados; JAR gerado. `openspec validate relatorios-gerenciais --strict` e `git diff --check` aprovados. Foram verificados, com PostgreSQL e dados de datas e descontos controlados (T-RL-01), o total de vendas, o ticket médio, os descontos por campanha, os produtos mais vendidos, os cancelamentos, a exclusão de pedido pago e depois cancelado, o período vazio, o 400 de período e limite inválidos e o 401/403 por perfil. A change `relatorios-gerenciais` foi arquivada em 2026-10-23.

Evidência E6 em 2026-10-22: `./mvnw clean verify` — BUILD SUCCESS, 562 testes, zero falhas/erros/ignorados; JAR gerado. Os 19 itens de `pagamento-idempotencia` foram concluídos. Foram implementados cobrança por três meios, confirmação autenticada/idempotente, rollback e concorrência com cancelamento, estorno recuperável, migração V5→V6 e remoção do atalho temporário. Atribuição original de Mateus preservada; implementação realizada em sessão assistida a pedido do autor. Change arquivada em 2026-10-23. Expiração automática permanece pendente de prazo e rotina próprios.

Evidência E7 em 2026-10-22: `./mvnw -o clean verify` — BUILD SUCCESS, 584 testes, zero falhas/erros/ignorados e JAR gerado. Os 21 itens foram concluídos; cotação por região/modalidade, frete grátis, seleção/reconfirmação, snapshot V7, migração de legados e ciclo completo até entrega foram verificados. Atribuição original de Mateus preservada; implementação executada em sessão assistida a pedido do autor. Change `entrega-frete` arquivada em 2026-10-23.

E8 — grupo 3 (2026-10-23): consulta GET `/api/admin/auditoria` com filtros, paginação no banco, ordenação determinística e acesso exclusivo ADMIN implementada; roteiro de compra notificada/falha de canal e consulta de preço/campanha/cancelamento exercitado com JWT e PostgreSQL. Progresso nesse momento: 15/17 tarefas. Atribuições originais de Mateus e Amanda preservadas; implementação em sessão assistida a pedido do autor. O grupo 4 foi concluído depois e a change `eventos-notificacoes-auditoria` foi arquivada em 2026-10-23.

## Marcos

| Marco | Critério | Situação |
|---|---|---|
| Onda 0 fechada | `fundacao-projeto` arquivada e contratos revisados por todos | Parcial: `fundacao-projeto` arquivada em 2026-10-14; a reunião de revisão conjunta dos contratos com todos os integrantes não foi registrada |
| Primeira compra ponta a ponta | Carrinho → checkout → pagamento aprovado → pedido `PAGO` | Verificada (E6, arquivada em 2026-10-23): cobrança por gateway simulado e confirmação idempotente até pedido `PAGO`, no lugar do atalho administrativo da E4b |
| Demo da defesa | Compra completa, múltiplas promoções e um cenário de erro ([08-roadmap.md](08-roadmap.md)) | Roteiro pronto em [10-execucao.md](10-execucao.md); apresentação da defesa pendente |
