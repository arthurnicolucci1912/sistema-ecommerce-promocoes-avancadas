# Tasks

## 1. Modelo persistente e concorrência

- [x] 1.1 Criar cobrança, confirmação e estorno com invariantes e repositórios; verificar testes de valor, estados e unicidade por pedido/transação.
- [x] 1.2 Implementar V6 aditiva e persistência PostgreSQL; verificar upgrade V5 com dados preservados e atualizar limpeza dos fixtures sem quebrar a suíte existente.
- [x] 1.3 Implementar bloqueio por pedido também no cancelamento e ordem de locks documentada; verificar disputa de cancelamentos sem deadlock e manter regras ArchUnit.
- [x] 1.4 Atualizar docs/02 e 03 com entidades, dependências e transações; conferir correspondência com migração e contratos reais.

## 2. Gateways e início de cobrança

- [x] 2.1 Implementar três adapters com APIs simuladas distintas e chaves idempotentes; verificar aprovação/recusa de cartão, pendência PIX/boleto e tradução de estorno em testes unitários.
- [x] 2.2 Implementar registro e configuração de fornecedores; verificar substituição por fornecedor de teste sem alterar o núcleo, seleção inválida no startup e resolução do fornecedor original para cobranças antigas.
- [x] 2.3 Implementar início/consulta pelo CLIENTE com total do snapshot e identidade JWT; verificar 201/200, 400/401/403/404/409 e repetição/concorrência sem segunda cobrança.
- [x] 2.4 Documentar Adapter, participantes reais e papéis em Javadoc e docs/04; atualizar docs/10 com início e consulta executáveis sem dados reais de cartão.

## 3. Confirmação única e efeitos comerciais

- [x] 3.1 Implementar confirmação persistente com inserção sem erro de unicidade transacional e comparação de resultado; verificar replay sequencial/simultâneo 200, conteúdo divergente 409 e desconhecido 404.
- [x] 3.2 Encaminhar resultado síncrono e callback ADMIN pelo mesmo processamento usando transições State e eventos existentes; verificar aprovação, recusa, estoque e devolução única do cupom.
- [x] 3.3 Verificar rollback com falha injetada após transição/efeitos, reenvio bem-sucedido, confirmação após cancelamento e corrida pagamento/cancelamento com timeout e sequência comercial válida.
- [x] 3.4 Atualizar docs/05, 06, 07 e 09, reconciliando RN-24/25 e UC-S01 sobre confirmação tardia; conferir endpoints, diagrama e cenários com testes reais.

## 4. Estorno e recuperação

- [x] 4.1 Implementar ouvinte Observer de cancelamento após estoque/cupons, sem dependência do domínio pedido em pagamento; verificar estorno integral único em PAGO/EM_PREPARACAO e ausência de estorno sem aprovação.
- [x] 4.2 Persistir falha recuperável e permitir tentativa administrativa idempotente; verificar falha seguida de sucesso, concorrência, permissões, fornecedor original e ausência de novas devoluções de estoque/cupom.
- [x] 4.3 Tratar pedidos legados sem transação com pendência explícita; verificar cancelamento preservado e retentativa 409 sem transação inventada.
- [x] 4.4 Atualizar Javadoc, docs/04, 05, 06, 09 e 10 com Observer e roteiro de recuperação; verificar novo ouvinte de teste sem alterar módulo pedido.

## 5. Substituição do atalho e validação integrada

- [x] 5.1 Migrar testes de PagamentoSimuladoTests, CheckoutIntegracaoTests e PromocoesCuponsIntegracaoTests para cobrança/confirmação e remover controller/request temporários; verificar cobertura equivalente e ausência da rota no OpenAPI.
- [x] 5.2 Revisar documentação 03–10 e 12, mantendo responsáveis, explicitando expiração automática pendente e removendo instruções obsoletas; verificar referências com busca textual e roteiro de compra/pagamento/cancelamento.
- [x] 5.3 Executar Maven verify completo e validar pagamento-idempotencia com OpenSpec strict; registrar contagem real e resultados, sem commit ou arquivamento automático.

## Evidência de conclusão

Em 2026-10-22, `./mvnw clean verify`: BUILD SUCCESS, 562 testes, zero falhas/erros/ignorados e JAR gerado. Os testes novos verificam persistência V6 e upgrade V5, três APIs distintas, extensão por fornecedor registrado, identidade/autorização, replays e resultados conflitantes, concorrência de início/confirmação/cancelamento/retentativa, rollback após estoque/cupom/observador, estorno único/falho/legado e ausência do atalho no OpenAPI. A regressão de checkout e promoções usa os novos endpoints. A validação estrita OpenSpec e `git diff --check` passaram. Revisão humana, commit, PR e arquivamento permanecem sob controle do autor.
