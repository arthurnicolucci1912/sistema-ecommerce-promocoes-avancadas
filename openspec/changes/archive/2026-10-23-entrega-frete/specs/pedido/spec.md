## MODIFIED Requirements

### Requirement: Snapshot comercial do pedido
Ao ser criado, o pedido SHALL guardar uma cópia dos dados comerciais: para cada item, identificação, nome, descrição da variação, quantidade e preço unitário; os descontos aplicados, com nome e valor; o frete; o subtotal, o total de descontos e o total; o endereço de entrega; e, para novas compras no checkout, código da opção de entrega, fornecedor, modalidade e prazo estimado em dias úteis após aprovação. Esses dados MUST NOT mudar depois da criação, mesmo que preços, promoções, tarifas, prazos ou o endereço do cliente mudem. Pedidos legados sem opção registrada MUST preservar seus valores e expor a opção como ausente, sem fabricar dados de entrega.

#### Scenario: Pedido criado em AGUARDANDO_PAGAMENTO
- **WHEN** um pedido é criado a partir de um snapshot válido e a reserva é confirmada
- **THEN** o pedido fica em `AGUARDANDO_PAGAMENTO` com itens, descontos, frete e totais iguais aos do snapshot

#### Scenario: Preço alterado depois da compra
- **WHEN** um item foi comprado a R$ 220,00 e o snapshot de um novo pedido do mesmo item informa R$ 250,00
- **THEN** o pedido antigo continua mostrando R$ 220,00

#### Scenario: Endereço alterado depois da compra
- **WHEN** o cliente altera ou remove o endereço usado em um pedido
- **THEN** o pedido continua mostrando o endereço do momento da compra

#### Scenario: Totais inconsistentes
- **WHEN** o snapshot informa um total diferente de subtotal menos descontos mais frete
- **THEN** o pedido não é criado e o sistema rejeita o snapshot

#### Scenario: Cotação alterada depois da compra
- **WHEN** o fornecedor muda tarifa ou prazo depois que o pedido é criado
- **THEN** o pedido mantém a modalidade, fornecedor, prazo e frete originais, inclusive após pagamento e envio

#### Scenario: Pedido legado
- **WHEN** um pedido anterior à integração de frete é consultado
- **THEN** seus valores e endereço permanecem iguais e a opção de entrega está ausente
