# Tasks

## 1. Contratos, contexto e condições

- [x] 1.1 Evoluir `ContextoPreco`, `DescontoCandidato` e `DescontoAplicado` com cupom/perfis/frete bruto, data de criação e origem/identidade, preservando construtores e acessos legados; verificar testes de compatibilidade, imutabilidade e ausência de dependência do domínio de preço/promoção para pedido ou infraestrutura.
- [x] 1.2 Montar o contexto com Clock, identidade/perfis autenticados, endereço principal no carrinho e escolhido no checkout, e consulta ao histórico de pagamentos aprovados pela porta de pedido; verificar primeira compra para cliente sem pedidos, só recusados/cancelados sem pagamento, pedido pago avançado/cancelado e tentativa de falsificar contexto no corpo HTTP.
- [x] 1.3 Implementar folhas e compostos E/OU/NÃO de `CondicaoElegibilidade`, árvore configurável e registro de criadores com limites de profundidade/tamanho; verificar tabela de verdade, categorias ancestrais, SKU de kit, região desconhecida, fronteiras de período e nova folha registrada sem modificar os compostos.
- [x] 1.4 Atualizar docs/02-modelo-dominio.md, docs/03-arquitetura.md, docs/04-design-patterns.md e RN-01/04 em docs/05-regras-negocio.md com contratos/condições reais e primeira compra por pagamento, além de rastreabilidade T-PR em docs/09-testes.md; verificar correspondência com os testes e Javadoc dos participantes do Composite.

## 2. Estratégias, fábricas e política de conflitos

- [x] 2.1 Implementar as cinco estratégias de promoção e suas fábricas/registro: leve X/pague Y, percentual por categoria, valor fixo acima de mínimo, percentual de primeira compra e frete grátis; verificar parâmetros inválidos, SKU/categoria, grupos completos de quantidade, preço próprio de kit, vigência, HALF_EVEN e cotação de frete simulada.
- [x] 2.2 Demonstrar materialização por configuração de um sexto tipo de promoção e uma condição nova com seus criadores registrados, sem alterar motor, CRUD ou estratégias existentes; verificar testes de cadastro/cálculo pelo registro, metadados de tipos e rejeição de registros duplicados/desconhecidos.
- [x] 2.3 Implementar `PoliticaPadrao` D4 com desempates por desconto, prioridade, criação e id, e teto por alvo preservando descontos de maior prioridade; verificar cenários A–D da RN-02, ordem independente da consulta, teto da RN-03, exclusividade e remoção de valores efetivos zero. Demonstrar `PoliticaSomenteMaiorDesconto` substituída sem alterar benefícios/motor.
- [x] 2.4 Implementar benefícios e registro de cupom para percentual, fixo, frete grátis e redução fixa de frete; verificar percentual sobre saldo após promoções, teto de fixo/frete, base zero, incompatibilidade com exclusiva/não acumulável e novo benefício de teste sem alterar os existentes.
- [x] 2.5 Atualizar docs/04-design-patterns.md, RN-02/03/08/09/14/16 em docs/05-regras-negocio.md e T-PR/T-CP em docs/09-testes.md, com Javadoc dos participantes reais de Strategy/Factory Method; verificar que os exemplos numéricos e classes documentadas correspondem aos testes de extensão e conflito.

## 3. Persistência e administração

- [x] 3.1 Criar V5 aditiva para campanhas, cupons, saldos/usos, cupom no carrinho e referências opcionais em descontos do pedido; implementar entidades/portas/adaptadores JPA e limpeza de testes. Verificar migração do zero e V4→V5 com pedidos/carrinhos existentes, unicidade de código normalizado/uso por pedido, restrições e persistência de parâmetros/árvores entre sessões, sem editar V1–V4.
- [x] 3.2 Implementar serviços administrativos de campanhas e cupons, validação de configurações pelos registros e proteção de versão/limites; verificar criação/edição/ativação/desativação, período UTC [início,fim), código duplicado, alvo inexistente, versão obsoleta e tentativa de reduzir limite abaixo dos usos ativos sem gravação parcial.
- [x] 3.3 Expor CRUD/listagens paginadas e catálogos de tipos/condições em `/api/admin/promocoes` e `/api/admin/cupons`, exigindo ADMIN/PROMOCOES na rota e aplicação; verificar MockMvc/OpenAPI, 401/403 dos demais perfis, 400/404/409, paginação e ausência de catálogo de tipos duplicado no controller.
- [x] 3.4 Atualizar docs/02-modelo-dominio.md, docs/03-arquitetura.md, UC-A05/A06 em docs/07-casos-de-uso.md, exemplos em docs/10-execucao.md e rastreabilidade em docs/09-testes.md; verificar corpos/rotas contra a API e documentar auditoria completa como E8, sem declarar integração inexistente.

## 4. Pipeline e cupom no carrinho

- [x] 4.1 Implementar fontes de domínio/adaptadores, `MotorDePromocoes`, `EtapaPromocoes`, `EtapaCupom` e `EtapaFreteBruto` na cadeia explícita; verificar subtotal→promoção→cupom→frete→total com cenário 400−110−29=261, frete bruto simulado, limites por alvo, ausência de dupla contagem e independência de cálculos. Demonstrar novo handler de teste sem alterar os existentes.
- [x] 4.2 Persistir seleção de um cupom no agregado do carrinho com revisão/versão e adicionar PUT/DELETE `/api/clientes/me/carrinho/cupom`; verificar aplicação, substituição válida, substituição rejeitada preservando o anterior, normalização, remoção idempotente, retomada em nova sessão, 401/403 e isolamento entre clientes sem consumo de uso/reserva.
- [x] 4.3 Incluir origem/identidade/código nos descontos do resumo e problema do cupom selecionado, mantendo leitura e CRUD de itens quando ele perde validade; verificar inexistente/inativo/fora da vigência/esgotado/limite individual/incompatível, redução que elimina elegibilidade, total sem benefício inválido, ausência de remoção silenciosa e quantidade ainda preservada.
- [x] 4.4 Ajustar ArchUnit somente para permitir implementações de negócio dos contratos de promoções/condições/política em `promocao.dominio`, mantendo as fronteiras das demais integrações e dos handlers de preço; verificar a suíte arquitetural e correspondência de Javadoc/docs/04 com as classes efetivamente compostas.
- [x] 4.5 Atualizar UC-05/06, diagrama do pipeline, RN-05 a RN-14, docs/03/04/09/10 e schemas OpenAPI com seleção/erros/descontos, indicando frete de produção zero até E7; verificar exemplos reproduzíveis e que GET/prévia/aplicação nunca consomem uso de cupom.

## 5. Checkout, consumo e devolução

- [x] 5.1 Evoluir confirmação/prévia e comparação canônica com origem, identidade e código de benefício/cupom, preservando solicitações sem descontos; mapear o cálculo para snapshot de pedido com referências opcionais e total de descontos real. Verificar campanha expirada/alterada, troca de cupom com mesmo valor, endereço/região e primeiro pagamento aprovado após prévia, reconfirmação e snapshot imutável após edição administrativa.
- [x] 5.2 Bloquear conclusão com cupom selecionado inválido, retornando motivo e prévia atualizada sem efeitos; verificar que confirmar novamente um resumo sem benefício não contorna a rejeição, e que remover/substituir o cupom permite nova compra confirmada.
- [x] 5.3 Implementar consumo por pedido na transação do checkout com trava/releitura do cupom, versão de configuração, saldo total, limite individual e unicidade de uso; verificar que só desconto efetivo consome, consumo repetido não duplica, configuração concorrente não é ignorada e falhas de estoque/confirmação/limpeza após flush desfazem pedido, reservas, uso e saldo.
- [x] 5.4 Verificar concorrência em PostgreSQL com threads/transações independentes: última utilização global entre clientes, limite individual disputado diretamente pelo serviço de consumo em pedidos distintos, edição/desativação concorrente e duas conclusões da mesma seleção; exigir contadores corretos, no máximo o limite de usos, 409 das tentativas rejeitadas e preservação do carrinho/estoque da perdedora.
- [x] 5.5 Implementar devolução idempotente por `OuvinteDePedidosDoCupom` em recusa/cancelamento, com ordem explícita de estoque antes de cupom nos ouvintes e no checkout; verificar recusa seguida de cancelamento, cancelamento de pedido pago, evento repetido, pedido sem cupom, rollback conjunto e corrida entre devolução e novo consumo sem contadores negativos ou espera circular. Demonstrar novo ouvinte de teste sem mudar o domínio do pedido.
- [x] 5.6 Atualizar docs/02–05, UC-07/10 e diagramas de promoções/checkout em docs/07, T-PR/T-CP em docs/09 e roteiro em docs/10; verificar fluxo autenticado com campanha + cupom → reconfirmação → pedido com origens → recusa/cancelamento → vaga devolvida. Documentar limites individuais, primeira compra por histórico e compatibilidade da confirmação antiga sem desconto.

## 6. Integração e entrega

- [x] 6.1 Atualizar demonstração `dev` com campanhas/cupons identificáveis sem duplicar configuração nem restaurar contadores em reinícios; verificar execução repetida, ausência de carga fora de dev, cenários determinísticos com Clock e roteiro que demonstre desconto, conflito, cupom inválido e liberação de uso.
- [x] 6.2 Executar `./mvnw -o verify` com PostgreSQL/Testcontainers e regressão completa, incluindo migração incremental, contratos legados, autorização, extensão por tipo/condição/política/benefício/handler, rollback e concorrência; verificar BUILD SUCCESS, testes sem falhas e JAR gerado, registrando a contagem real.
- [x] 6.3 Atualizar docs/08-roadmap.md e docs/12-board.md com situação/evidências da E5, preservando Amanda como atribuição original e revisão conjunta pendente da onda 0; verificar `openspec validate promocoes-cupons --strict`, coerência com specs principais e entrega para revisão, sem git add, commit ou arquivamento automático.


## Evidências da implementação

- 2026-10-22: `./mvnw -o verify` — BUILD SUCCESS, 540 testes, sem falhas/erros/ignorados; JAR `target/ecommerce-0.0.1-SNAPSHOT.jar` gerado.
- E5: 12 casos de domínio, 18 de integração HTTP/concorrência, 1 de migração V4→V5 preservando dados e 1 de demonstração idempotente, além de regra ArchUnit adicional. Regressão dos módulos existentes aprovada.
- Extensão verificada por sexto tipo e nova folha registrados pela configuração, política alternativa, novo benefício de cupom e handler de teste. Primeira compra por histórico de pagamento confirmado, sem flags fornecidas pelo consumidor.
- PostgreSQL/Testcontainers: última utilização global, limite individual, mesma seleção, desativação durante compra, devolução concorrente, versão administrativa, limites, rollback após consumo/confirmação/flush da limpeza e snapshot imutável.
- `openspec validate promocoes-cupons --strict` e `git diff --check` aprovados. Documentação e board atualizados; autoria original de Amanda e revisão conjunta da onda 0 preservadas. Entrega pronta para revisão local, sem git add, commit, PR ou arquivamento automático.
- Integrações de gateway, cotação/modalidades de entrega, auditoria e relatórios continuam nas respectivas E6–E9; benefícios de frete já implementados com cotação simulada e zero na produção da E5.
