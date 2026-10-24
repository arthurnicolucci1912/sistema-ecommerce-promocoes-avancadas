# Design

## Context

Ver proposal.md. `pedido`, `pedido_item`, `pedido_desconto` e `pedido_historico` (V2, V5) guardam o snapshot comercial imutável: preços, itens, descontos com origem (`PROMOCAO` ou `CUPOM`), `referencia_id` da campanha ou cupom, `codigo` e o histórico de transições com `ocorrido_em`. O módulo `auditoria` (E8) é o molde: porta no domínio, serviço de aplicação `@Transactional(readOnly = true)` com `@PreAuthorize`, consulta JDBC com parâmetros vinculados e filtro que valida no construtor (`DadoInvalidoException` vira 400).

## Goals / Non-Goals

**Goals:** responder os indicadores de RF-66 e RF-72 para um período com um critério único e documentado de "venda"; leitura pura, sem alterar o modelo transacional; resultados determinísticos.

**Non-Goals:** exportação (CSV/PDF), gráficos, relatórios agendados, filtros por cliente/categoria, cache e tabelas de resumo materializadas, relatórios por fuso local. Não há migração nova.

## Decisions

### Design Patterns

Esta change **não usa Design Pattern**. O quadro abaixo registra a decisão de não aplicar, em conformidade com `openspec/config.yaml` e `docs/04-design-patterns.md`.

| Pattern | Papel | Problema do enunciado | Classes participantes | O que fica mais fácil de modificar |
|---|---|---|---|---|
| nenhum | não se aplica | O enunciado fixa os indicadores (RF-66, RF-72); não há comportamento variável nem tipos de relatório a acrescentar | não se aplica | não se aplica |

Alternativa descartada: Strategy por tipo de relatório. Seria uma interface com uma implementação por consulta, sem variação a absorver e sem segundo cliente (cada endpoint chama exatamente uma consulta); o ganho de extensibilidade seria hipotético e o pattern, artificial. Alternativa também descartada para "motor" genérico de relatório parametrizado por SQL: abriria superfície de injeção e esconderia critérios de negócio. Partes sem pattern próprio: filtro de período, registros de resultado, porta de consulta, consultas JDBC, serviço de aplicação e controller. Por isso a spec não traz requisito de extensibilidade.

### Estrutura do módulo `relatorio`

`dominio`: `PeriodoRelatorio` (inicio, fim; valida no construtor), `FiltroProdutosMaisVendidos` (período e limite), registros de resultado `ResumoVendas`, `ProdutoMaisVendido`, `DescontoPorCampanha` e a porta `ConsultarRelatorios`. `aplicacao`: `ConsultarRelatoriosGerenciais`. `infra.persistencia`: `ConsultarRelatoriosJdbc` (`NamedParameterJdbcTemplate`). `web`: `RelatoriosController`. O domínio não depende de web, infra, aplicação nem Spring; web não depende de infra; `relatorio` lê as tabelas de `pedido` por SQL, sem importar classes de outros módulos (leitura por consulta, como a auditoria).

### O que é uma venda

Venda é o pedido que tem registro em `pedido_historico` com `para = 'PAGO'` e `ocorrido_em` dentro do período (inclusivo) **e** cuja `pedido.situacao` atual não é `CANCELADO`. O instante do pagamento, não o de criação, define o período; assim o relatório não conta pedidos ainda não pagos ou recusados. Pedido pago e depois cancelado deixa de ser venda (o valor foi estornado). Pedido pago, enviado ou entregue permanece venda. O valor de cada venda é `pedido.total` (já com descontos e frete). O mesmo critério alimenta os três relatórios, o que garante coerência entre eles.

### Indicadores de resumo

`valorTotalVendas` = soma de `pedido.total` das vendas; `quantidadePedidos` = número de vendas; `ticketMedio` = valor total dividido pela quantidade, `HALF_EVEN` com 2 casas e zero se a quantidade é zero; `totalDescontos` = soma de `pedido_desconto.valor` das vendas (itens e frete, promoções e cupons); `quantidadeCancelamentos` = transições `para = 'CANCELADO'` em `pedido_historico` com `ocorrido_em` no período, independentemente de o pedido ter sido pago (mede cancelamentos ocorridos). Tudo em uma única consulta, para que os cinco números venham do mesmo instantâneo. Valores monetários saem como `BigDecimal` com escala 2, como nas demais respostas.

### Produtos mais vendidos

Sobre `pedido_item` das vendas, agrupado por `sku`, `nome_produto` e `descricao_variacao` (o snapshot, não o catálogo atual, para que renomeações não alterem o histórico). Soma `quantidade` e `total_item`. Ordem: quantidade decrescente, `sku` crescente, com desempate adicional por nome e descrição para resultado determinístico. `limite` de 1 a 50, padrão 10; fora disso, 400. A limitação ocorre no banco (`LIMIT`).

### Campanhas

Sobre `pedido_desconto` das vendas, agrupado por `(origem, referencia_id)`; quando `referencia_id` é nulo (descontos legados anteriores à V5) agrupa por `(origem, descricao)`. Cada linha traz `origem`, `referenciaId`, `descricao`, `codigo`, `quantidadePedidos` (pedidos distintos) e `valorConcedido`. Ordem: quantidade de pedidos decrescente, valor decrescente, descrição crescente (e origem e referência para desempate). Uma única consulta cobre "campanhas mais utilizadas" e "desconto concedido por campanha". A descrição exibida é a mais alta lexicograficamente entre os snapshots do grupo, para que uma campanha renomeada continue em uma única linha.

### Período

`inicio` e `fim` são obrigatórios, instantes ISO-8601 com fuso, intervalo inclusivo nas duas pontas, resposta em UTC. Ausente, malformado ou `inicio` posterior a `fim` resulta em 400 (Problem Details), validado antes de qualquer consulta ao banco. Período sem dados devolve zeros e lista vazia com 200.

### Segurança

`ConfiguracaoSeguranca` ganha, antes da regra geral `/api/admin/**` (a primeira regra que casa vence), `/api/admin/relatorios/campanhas` para `ADMIN`, `OPERADOR_COMERCIAL` e `PROMOCOES`, e `/api/admin/relatorios/**` para `ADMIN` e `OPERADOR_COMERCIAL`. O controller e o serviço de aplicação repetem a restrição com `@PreAuthorize`, de modo que a regra vale também fora do HTTP. A regra de campanhas precede a mais ampla.

### Persistência

Sem migração nova (última é V8). Os filtros usam `pedido_historico (pedido_id)` já indexado e a lista de vendas é filtrada por `EXISTS` sobre o histórico; para o volume desta fase não é necessário índice por `(para, ocorrido_em)`. Se o volume crescer, um índice parcial em `pedido_historico` seria uma migração aditiva, registrada como risco.

## Risks / Trade-offs

- Consulta sem índice por data de pagamento → varredura do histórico em bases grandes; aceito nesta fase e documentado, com índice aditivo como evolução.
- Pedido pago e depois cancelado fora do período do cancelamento → entra em "cancelamentos" do mês do cancelamento e não conta como venda em nenhum mês; é o critério documentado, testado com datas distintas.
- Descrição de campanha renomeada → agrupada pelo `referencia_id` e exibida pela descrição mais alta; legados sem referência agrupam pela descrição.
- Relatórios refletem a situação atual do pedido (cancelamento posterior retira a venda de períodos passados) → decisão consciente: o total vendido representa a receita não estornada.

## Migration Plan

Sem migração de banco nem alteração dos dados. A reversão é remover o módulo e as regras de segurança; nenhum dado é gravado.
