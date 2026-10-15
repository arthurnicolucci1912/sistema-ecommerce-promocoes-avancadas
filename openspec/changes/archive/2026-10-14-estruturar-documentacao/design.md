# Design

## Context

A motivação está em `proposal.md`. Não há código nem specs. As decisões abaixo saíram da sessão de explore sobre o enunciado e definem tanto o formato quanto o conteúdo inicial dos documentos. Stack exigida pelo enunciado: Java + Spring Boot, Spring Data JPA, Spring Security, API REST com OpenAPI/Swagger e banco relacional (PostgreSQL).

## Goals / Non-Goals

**Goals:**
- Dar a cada exigência de documentação do enunciado um arquivo dono.
- Registrar desde já as decisões de arquitetura e regras de negócio discutidas, como base para as próximas changes.
- Deixar explícito o que é decisão e o que ainda é proposta (marcar `Status: proposto` onde a decisão ainda pode mudar na implementação).

**Non-Goals:**
- Diagramas UML finais com nomes de classe definitivos. Nesta etapa os diagramas são preliminares e serão refinados em cada change de implementação.
- Documentação de API (endpoints). Ela virá do Swagger quando houver código.
- Qualquer código, build ou configuração de projeto.

## Decisions

### D1. Idioma e formato
Documentos em português. Diagramas em **Mermaid** dentro do Markdown (`classDiagram`, `stateDiagram-v2`, `sequenceDiagram`), que o GitHub renderiza nativamente.
- Alternativa: PlantUML ou imagens exportadas. Rejeitada porque exige ferramenta extra e fica fora do diff.

### D2. Numeração e cabeçalho comum
Arquivos com prefixo numérico, na ordem de leitura. Cada documento começa com um bloco curto contendo: objetivo do documento, etapa(s) do roadmap relacionadas e status (`rascunho` / `proposto` / `implementado`). Assim, cada change futura sabe quais documentos precisa atualizar.

### D3. Pontos de variação e patterns candidatos (conteúdo de 03 e 04)
| Problema | Pattern | Categoria |
|---|---|---|
| Tipos de promoção | Strategy | Comportamental |
| Condições de elegibilidade combináveis (E/OU/NÃO) | Composite (Specification) | Estrutural |
| Formação do preço em etapas | Chain of Responsibility (pipeline) | Comportamental |
| Ciclo de vida do pedido | State | Comportamental |
| Gateways de pagamento heterogêneos | Adapter | Estrutural |
| Reação a eventos (pagamento confirmado etc.) | Observer (eventos de domínio) | Comportamental |
| Criação de promoções a partir da configuração | Factory Method / registry | Criação |
| Produto simples vs. combo/kit | Composite | Estrutural |
| Orquestração do checkout | Facade | Estrutural |

O mínimo exigido é 6 patterns, sendo pelo menos 2 comportamentais e 1 de criação, e a lista cobre isso com folga. Todos ficam marcados como `proposto`. Para cada pattern, o documento 04 traz o modelo de justificativa exigido pelo enunciado: problema existente, por que este pattern, classes participantes e o que ficou mais fácil de modificar.

### D4. Política de conflito entre promoções (conteúdo de 05)
1. Filtrar as promoções elegíveis.
2. Se houver alguma marcada como **exclusiva**, aplicar apenas a exclusiva de maior desconto.
3. Caso contrário, aplicar todas as **acumuláveis** em ordem de prioridade, mais a melhor entre as **não acumuláveis**.
4. Em caso de empate, vence a maior prioridade e, depois, a promoção mais antiga.

A própria política é um ponto de variação: é uma Strategy trocável.

### D5. Estoque (conteúdo de 05)
- O carrinho só consulta disponibilidade, não reserva.
- A reserva acontece na conclusão do checkout, quando o pedido entra em `AGUARDANDO_PAGAMENTO`.
- Pagamento aprovado converte a reserva em baixa. Pagamento recusado, cancelamento ou expiração liberam a reserva.
- A concorrência pela última unidade é resolvida com atualização condicional atômica (decrementar somente se `disponivel >= qtd`) ou lock otimista. A escolha final fica para a change de checkout.

### D6. Idempotência do pagamento (conteúdo de 05 e 09)
Toda confirmação externa carrega um identificador da transação. Esse identificador é registrado com restrição de unicidade, e uma confirmação repetida não gera efeito (no-op), sem segunda baixa de estoque ou segunda transição de pedido.

### D7. Ciclo de vida do pedido (conteúdo de 06)
Estados: `CRIADO`, `AGUARDANDO_PAGAMENTO`, `PAGO`, `EM_PREPARACAO`, `ENVIADO`, `ENTREGUE`, `CANCELADO`, `PAGAMENTO_RECUSADO`. Transições válidas:
- `CRIADO` vai para `AGUARDANDO_PAGAMENTO`.
- `AGUARDANDO_PAGAMENTO` vai para `PAGO` ou `PAGAMENTO_RECUSADO`.
- `PAGO` vai para `EM_PREPARACAO`.
- `EM_PREPARACAO` vai para `ENVIADO`.
- `ENVIADO` vai para `ENTREGUE`.

Cancelamento é permitido a partir de `CRIADO`, `AGUARDANDO_PAGAMENTO`, `PAGAMENTO_RECUSADO`, `PAGO` e `EM_PREPARACAO`. Quando já existe pagamento, o cancelamento dispara um estorno simulado. Qualquer outra transição é inválida e gera erro. Todo cancelamento fica no histórico.

### D8. Roadmap (conteúdo de 08)
1. fundação
2. catálogo e estoque
3. carrinho
4. checkout e pedido
5. promoções e cupons
6. pagamento e idempotência
7. entrega e frete
8. eventos, notificações e auditoria
9. relatórios

Cada etapa vira uma change do OpenSpec e precisa deixar o sistema executável ao final.

## Risks / Trade-offs

- [Documentos desatualizarem em relação ao código] → O cabeçalho de status (D2) e as tasks de cada change futura incluem a atualização do documento correspondente.
- [Decisões prematuras engessarem a implementação] → Os itens ficam marcados como `proposto` e podem ser revistos na change da etapa, registrando o motivo da mudança.
- [Patterns parecerem "forçados" na avaliação] → O documento 04 exige que cada pattern seja ligado a um requisito concreto do enunciado. Patterns sem esse vínculo são removidos.

## Open Questions

- Mecanismo exato de concorrência no estoque (atualização condicional ou `@Version`): será decidido na change de checkout, sem impacto nesta documentação.
