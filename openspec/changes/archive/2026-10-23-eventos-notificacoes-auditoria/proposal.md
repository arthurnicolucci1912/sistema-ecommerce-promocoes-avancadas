# Proposal

## Why

A E7 encerrou o fluxo comercial com entrega, mas o cliente ainda não recebe notificações e as operações administrativas não têm trilha consultável. A E8 completa esses requisitos preservando a atomicidade e a idempotência de pagamento, estoque e cupons.

## What Changes

- Notificar criação, aprovação, recusa, envio e cancelamento por canais simulados, somente após commit; registrar falhas sem afetar os efeitos comerciais.
- Registrar operações administrativas bem-sucedidas de catálogo, estoque, campanhas, cupons, usuários e pedidos, com responsável, momento, recurso e alterações relevantes.
- Disponibilizar consulta paginada e somente leitura de auditoria para ADMIN, filtrada por usuário, operação e período.
- Demonstrar extensão por novo canal e novo ouvinte sem alterar pedido ou pagamento.
- Atualizar documentação, Javadoc, roteiro de execução e testes de regressão.

## Capabilities

### New Capabilities

- `notificacoes`: comunicação simulada após commit, isolamento de falhas e extensão de canais.
- `auditoria`: trilha transacional administrativa imutável e consulta restrita.

### Modified Capabilities

Nenhuma. Os eventos de pedido existentes já permitem novos interessados; a E8 os consome sem alterar as transições especificadas.

## Design Patterns

Observer atende aos requisitos de eventos e notificações do enunciado: ouvintes independentes de estoque, comunicação e auditoria reagem sem uma classe central conhecer todos os interessados. O contrato existente de canal permite variar a tecnologia de envio; não será introduzida fábrica ou hierarquia artificial para auditoria.

## Impact

Módulos notificacao e auditoria; publicação de operações nos serviços administrativos; migração V8 aditiva; endpoint GET /api/admin/auditoria; configuração de segurança, OpenAPI e docs/03, 04, 05, 07, 08, 09, 10 e 12. Sem dependência de provedor real de comunicação. Efeitos de estoque/cupons/estorno já usam ouvintes na E6 e serão preservados, evitando refatoração sem necessidade.
