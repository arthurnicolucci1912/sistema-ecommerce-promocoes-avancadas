# Tasks

> Não fazer commits. Ao final, os arquivos ficam no working tree para revisão do autor.

## 1. Estrutura e índice

- [x] 1.1 Criar a pasta `docs/` e o `docs/README.md` com visão geral do sistema, índice com link para os 10 documentos numerados e a tabela documento x etapa do roadmap. Verificar: todos os links relativos apontam para arquivos existentes.

## 2. Requisitos e domínio

- [x] 2.1 Escrever `docs/01-requisitos.md` com requisitos funcionais e não funcionais extraídos do enunciado, separados em obrigatórios e opcionais (devolução, interface gráfica). Verificar: cada seção do PDF (usuários, clientes, catálogo, categorias, estoque, carrinho, promoções, cupons, preço, checkout, pedidos, pagamentos, idempotência, entrega, frete grátis, cancelamento, notificações, eventos, segurança, auditoria, relatórios, administração de promoções) tem ao menos um requisito.
- [x] 2.2 Escrever `docs/02-modelo-dominio.md` com entidades, relacionamentos e decisões sobre variações de produto, categorias hierárquicas e combos/kits, incluindo um `classDiagram` Mermaid preliminar. Verificar: o diagrama tem sintaxe Mermaid válida e cobre cliente, endereço, produto, variação, categoria, estoque, carrinho, pedido, item do pedido, promoção, cupom, pagamento e entrega.

## 3. Arquitetura e patterns

- [x] 3.1 Escrever `docs/03-arquitetura.md` com as camadas (domínio, aplicação, persistência, HTTP, integrações), a organização de pacotes por módulo, os pontos de variação e os contratos centrais (promoção, condição, política de conflito, gateway de pagamento, provedor de frete, canal de notificação, eventos de domínio). Verificar: nenhum módulo concentra catálogo, carrinho, promoções, checkout, pagamento e entrega ao mesmo tempo.
- [x] 3.2 Escrever `docs/04-design-patterns.md` com os patterns da tabela D3 do design. Para cada um: requisito do enunciado de origem, problema, por que este pattern, classes participantes (preliminar), benefício e status `proposto`. Incluir o quadro-resumo por categoria. Verificar: pelo menos 6 patterns, pelo menos 2 comportamentais e pelo menos 1 de criação.

## 4. Regras de negócio e ciclo de vida

- [x] 4.1 Escrever `docs/05-regras-negocio.md` com a política de conflito (D4), cupons (validade, limite de uso, compatibilidade), etapas de formação do preço (subtotal, promoções, cupom, frete, total), reserva de estoque (D5), idempotência (D6), cancelamento e estorno, e frete grátis como promoção. Verificar: cada regra tem pelo menos um exemplo numérico ou cenário.
- [x] 4.2 Escrever `docs/06-ciclo-vida-pedido.md` com os estados e transições de D7 em `stateDiagram-v2` e uma tabela de transições válidas e inválidas. Verificar: a tabela e o diagrama listam exatamente as mesmas transições.

## 5. Casos de uso, roadmap, testes e execução

- [x] 5.1 Escrever `docs/07-casos-de-uso.md` com os principais casos de uso (cliente e administradores) e `sequenceDiagram` Mermaid preliminares para checkout, aplicação de promoções e confirmação de pagamento. Verificar: os três diagramas de sequência exigidos estão presentes.
- [x] 5.2 Escrever `docs/08-roadmap.md` com as 9 etapas (D8). Para cada uma: escopo, nome sugerido da change OpenSpec, documentos a atualizar e critério de "sistema executável". Verificar: as etapas cobrem todos os requisitos obrigatórios de `01-requisitos.md`.
- [x] 5.3 Escrever `docs/09-testes.md` mapeando os cenários obrigatórios do enunciado (cada tipo de promoção, múltiplas promoções simultâneas, estoque insuficiente, cupom inválido, pagamento recusado, transição inválida, confirmação de pagamento repetida) para a etapa do roadmap em que serão testados. Verificar: todos os cenários citados no enunciado aparecem.
- [x] 5.4 Escrever `docs/10-execucao.md` como placeholder com pré-requisitos previstos (JDK, PostgreSQL, Maven/Gradle) e a seção "a preencher na etapa de fundação". Verificar: o arquivo existe e está referenciado no README.

## 6. Verificação final

- [x] 6.1 Conferir que `docs/` contém exatamente os 11 arquivos planejados, que todos os links internos resolvem e que nenhum commit foi criado (`git status` mostra `docs/` como não rastreado ou modificado).
