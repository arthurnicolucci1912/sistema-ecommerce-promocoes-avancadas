# Proposal

## Why

A E8 fechou o ciclo operacional (eventos, notificações e auditoria), mas a administração ainda não consegue responder perguntas gerenciais do enunciado: quanto foi vendido, qual o ticket médio, quais produtos e campanhas mais saem e quanto foi concedido em descontos. Os snapshots dos pedidos (itens, descontos e histórico) já guardam tudo o que é preciso; falta expô-los em consultas de leitura (RF-04, RF-66 e RF-72, UC-A10).

## What Changes

- Relatório de vendas de um período: valor total de vendas, quantidade de pedidos, ticket médio, total concedido em descontos e quantidade de cancelamentos.
- Relatório de produtos mais vendidos de um período, com limite configurável.
- Relatório de campanhas de um período: campanhas e cupons mais utilizados e desconto concedido por campanha.
- Período obrigatório e inclusivo (`inicio` e `fim`); período inválido é rejeitado e período sem dados devolve zeros e listas vazias.
- Acesso por perfil: `ADMIN` e `OPERADOR_COMERCIAL` em todos os relatórios; `PROMOCOES` somente no de campanhas.
- Atualização de documentação, Javadoc, roteiro de execução e testes (T-RL-01), fechando o roadmap da E9.

## Capabilities

### New Capabilities

- `relatorios`: consultas gerenciais somente leitura sobre vendas, produtos e campanhas, com critério explícito do que conta como venda.

### Modified Capabilities

Nenhuma. As consultas leem os dados de `pedido` e do desconto registrado no pedido sem alterar o modelo transacional, as transições ou a migração (nenhuma migração nova).

## Design Patterns

Esta change **não aplica nem prepara nenhum Design Pattern do enunciado, e isso é deliberado**: não há ponto de variação real. O enunciado pede um conjunto fixo de indicadores sobre dados já gravados (RF-66, RF-72); nada nele diz que tipos de relatório, formas de cálculo ou formatos de saída poderão mudar ou ser acrescentados. O roadmap também define "nenhum obrigatório" para a etapa 9. Os três relatórios são consultas distintas, não variantes de um mesmo comportamento.

Alternativa descartada: Strategy por tipo de relatório (uma interface `Relatorio` com uma implementação por indicador). Seria artificial: não resolveria variação de comportamento do enunciado, só trocaria três métodos de leitura por três classes e uma interface sem segundo uso, o que o `CLAUDE.md` proíbe. Se um dia surgirem tipos de relatório escolhidos em tempo de execução, o pattern deve ser introduzido nessa change, com o requisito que o justifique. Os patterns do projeto (Strategy, Composite, Chain of Responsibility, State, Adapter, Observer, Factory Method e Facade) já estão cobertos pelas etapas 2 a 8 e documentados em `docs/04-design-patterns.md`.

## Impact

Novo módulo `relatorio` (somente leitura sobre `pedido` e `promocao`, conforme `docs/03-arquitetura.md`) com os endpoints `GET /api/admin/relatorios/vendas`, `GET /api/admin/relatorios/produtos-mais-vendidos` e `GET /api/admin/relatorios/campanhas`; regras de segurança antes da regra geral `/api/admin/**`; OpenAPI; `docs/02`, `03`, `04` (nota), `07`, `08`, `09`, `10` e `12`. Sem dependência nova, sem escrita em dados de negócio.
