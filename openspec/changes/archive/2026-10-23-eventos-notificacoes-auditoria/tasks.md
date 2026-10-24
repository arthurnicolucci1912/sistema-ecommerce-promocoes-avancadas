# Tasks

## 1. Comunicação por eventos e canais simulados

- [x] 1.1 Implementar resolução do destinatário por porta de leitura e canal simulado pelo contrato existente, validando registro de canais; verificar destinatário correto, ausência de dados sensíveis nos logs e rejeição de canais ausentes/duplicados/inválidos.
- [x] 1.2 Implementar Observer de criação, aprovação, recusa, envio e cancelamento após commit, com leitura independente e isolamento de exceções; testar os cinco acontecimentos, rollback sem envio, falha de resolução e falha de um canal sem bloquear os demais ou desfazer efeitos comerciais.
- [x] 1.3 Demonstrar novo canal e novo interessado por beans de teste sem alterar pedido/pagamento/canais existentes; verificar que ambos recebem o acontecimento e que regras ArchUnit preservam as dependências.
- [x] 1.4 Integrar notificações aos testes reais de confirmação de pagamento sequencial e concorrente; verificar uma única comunicação, baixa e transição, incluindo resposta de sucesso quando o canal falha e fluxo de recusa/cancelamento com cupom.
- [x] 1.5 Atualizar docs/03, 04, 05, 07 e 09 e Javadoc dos participantes com Observer/canal, classes reais, limite de entrega e transações; conferir cenários T-ID-03, T-EV-01 e T-EV-02 contra os testes executados.

## 2. Auditoria transacional das operações

- [x] 2.1 Criar evento imutável de operação administrativa, resolução do ator, modelo e persistência de auditoria com V8 aditiva; testar campos obrigatórios, cópia dos detalhes, ausência de credenciais e upgrade V7 com pedidos/cobranças existentes intactos.
- [x] 2.2 Implementar ouvinte síncrono de auditoria na transação original; verificar rollback da operação e trilha juntos em falha posterior e rollback da operação se a gravação da auditoria falhar, além de novo ouvinte registrado sem modificar publicadores.
- [x] 2.3 Publicar operações de categorias, produtos, variações e combos com valores anteriores/novos relevantes; testar criação, alterações, preço, componentes e inativação, rejeição sem registro e operações compostas sem duplicação.
- [x] 2.4 Publicar ajustes administrativos de estoque com quantidade e motivo; verificar ator, recurso e dados corretos, estoque negativo rejeitado sem registro e ausência de registros administrativos nas movimentações automáticas.
- [x] 2.5 Publicar criação/alteração/ativação de campanhas e cupons e gestão de usuários administrativos; verificar usuário correto, versão/configuração relevante, rollback por conflito e exclusão de senha/hash/token dos detalhes.
- [x] 2.6 Publicar preparação/envio/entrega/cancelamento administrativo de pedido; verificar estado anterior, motivo, identidade, nenhuma trilha administrativa para cancelamento de cliente ou replay de callback e execução dev sem ator fictício.
- [x] 2.7 Atualizar docs/02, 03, 04, 05, 07 e 09 e Javadoc com esquema, operações cobertas, Observer e fronteiras transacionais; conferir T-EV-03 contra testes reais e preservar limites ArchUnit incluindo auditoria na proteção do compartilhado.

## 3. Consulta administrativa e demonstração

- [x] 3.1 Implementar GET /api/admin/auditoria com consulta paginada no banco e filtros combinados; testar usuário/operação/período inclusivo, ordenação determinística, lista vazia, total e limites de paginação.
- [x] 3.2 Restringir consulta a ADMIN na aplicação e API e documentar OpenAPI; verificar 200/400/401/403, instantes malformados, intervalo invertido, identificador inválido e impossibilidade de edição/exclusão pela API.
- [x] 3.3 Atualizar docs/07, 08, 09, 10 e 12 preservando atribuições, com roteiro de compra notificada, falha de canal e consulta de alterações de preço/campanha/cancelamento; executar os exemplos documentados e registrar status real.

## 4. Validação integrada

- [x] 4.1 Executar Maven clean verify completo com PostgreSQL e ArchUnit; registrar contagem real, ausência de falhas/erros/ignorados e JAR, mantendo regressões E3–E7 de concorrência, rollback, pagamento, cupons e entrega.
- [x] 4.2 Executar OpenSpec strict e git diff --check; conferir coerência entre specs, design, tarefas, documentação e evidências, sem git add, commit, PR ou arquivamento automático.


## Evidência parcial — grupo 1

Em 2026-10-22, grupo 1 concluído: ouvinte após commit, consulta independente de destinatário, canal simulado e extensão por beans, isolamento de falhas, replay e concorrência de pagamento, rollback e devolução de cupom testados. Seleção focada: 36 testes aprovados. `./mvnw -o clean verify`: BUILD SUCCESS, 592 testes, zero falhas/erros/ignorados; JAR gerado. Docs/03, 04, 05, 07 e 09 e Javadoc atualizados. Auditoria e consulta permanecem pendentes. Pausa para revisão do autor ao fim do grupo, conforme orientação de apply em openspec/config.yaml; sem git add, commit, PR ou arquivamento.

## Evidência parcial — grupo 2

Em 2026-10-23, tarefas 2.1–2.7 concluídas: evento/snapshots imutáveis, resolução do ator por JWT administrativo, V8 aditiva, persistência síncrona na transação original e publicação explícita em catálogo, estoque manual, campanhas/cupons, usuários e transições administrativas de pedido. Testes demonstram rollback conjunto, falha de persistência abortando mutação, extensão por ouvinte, valores anterior/novo, operações compostas sem duplicação e exclusão de credenciais. Pagamento/replay sequencial e concorrente não produzem auditoria administrativa adicional; carga dev sem JWT de usuário não recebe ator fictício.

Seleção focada inicial: 26 testes aprovados. O primeiro verify completo revelou incompatibilidade com a autenticação temporária da carga dev; a resolução foi corrigida para reconhecer somente JWT administrativo. Validação final: `./mvnw -o clean verify`, BUILD SUCCESS, **607 testes, zero falhas/erros/ignorados**, JAR gerado em `target/ecommerce-0.0.1-SNAPSHOT.jar`; log `/tmp/bdr-auditoria-verify-final.log`. Inclui 15 novos testes de auditoria, asserções adicionais de dev/pagamento, migração V7→V8 e ArchUnit. OpenSpec strict e git diff --check aprovados. Docs/02, 03, 04, 05, 07 e 09 e Javadoc atualizados. Progresso **12/17**; consulta administrativa e validação final da change permanecem nos grupos 3 e 4. Pausa para revisão do autor ao fim do grupo conforme openspec/config.yaml; sem git add, commit, PR ou arquivamento automático.

## Evidência parcial — grupo 3

Em 2026-10-23, tarefas 3.1–3.3 concluídas: GET `/api/admin/auditoria` somente leitura, protegido por ADMIN no controller e na aplicação (também pela regra geral de segurança `/api/admin/**`), com filtros combinados por usuário/operação/período inclusivo, ordenação ocorrido_em DESC/id DESC e COUNT/LIMIT/OFFSET no PostgreSQL. Filtros e limites inválidos retornam 400 antes da consulta. Operação desconhecida e páginas além do fim retornam conteúdo vazio com total correto. OpenAPI documenta parâmetros e 200/400/401/403; não há rotas de escrita.

`ConsultarAuditoriaTests`: dez testes novos para ordenação/empates, filtros, intervalo/fuso, paginação de 1 a 100, offset grande, total, lista vazia, validação, 401/403 de todos os demais perfis, proteção na aplicação e ausência de mutação. Novo `PagamentoIntegracaoTests.roteiroDeCompraNotificadaEConsultaDaTrilha` exercita o fluxo HTTP documentado de alteração de preço, criação de campanha, checkout, pagamento PIX notificado, cancelamento pago e consultas da trilha com filtros. O cenário já existente de falha de canal também foi executado. Corpos/rotas dos exemplos curl foram exercitados por MockMvc com JWTs reais e PostgreSQL; os curl permanecem instruções para sessão manual dev com ids locais.

Comando documentado e executado: `./mvnw -o -Dtest=ConsultarAuditoriaTests,AuditoriaIntegracaoTests,MigracaoAuditoriaTests,OperacaoAdministrativaTests,PagamentoIntegracaoTests,NotificacoesIntegracaoTests,EnviarNotificacoesTests,RegrasDeArquiteturaTests test`: **BUILD SUCCESS, 62 testes, zero falhas/erros/ignorados**, incluindo ArchUnit. Log `/tmp/bdr-auditoria-grupo3-tests.log`. OpenSpec strict e git diff --check aprovados. Docs/07, 08, 09, 10 e 12 atualizados, preservando atribuições originais de Mateus e Amanda. Progresso **15/17**; grupo 4 (clean verify completo, JAR atualizado e conferência final de coerência) permanece pendente. Pausa para revisão ao fim do grupo conforme openspec/config.yaml; sem git add, commit, PR ou arquivamento.

## Evidência final — grupo 4

Em 2026-10-23, `./mvnw -o clean verify` com PostgreSQL (Testcontainers) e ArchUnit: **BUILD SUCCESS, 618 testes, zero falhas/erros/ignorados**, JAR `target/ecommerce-0.0.1-SNAPSHOT.jar` gerado, mantendo as regressões E3–E7 (concorrência, rollback, pagamento, cupons e entrega). `openspec validate eventos-notificacoes-auditoria --strict` e `git diff --check` aprovados. Progresso **17/17**. Implementado por Arthur (sessão assistida); atribuição original: Mateus (eventos/notificações) e Amanda (auditoria).
