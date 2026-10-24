# Spec Delta

## REMOVED Requirements

### Requirement: Simulação temporária do resultado do pagamento
**Reason**: A E6 passa a processar cobranças e confirmações identificadas pelo fornecedor.
**Migration**: Remover POST /api/admin/pedidos/{id}/pagamento-simulado; iniciar cobrança em POST /api/clientes/me/pedidos/{id}/pagamento e confirmar resultados assíncronos em POST /api/admin/pagamentos/confirmacoes.
