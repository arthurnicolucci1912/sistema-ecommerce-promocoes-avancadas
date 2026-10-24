# Histórico de commits e responsabilidades

Este documento relaciona cada commit do repositório à change do OpenSpec e aos grupos de tarefas que ele contém. Para cada um, mostra o responsável **planejado**, segundo [docs/11-divisao-equipe.md](docs/11-divisao-equipe.md), e quem **implementou de fato**.

**Resumo da situação real:** até agora, todo o desenvolvimento foi feito por **Arthur**, com auxílio do assistente de IA Claude Code. Lucas Gomes, Amanda Duara e Mateus Rocha não implementaram nenhuma parte do código registrada aqui; as changes atribuídas a eles foram implementadas por Arthur, preservando a atribuição original em `docs/11`. Os commits 5, 6 e 7 chegaram a ser registrados com a identidade de Lucas Gomes. Em 2026-10-15 a autoria foi corrigida para Arthur por rebase; os hashes atuais são `429ca70`, `6d3d278` e `b886317`.

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
| 10 | `8bd996d` | 2026-10-19 | Arthur | `catalogo-estoque`: grupo 5 (API pública e administrativa do catálogo e do estoque, regras de rota por perfil, regras ArchUnit entre `estoque`, `catalogo` e `pedido`) | Lucas | Arthur |
| 11 | `a5a6f87` | 2026-10-19 | Arthur | `catalogo-estoque`: grupo 6 (carga de demonstração de catálogo, combo e estoque no perfil `dev`; pedido demo com reserva de estoque) | Lucas | Arthur |
| 12 | `e0a95a5` | 2026-10-19 | Arthur | `catalogo-estoque`: grupo 7 (documentação de catálogo, estoque, Composite e Observer; registro da divisão real em `docs/11` e no board; ajustes de Javadoc e `openspec/config.yaml`) | Lucas | Arthur |
| 13 | `20ccb0b` | 2026-10-19 | Arthur | `catalogo-estoque`: grupo 8 (verificação final e roteiro manual) e arquivamento da change; specs principais `catalogo` e `estoque`; board atualizado | Lucas | Arthur |
| 14 | `092c2cb` | 2026-10-23 | Arthur | `carrinho-compras`, `checkout`, `promocoes-cupons`, `pagamento-idempotencia`, `entrega-frete`, `eventos-notificacoes-auditoria` e `relatorios-gerenciais`: implementação (código, migrações V4 a V8, testes) e documentação com status final; 649 testes aprovados | Lucas (carrinho, relatórios), Arthur (checkout), Amanda (promoções, auditoria), Mateus (pagamento, frete, eventos) | Arthur |
| 15 | `41873d5` | 2026-10-23 | Arthur | Planejamento OpenSpec (proposal, specs, design, tasks) e arquivamento das 7 changes acima; specs principais atualizadas | Os mesmos do commit 14 | Arthur |
| 16 | `3266942` | 2026-10-23 | Arthur | Registro dos commits 14 e 15 neste `commits.md` | Arthur | Arthur |
| 17 | `41afe84` | 2026-10-23 | Arthur | `docs/10-execucao.md`: ordem do roteiro da demo validada do zero; registro do commit 16 | Arthur | Arthur |
| 18 | `4afb0a2` | 2026-10-24 | Arthur | `docs/08-roadmap.md` seção 6: roteiro de alteração de requisito com as classes reais; registro do commit 17 | Arthur | Arthur |

## Divergências entre o planejado e o realizado

| Change | Responsável planejado | Situação |
|---|---|---|
| `estruturar-documentacao`, `fundacao-projeto`, `pedido-ciclo-vida` | Arthur | Feitas por Arthur, conforme o planejado |
| `catalogo-estoque` (grupos 1 a 8, arquivada) | Lucas | Feita por Arthur (commits 5 a 13, autoria registrada corretamente) |
| `carrinho-compras` | Lucas | Feita por Arthur (sessão assistida); commits 14 e 15 |
| `relatorios-gerenciais` | Lucas | Feita por Arthur (sessão assistida); commits 14 e 15 |
| `checkout` | Arthur | Feita por Arthur, conforme o planejado; commits 14 e 15 |
| `promocoes-cupons`, auditoria | Amanda | Feitas por Arthur (sessão assistida); commits 14 e 15 |
| `pagamento-idempotencia`, `entrega-frete`, eventos e notificações | Mateus | Feitas por Arthur (sessão assistida); commits 14 e 15 |
