# Proposal

## Why
O checkout conclui a compra, mas o pagamento ainda depende de uma rota administrativa temporária. A E6 completa cobrança simulada, confirmação idempotente e estorno, preservando estoque e cupons já integrados.

## What Changes
- Iniciar e consultar cobrança pelo cliente dono do pedido, com cartão, PIX e boleto simulados.
- Confirmar aprovação/recusa com autenticação administrativa de integração simulada e unicidade persistente da transação externa.
- Garantir atomicidade entre confirmação, histórico, eventos e efeitos de estoque/cupons, inclusive em concorrência.
- Estornar uma única vez no cancelamento pago, registrando falhas recuperáveis e permitindo nova tentativa administrativa.
- **BREAKING**: remover `POST /api/admin/pedidos/{id}/pagamento-simulado`; demonstrações e testes passam pela cobrança e confirmação.
- Atualizar documentação, migração aditiva e roteiro executável. Expiração automática não integra este incremento: o roadmap E6 não define prazo nem rotina, e será explicitada como pendência.

## Capabilities

### New Capabilities
- `pagamento`: cobrança, confirmação idempotente, consulta, estorno e troca de fornecedor simulado.

### Modified Capabilities
- `pedido`: remover o requisito de simulação temporária substituído pela integração de pagamento.

## Design Patterns
- Adapter: fornecedores de pagamento com interfaces distintas, exigência do enunciado; troca sem alterar regras centrais.
- Observer: reutilizar eventos do pedido para estoque/cupons e adicionar estorno ao cancelamento.
- State: reutilizar transições válidas do pedido, sem criar estados novos.

## Impact
Módulo `pagamento`, segurança, persistência Flyway V6, integração com `pedido`, testes de checkout/promoções e documentação 02–10/12. Checkout permanece com conclusão separada da ação de pagar (UC-08), sem recalcular o snapshot. Não há integração financeira real, dependência externa nova, commit ou arquivamento automático.
