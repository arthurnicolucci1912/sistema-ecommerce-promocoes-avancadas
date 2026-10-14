# Proposal

## Why

O projeto começa do zero e hoje só tem o enunciado (`Sistema de E-commerce.pdf`). O PDF cobra documentação como entrega: requisitos, modelo de domínio, arquitetura, casos de uso, diagrama de classes, ciclo de vida do pedido, diagramas de sequência e a justificativa de cada Design Pattern. Escrever essa base antes do código alinha as decisões de arquitetura e o roadmap incremental. Também evita que a documentação vire um esforço de última hora, desconectado da implementação.

## What Changes

- Criar a pasta `docs/` na raiz do repositório com 11 arquivos Markdown:
  - `README.md`: índice, visão geral do sistema e de como os documentos se ligam às etapas de produção.
  - `01-requisitos.md`: requisitos funcionais e não funcionais extraídos do enunciado, separados em obrigatórios e opcionais.
  - `02-modelo-dominio.md`: entidades, relacionamentos e decisões (variações de produto, categorias hierárquicas, combos/kits).
  - `03-arquitetura.md`: camadas, módulos, pacotes e contratos centrais (pontos de variação).
  - `04-design-patterns.md`: patterns candidatos, cada um com problema, motivo da escolha, classes participantes e benefício.
  - `05-regras-negocio.md`: conflito entre promoções, cupons, formação do preço, reserva de estoque, idempotência, cancelamento e frete grátis.
  - `06-ciclo-vida-pedido.md`: estados e transições válidas do pedido, com diagrama.
  - `07-casos-de-uso.md`: principais casos de uso e roteiros para os diagramas de sequência.
  - `08-roadmap.md`: as 9 etapas incrementais, cada uma mapeada para uma futura change do OpenSpec.
  - `09-testes.md`: cenários de teste obrigatórios do enunciado e onde cada um será coberto.
  - `10-execucao.md`: como executar o sistema (placeholder até existir código).
- Nenhum código de aplicação é criado. Nenhum commit é feito: os arquivos ficam no working tree para revisão do autor.

## Capabilities

### New Capabilities

Nenhuma. A change só cria documentação, não altera o comportamento do sistema. Por isso `.openspec.yaml` declara `skip_specs: true`.

### Modified Capabilities

Nenhuma.

## Impact

- Novo diretório `docs/` com 11 arquivos `.md`.
- Nenhum impacto em código, API, dependências ou banco de dados.
- Os documentos viram referência para as próximas changes (fundação, catálogo, carrinho, checkout, promoções, pagamento, entrega, eventos, relatórios). Cada uma deverá atualizar os documentos correspondentes quando implementada.
