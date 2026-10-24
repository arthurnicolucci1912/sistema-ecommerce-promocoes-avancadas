# Proposal

## Why
O cliente já conclui e paga compras, mas o frete continua zero na API e não há escolha de entrega. A E7 conecta cotação, descontos sobre frete e snapshot ao ciclo de envio/entrega existente.

## What Changes
- Consultar modalidades econômica e expressa simuladas para endereço próprio, com custo regional e prazo estimado.
- **BREAKING**: exigir a seleção de entrega na prévia/conclusão do checkout; substituir frete zero fixo pela cotação corrente.
- Aplicar promoções e cupons de frete à cotação, discriminando valor bruto, descontos e frete líquido sem abatimento duplicado.
- Revalidar modalidade, fornecedor, prazo e custo antes de comprar; preservar esses dados no pedido via migração aditiva V7.
- Demonstrar compra, pagamento, preparação, envio e entrega usando as transições e consultas existentes.

## Capabilities

### New Capabilities
- `entrega`: opções de frete, cobertura, simulação regional e extensão por transportadora.

### Modified Capabilities
- `checkout`: seleção obrigatória, prévia com entrega e reconfirmação de alterações na cotação.
- `formacao-preco`: cotação comercial na entrada e apresentação separada do frete bruto/líquido.
- `pedido`: preservar fornecedor, modalidade e prazo junto ao snapshot comercial.

## Design Patterns
- Adapter: transportadoras com interfaces distintas, requisito de serviços novos sem alterar regras centrais.
- Chain of Responsibility e Strategy existentes: preço composto e benefício de frete passam a receber cotação real simulada, preservando política de conflito promocional.
- Facade e State existentes: coordenação atômica do checkout e andamento válido do pedido.
- Não criar Strategy adicional de modalidade: o ponto de variação já é atendido pelo contrato de provedor.

## Impact
`entrega`, checkout, contexto comercial/preço, snapshot/resposta de pedido, testes de compra/pagamento/promoções, Flyway V7 e docs/02–10/12. Custos simulados variam por região/modalidade, sem inventar pesos ausentes no catálogo. Seleção fica no checkout; carrinho continua resumo anterior à escolha de entrega. Sem contratação logística real, rastreamento externo, novos estados ou alteração das permissões de envio. Changes E3–E6 permanecem preservadas e não serão arquivadas/commitadas automaticamente.
