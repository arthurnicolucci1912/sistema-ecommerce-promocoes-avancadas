# Proposal

## Why

O catálogo, o estoque e o ciclo de vida do pedido já estão implementados e arquivados, mas o cliente ainda não consegue montar uma compra. O carrinho é a próxima dependência do checkout na sequência da seção 7 do roadmap.

## What Changes

- Carrinho persistente por cliente autenticado: consultar, incluir SKU, alterar quantidade e remover item, incluindo combos.
- Recálculo com preços atuais a cada consulta e modificação, exibindo itens, subtotal, descontos, frete e total; descontos e frete ficam zerados nesta etapa.
- Consulta de disponibilidade dos componentes, inclusive componentes compartilhados entre linhas, sem reserva ou alteração do estoque.
- Pipeline extensível de preço usando os contratos existentes da onda 0.
- Testes de domínio, persistência, API, autorização e extensibilidade; documentação de uso e rastreabilidade.

## Capabilities

### New Capabilities

- `carrinho`: manutenção do carrinho próprio, validações, disponibilidade informativa e recálculo.
- `formacao-preco`: cálculo ordenado e extensível do resumo monetário.

### Modified Capabilities

Nenhuma. O catálogo e o estoque serão consumidos pelos contratos existentes, sem mudar seus requisitos.

## Impact

Novos pacotes `carrinho` nas camadas dominio/aplicacao/infra/web, migração Flyway V4 e implementação do pipeline em `preco`. Reutiliza `ConsultarItensVendaveis`, `ItemVendavelInfo`, `ConsultarSaldo`, `Dinheiro`, `EtapaPreco` e `CalculoPreco`. API proposta em `/api/clientes/me/carrinho`. Atualiza docs 02 a 05, 07 a 10 e board. Sem dependência nova; checkout, promoções, cupons e frete permanecem em suas etapas.

## Design Patterns

Chain of Responsibility atende ao requisito de formação do preço em etapas e à inclusão posterior de promoções, cupom e frete sem concentrar regras em um método. O ponto de variação são as etapas registradas no pipeline. O Composite já implementado no catálogo é consumido pelo contrato uniforme de itens vendáveis, mantendo o preço próprio do combo e a composição de estoque. CRUD, persistência e isolamento por cliente não precisam de pattern adicional.
