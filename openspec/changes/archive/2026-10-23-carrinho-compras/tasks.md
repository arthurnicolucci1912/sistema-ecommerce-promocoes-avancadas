# Tasks

## 1. Agregado e persistência do carrinho

- [x] 1.1 Implementar `Carrinho`, `ItemCarrinho` e `CarrinhoRepository`, com inclusão acumulativa, substituição, remoção e validação de quantidades/estouro; verificar testes unitários de linhas repetidas, valores inválidos e remoção ausente.
- [x] 1.2 Criar migração V4 e adaptador JPA com cliente único, SKU único por carrinho, FK, quantidade positiva e controle de versão do agregado; verificar migração do zero, persistência entre sessões e conflito concorrente inclusive em alteração só de filhos com PostgreSQL/Testcontainers.
- [x] 1.3 Atualizar docs/02-modelo-dominio.md e docs/03-arquitetura.md com agregado e persistência reais e docs/09-testes.md com IDs T-CA para domínio/persistência; verificar correspondência com as classes e testes entregues.

## 2. Pipeline de formação do preço

- [x] 2.1 Implementar Chain of Responsibility com `PipelinePreco`, `EtapaSubtotal`, `EtapaTotal` e composição explícita de etapas, preservando `EtapaPreco`/`CalculoPreco`; verificar soma, carrinho vazio, precisão monetária e independência entre cálculos em testes unitários.
- [x] 2.2 Demonstrar extensibilidade registrando handler de teste que acrescenta frete entre subtotal e total sem alterar handlers existentes; verificar subtotal 100 e total 105, e ajustar ArchUnit apenas para implementações de `EtapaPreco` no módulo de preço, mantendo as demais restrições.
- [x] 2.3 Documentar classes e papéis reais em docs/04-design-patterns.md e Javadoc dos participantes, atualizar docs/03 sobre `ItemPreco.varianteId` em combos e docs/05 sobre a parcela implementada das RN-11 a RN-14; verificar rastreabilidade em docs/09 para os testes do pipeline.

## 3. Aplicação, catálogo e disponibilidade

- [x] 3.1 Implementar consulta e manutenção do carrinho usando identidade autenticada, cliente proprietário e preços atuais; verificar testes de inclusão repetida, atualização de preço, SKU ausente, inativação, redução/remoção de inativo e rollback de falhas sem alteração parcial.
- [x] 3.2 Consumir uniformemente variações e combos via contrato do catálogo e agregar demanda física de componentes antes da consulta de saldo, sem reservar; verificar kit com preço próprio, kit aninhado, componente compartilhado, saldo ausente, aviso de insuficiência e estoque intacto. Demonstrar novo tipo de item vendável em teste sem condicionais por tipo no carrinho.
- [x] 3.3 Atualizar docs/04 com o consumidor real de Composite, docs/05 RN-17 e docs/09 com testes T-CA de catálogo/disponibilidade; verificar que avisos de estoque e ausência de reserva estão documentados como comportamento implementado.

## 4. API do cliente

- [x] 4.1 Expor GET do carrinho, POST de inclusão, PUT de quantidade e DELETE de SKU em `/api/clientes/me/carrinho`, retornando resumo/avisos, com validação e tratamento padronizado de 400/404/409; verificar testes web de CRUD, vazio, quantidade fracionária e resumo recalculado.
- [x] 4.2 Proteger aplicação e rotas com CLIENTE, obtendo identidade do token sem clienteId fornecido pelo chamador; verificar 401 sem token, 403 de perfis sem CLIENTE e isolamento entre dois clientes sem alterações cruzadas.
- [x] 4.3 Atualizar UC-04/UC-06 em docs/07-casos-de-uso.md, exemplos reproduzíveis em docs/10-execucao.md e rastreabilidade em docs/09-testes.md; verificar exemplos contra a API e schemas OpenAPI da implementação.

## 5. Integração e entrega para revisão

- [x] 5.1 Executar `./mvnw verify` com PostgreSQL/Testcontainers e exercitar fluxo autenticado de variação/kit, atualização de quantidade, mudança de preço, aviso de falta de estoque e remoção; verificar saldo/reservas inalterados e suíte de regressão aprovada.
- [x] 5.2 Atualizar docs/08-roadmap.md e docs/12-board.md com situação real e evidências da etapa, preservando atribuições e revisão humana pendente da onda 0; verificar `openspec validate carrinho-compras --strict` e encaminhar a change para revisão, sem commit nem arquivamento automáticos.

## Evidências da implementação

- 2026-10-22: revalidação com `./mvnw -o verify` — BUILD SUCCESS, 480 testes, zero falhas, erros ou ignorados; `openspec validate carrinho-compras --strict` aprovado. Entrega permanece aguardando revisão do autor.
- 2026-10-21: `./mvnw -o verify` — BUILD SUCCESS, 480 testes, zero falhas, erros ou ignorados; JAR gerado.
- Testes E3: `CarrinhoTests`, `CarrinhoPersistenciaTests`, `PipelinePrecoTests`, `CarrinhoAplicacaoTests`, `CarrinhoWebTests`; regressão dos demais módulos e ArchUnit aprovada.
- `openspec validate carrinho-compras --strict` aprovado.
- Implementação pronta para revisão local; sem commit, PR ou arquivamento automático. Revisão conjunta da onda 0 permanece pendente no board.
