# Histórico de commits e responsabilidades

Este documento relaciona cada commit do repositório à change do OpenSpec e aos grupos de tarefas que ele contém. Para cada um, mostra o responsável **planejado**, segundo [docs/11-divisao-equipe.md](docs/11-divisao-equipe.md), e quem **implementou de fato**.

**Resumo da situação real:** até agora, todo o desenvolvimento foi feito por **Arthur**, com auxílio do assistente de IA Claude Code. Lucas Gomes, Amanda Duara e Mateus Rocha ainda não implementaram nenhuma parte do código. Os commits 5, 6 e 7 chegaram a ser registrados com a identidade de Lucas Gomes. Em 2026-10-15 a autoria foi corrigida para Arthur por rebase; os hashes atuais são `429ca70`, `6d3d278` e `b886317`.

## Commits

| # | Commit | Data | Autor registrado no Git | Change / grupos de tarefas | Responsável planejado (docs/11) | Implementado de fato por |
|---|---|---|---|---|---|---|
| 1 | `64b5f23` | 2026-10-14 | Arthur | `estruturar-documentacao`: grupos 1 a 6 (documentação `docs/` 01 a 11) e configuração do OpenSpec e dos agentes | Arthur | Arthur |
| 2 | `9433170` | 2026-10-14 | Arthur | Regras de Design Patterns no OpenSpec (`openspec/config.yaml`, `CLAUDE.md`) e planejamento da change `fundacao-projeto` | Arthur | Arthur |
| 3 | `00e25cc` | 2026-10-14 | Arthur | `fundacao-projeto`: grupo 1 (esqueleto Spring Boot, Docker Compose, perfis) | Arthur | Arthur |
| 4 | `9744d10` | 2026-10-14 | Arthur | `fundacao-projeto`: grupos 2 a 8 (migração V1, erros padronizados, Swagger, `Dinheiro`, autenticação JWT, cadastro de cliente e endereços, usuários de demonstração, contratos da onda 0, ArchUnit, documentação). `pedido-ciclo-vida`: grupos 1 a 6 (State, eventos, API, documentação) | Arthur | Arthur |
| 5 | `429ca70` | 2026-10-15 | Arthur | Arquivamento da change `pedido-ciclo-vida` e planejamento da change `catalogo-estoque` (proposal, specs, design, tasks) | Arthur (`pedido-ciclo-vida`) e Lucas (`catalogo-estoque`) | Arthur |
| 6 | `6d3d278` | 2026-10-15 | Arthur | `catalogo-estoque`: grupo 1 (migração V3) | Lucas | Arthur |
| 7 | `b886317` | 2026-10-15 | Arthur | `catalogo-estoque`: grupo 2 (Composite de item vendável, categorias, evento `VariacaoCadastrada`) | Lucas | Arthur |
| 8 | `0644210` | 2026-10-15 | Arthur | `catalogo-estoque`: grupo 3 (saldo e reserva atômica, movimentação idempotente, ouvintes de pedido e catálogo, teste de concorrência) e criação deste `commits.md` | Lucas | Arthur |
| 9 | `ed0e394` | 2026-10-19 | Arthur | `catalogo-estoque`: grupo 4 (casos de uso de administração de categorias, produtos e combos; `ConsultarItensVendaveis`; consulta pública com filtros e disponibilidade) | Lucas | Arthur |

## Divergências entre o planejado e o realizado

| Change | Responsável planejado | Situação |
|---|---|---|
| `estruturar-documentacao`, `fundacao-projeto`, `pedido-ciclo-vida` | Arthur | Feitas por Arthur, conforme o planejado |
| `catalogo-estoque` (grupos 1 a 4) | Lucas | Feitos por Arthur (commits 5 a 9, autoria registrada corretamente) |
| `catalogo-estoque` (grupos 5 a 8), `carrinho-compras`, `relatorios-gerenciais` | Lucas | Não iniciados |
| `checkout` | Arthur | Não iniciado (depende de `carrinho-compras`) |
| `promocoes-cupons`, auditoria | Amanda | Não iniciados |
| `pagamento-idempotencia`, `entrega-frete`, eventos e notificações | Mateus | Não iniciados |
