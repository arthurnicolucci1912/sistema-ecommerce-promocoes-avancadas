# Tasks

## 1. Provedores e cotação simulada

- [x] 1.1 Validar os contratos de cotação/contexto/opção, acrescentar identidade do fornecedor e registro de opções inequívocas; verificar testes de UF, dinheiro, prazo, códigos e configuração duplicada/inválida.
- [x] 1.2 Implementar Adapter para duas APIs simuladas distintas (econômica/expressa), com tarifas e cobertura regionais configuráveis do design; verificar as cinco regiões, ausência de cobertura e correspondência dos preços/prazos da demonstração.
- [x] 1.3 Implementar consulta de opções a partir de carrinho/endereço próprios, sem escrita; verificar 200, lista vazia, 400/401/403/404/409, falha parcial e 503 para falha de todos os elegíveis, sem fallback gratuito.
- [x] 1.4 Demonstrar nova transportadora/modalidade por bean de teste sem alterar checkout/preço; verificar listagem e seleção inequívoca da nova opção.
- [x] 1.5 Documentar os contratos, tabela simulada, cobertura, erros e participantes Adapter em docs/03, 04, 05, 09 e Javadoc; conferir exemplos com os testes e preservar limites ArchUnit.

## 2. Preço e seleção no checkout

- [x] 2.1 Acrescentar opcaoEntrega obrigatória aos requests de prévia/conclusão e reutilizar a leitura validada da compra para cotação; verificar ausência 400, opção desconhecida/sem cobertura 409 e isolamento de endereço, sem alterar recursos em prévia.
- [x] 2.2 Alimentar ContextoPreco com frete cotado antes de promoções/cupons, mantendo Chain/Strategy existentes; verificar total 400−110−29+25−25=261, frete líquido derivado, cupom residual, exclusividade e benefício zero sem consumo.
- [x] 2.3 Acrescentar entrega e freteLiquido à confirmação e comparação completa; verificar 409 com prévia atualizada para mudança de custo, prazo, fornecedor/modalidade de mesmo preço, endereço e tarifa com frete grátis.
- [x] 2.4 Tratar perda de cobertura com opções atuais e sem escolha automática; verificar que nova opção exige nova prévia/confirmação e nenhuma tentativa rejeitada cria pedido/reserva/consumo.
- [x] 2.5 Migrar imediatamente os helpers de checkout/promoções/pagamento e expectativas de frete/total para a seleção explícita; verificar a regressão dos módulos afetados sem zerar tarifas artificialmente nem remover cenários anteriores.
- [x] 2.6 Atualizar OpenAPI e docs/04, 05, 07, 09 e 10 com Facade/Chain/Strategy reais, novo corpo HTTP e distinção entre resumo do carrinho e preço com entrega; verificar exemplos de cotação e reconfirmação executáveis.

## 3. Snapshot e persistência

- [x] 3.1 Acrescentar escolha de entrega imutável ao snapshot do pedido e resposta com frete líquido derivado; verificar invariantes e preservação após alterar endereço, preço, tarifa, prazo e fornecedor correntes.
- [x] 3.2 Criar V7 aditiva com colunas opcionais e restrição de preenchimento conjunto; verificar upgrade V6 com pedido pago/cobrança existentes intactos e opção ausente nos legados, sem dados fabricados.
- [x] 3.3 Integrar snapshot à transação do checkout preservando reserva/cupom/limpeza versionada; verificar rollback posterior a flush, última unidade e conclusão concorrente da mesma seleção com entrega.
- [x] 3.4 Atualizar docs/02, 03, 05 e 09 com dados e transações reais; conferir resposta e colunas contra migração e testes de imutabilidade.

## 4. Ciclo completo e demonstração

- [x] 4.1 Verificar compra cotada até pagamento, preparação, envio e entrega usando State existente e consulta do cliente; testar permissões/saltos inválidos e preservação de prazo, modalidade e valores em todo o fluxo.
- [x] 4.2 Verificar recusa, cancelamento e estorno do total com frete original, sem recotação; confirmar que estoque e uso de cupom de frete são devolvidos uma única vez e que falha recuperável do estorno mantém garantias E6.
- [x] 4.3 Atualizar pedido dev novo para opção explícita sem alterar pedidos existentes; verificar carga repetida idempotente e ausência fora de dev.
- [x] 4.4 Atualizar docs/06, 07, 08, 09, 10 e 12 mantendo atribuições, com roteiro completo das duas modalidades, frete grátis e entrega; conferir comandos documentados e remover descrições obsoletas de frete zero fixo.

## 5. Validação integrada

- [x] 5.1 Executar Maven verify completo com PostgreSQL e ArchUnit; registrar contagem e resultados reais, confirmar JAR gerado e preservar regressões E3–E6.
- [x] 5.2 Validar entrega-frete com OpenSpec strict e git diff --check; conferir que deltas preservam os cenários E5 e registrar evidências sem commit, PR ou arquivamento automático.

## Evidência de conclusão

Em 2026-10-22, `./mvnw -o clean verify`: BUILD SUCCESS, 584 testes, zero falhas/erros/ignorados e JAR gerado. Cobertura E7: 22 casos em EntregaDominioTests (9), EntregaIntegracaoTests (12) e MigracaoEntregaTests (1), incluindo duas APIs distintas, todas as UFs, extensão por novo bean, preço promocional, prazo/modalidade com mesmo custo, frete grátis com tarifa alterada, falta de cobertura e falha parcial/total, snapshot e migração V6→V7, pagamento/recusa/estorno e ciclo até ENTREGUE. Regressões anteriores continuam exercitando rollback, concorrência e carga dev com modalidade explícita. Deltas de checkout/preço preservam integralmente os cenários E5. OpenSpec strict e git diff --check aprovados; sem commit, PR ou arquivamento automático.
