# Tasks

> Não fazer commits. Ao final, as alterações ficam no working tree para revisão e commit do autor. Esta change não implementa nenhum Design Pattern (ver design.md, decisão "Design Patterns"); por isso não há tarefa de extensibilidade de pattern.

## 1. Domínio e consultas

- [x] 1.1 Criar o módulo `relatorio` com `PeriodoRelatorio` e `FiltroProdutosMaisVendidos` (validação no construtor: período obrigatório e não invertido, limite de 1 a 50), os registros `ResumoVendas` (ticket médio por divisão `HALF_EVEN` com 2 casas, zero sem vendas), `ProdutoMaisVendido` e `DescontoPorCampanha`, e a porta `ConsultarRelatorios`, com Javadoc que cita o critério de venda. Verificar: testes unitários do período (nulo, invertido, `inicio` igual a `fim`) do limite (0, 51, 1, 50) e do ticket médio (arredondamento, zero).
- [x] 1.2 Implementar `ConsultarRelatoriosJdbc` em `infra.persistencia` com parâmetros vinculados: resumo em uma consulta, ranking de produtos e campanhas conforme o design. Verificar nos testes de integração do grupo 3.

## 2. Aplicação, API e segurança

- [x] 2.1 Implementar `ConsultarRelatoriosGerenciais` (`@Transactional(readOnly = true)`, `@PreAuthorize` para ADMIN e OPERADOR_COMERCIAL, e PROMOCOES também em campanhas) e `RelatoriosController` com `GET /api/admin/relatorios/vendas`, `/produtos-mais-vendidos` e `/campanhas`, parâmetros de período e limite lidos como texto e convertidos em `DadoInvalidoException`, OpenAPI com 200/400/401/403.
- [x] 2.2 Acrescentar as regras em `ConfiguracaoSeguranca` antes da regra geral `/api/admin/**` (campanhas com PROMOCOES, demais relatórios sem PROMOCOES). Verificar: 401, 403 e 200 por perfil nos testes do grupo 3. Verificar ArchUnit (`RegrasDeArquiteturaTests`), incluindo a regra de que o módulo `relatorio` não é dependido por outros módulos.

## 3. Testes de integração (T-RL-01)

- [x] 3.1 Testar o relatório de vendas sobre pedidos conhecidos (valores, datas ajustadas em `pedido_historico` e descontos com `referencia_id` controlados): valor total, quantidade, ticket médio, total de descontos, cancelamentos no período, exclusão de pago depois cancelado, de pedidos não pagos e de pagos fora do período, bordas inclusivas e período vazio com zeros.
- [x] 3.2 Testar produtos mais vendidos (soma entre pedidos, ordenação e desempate, limite e padrão, exclusão de não vendas) e campanhas (agrupamento por referência, campanha e cupom no mesmo pedido, frete, legado sem referência, ordenação, exclusão de não vendas).
- [x] 3.3 Testar a validação (período ausente, malformado, invertido, limite inválido: 400 sem consultar) e a segurança (401; ADMIN e OPERADOR_COMERCIAL com 200; PROMOCOES só em campanhas; ESTOQUE e CLIENTE com 403; serviço negando fora da API; sem rotas de escrita; OpenAPI).

## 4. Documentação e validação

- [x] 4.1 Atualizar `docs/02-modelo-dominio.md`, `03-arquitetura.md`, `04-design-patterns.md` (nota de que a E9 não introduz pattern), `07-casos-de-uso.md` (UC-A10 `implementado`), `08-roadmap.md`, `09-testes.md` (T-RL-01), `10-execucao.md` (exemplos `curl` dos relatórios) e `12-board.md` (`relatorios-gerenciais` em revisão; implementado por Arthur, atribuição original Lucas), mais Javadoc das classes principais. Conferir T-RL-01 contra os testes executados.
- [x] 4.2 Executar `./mvnw -o clean verify` completo (zero falhas, erros e ignorados), `openspec validate relatorios-gerenciais --strict` e `git diff --check`, sem git add, commit, PR ou arquivamento.

## Evidência — implementação

Em 2026-10-23, tarefas 1.1 a 4.2 concluídas. Módulo `relatorio` (domínio, aplicação, JDBC e controller) com `GET /api/admin/relatorios/vendas`, `/produtos-mais-vendidos` e `/campanhas`, regras de segurança antes de `/api/admin/**`, sem migração nova e sem Design Pattern (decisão registrada em design.md e em docs/04). Testes novos: `RelatoriosDominioTests` (13) e `RelatoriosGerenciaisTests` (16, T-RL-01 com PostgreSQL), mais duas regras ArchUnit para o módulo; seleção focada com `RegrasDeArquiteturaTests`: 41 testes aprovados. `./mvnw -o clean verify`: **BUILD SUCCESS, 649 testes, zero falhas/erros/ignorados**, JAR gerado. `openspec validate relatorios-gerenciais --strict` e `git diff --check` aprovados. Docs/02, 03, 04, 07, 08, 09, 10, 11 e 12 atualizados. Sem git add, commit, PR ou arquivamento.
