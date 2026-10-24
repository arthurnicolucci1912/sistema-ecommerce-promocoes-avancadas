# Design

## Context

Ver proposal.md para motivação. TransicionarPedido já publica os eventos PedidoCriado, PedidoPago, PedidoPagamentoRecusado, PedidoEnviado e PedidoCancelado via PublicadorEventosSpring. Estoque, cupons e estorno já reagem sincronamente na transação comercial. CanalNotificacao e Notificacao existem como contratos, mas não têm implementação. ClienteRepository não consulta cliente por id; a resolução do destinatário exigirá uma porta de leitura explícita.

Os serviços GerenciarProdutos, GerenciarCombos, GerenciarCategorias, AjustarEstoque, GerenciarPromocoes, GerenciarCupons e GerenciarUsuariosAdministrativos concentram as operações administrativas. Pedido registra responsável no histórico, mas não existe trilha transversal. A segurança já restringe a ADMIN o restante de /api/admin/**.

## Goals / Non-Goals

**Goals:** separar efeitos comerciais atômicos, comunicação após commit e auditoria transacional; usar os contratos existentes; manter os cenários E3–E7.

**Non-Goals:** provedor real de e-mail, fila externa, garantia de entrega após queda do processo, retentativa automática, relatórios E9 ou auditoria de todas as leituras. Falhas de comunicação são registradas, sem promessa de entrega garantida.

## Decisions

### Design Patterns

| Pattern | Papel | Problema do enunciado | Classes participantes | Extensão facilitada |
|---|---|---|---|---|
| Observer | Publicador e ouvintes independentes | Eventos de pedido sem conhecer todos os interessados | TransicionarPedido, PublicadorEventosSpring, OuvinteDePedidosDoEstoque, OuvinteDePedidosDaNotificacao, OuvinteDeOperacoesAdministrativas | Novo interessado sem alterar pedido/pagamento |
| Strategy/Adapter de canal existente | Porta e canal simulado | Tecnologias de comunicação substituíveis | CanalNotificacao, CanalNotificacaoSimulado, EnviarNotificacoes | Novo bean de canal sem alterar canais existentes |

Alternativa descartada para Observer: chamar comunicação e auditoria diretamente no pedido, acoplando-o aos interessados. Alternativa descartada para canal: switch por nome da tecnologia, que exigiria editar o emissor a cada canal. Persistência, filtros, paginação e montagem de registros não usam pattern próprio.

### Comunicação somente após commit

OuvinteDePedidosDaNotificacao usa @TransactionalEventListener(AFTER_COMMIT), sem fallbackExecution. Normaliza os cinco eventos relevantes em pedido, cliente e assunto. EnviarNotificacoes resolve o destinatário por porta de consulta implementada na infra, em transação de leitura REQUIRES_NEW, evitando depender de entidades lazy ou do contexto transacional encerrado. O canal simulado registra envio sem expor endereço completo, dados pessoais ou conteúdo sensível.

Uma exceção de resolução é capturada na fronteira do ouvinte; exceções de envio são capturadas por canal. Logs estruturados incluem pedido, acontecimento, canal e categoria do erro, sem payload ou mensagem arbitrária do provedor. Os demais canais continuam. Não se captura Error. Sem canais registrados é falha de configuração na inicialização; nomes duplicados ou vazios também são rejeitados.

Manter PedidoPago como evento canônico da aprovação: o roadmap usa PagamentoConfirmado como nome conceitual, mas introduzir outro evento de aprovação produziria risco de duplicidade. Replays idênticos na E6 já não publicam novo evento; validar esse comportamento com o ouvinte real e confirmação concorrente. Estoque e cupons permanecem síncronos; movê-los para AFTER_COMMIT quebraria atomicidade.

### Auditoria explícita e transacional

OperacaoAdministrativa é um evento imutável em compartilhado.dominio, contendo apenas campos permitidos e snapshots de dados relevantes. Serviços administrativos o publicam após a mutação, ainda na transação. Um auxiliar de aplicação resolve o usuário autenticado pelo identificador usado na segurança; nenhuma entidade de domínio acessa SecurityContext. Não interpretar qualquer usuário autenticado como administrativo: cancelamento de cliente não produz operação administrativa; cargas dev/sistema sem ator administrativo não são atribuídas a usuário fictício.

OuvinteDeOperacoesAdministrativas persiste RegistroAuditoria sincronamente na mesma transação; falha de persistência aborta também a operação administrativa. Não usar REQUIRES_NEW ou captura silenciosa na auditoria. Eventos são explícitos, sem AOP genérico de parâmetros ou interceptação HTTP, para obter valores anteriores corretos e excluir senhas e tokens por construção. Uma chamada administrativa composta gera um registro por operação pública concluída, evitando duplicatas por chamadas internas.

Cobertura: criação/alteração/inativação de categorias, produtos, variações e combos; preço e componentes; ajustes manuais de estoque; criação/alteração/ativação de campanhas e cupons; criação/alteração de perfis/inativação de usuários administrativos; preparação/envio/entrega/cancelamento administrativo de pedidos. Leituras, replay de callback e movimentações automáticas de estoque não geram registros administrativos adicionais.

### Persistência e consulta

V8 cria auditoria_operacao com id, usuario_id, ocorrido_em, operacao, recurso_tipo, recurso_id e detalhes JSONB de campos permitidos. Índices por momento/id e usuário/operação/momento conforme os filtros. Não adicionar FK que permita apagar a trilha em cascata. Não criar dados históricos sintéticos. Entidade sem setters de edição e repositório de aplicação sem delete/update; detalhes são copiados ao construir registro.

GET /api/admin/auditoria usa ConsultarAuditoria com @PreAuthorize ADMIN e transação readOnly. A infra implementa filtros combináveis e paginação no banco; DTO retorna registros e total. Instantes UTC, intervalo inclusivo, ordenação ocorrido_em DESC, id DESC. Validar paginação, usuarioId positivo, formato e tamanho de operação (máximo 100), e intervalo. Operação desconhecida bem formada retorna lista vazia. OpenAPI documenta 200/400/401/403 e ausência de rotas de escrita.

## Risks / Trade-offs

- Queda entre commit e comunicação → limitação explícita do canal simulado; outbox durável seria uma change posterior.
- Falha após commit propagada ao HTTP pode induzir repetição → capturar falhas de preparação e envio no ouvinte, testar resultado comercial e resposta.
- Vazamento de credenciais na auditoria/log → campos permitidos explícitos e testes com senha/token sentinela.
- Auditoria depende da identidade correta → testes com usuários reais por JWT, operações de cliente e execução dev sem ator.
- Chamadas compostas duplicadas → publicar na fronteira de cada operação pública e testar contagem exata.

## Migration Plan

1. Adicionar V8 sem modificar V1–V7 e sem alterar pedidos/cobranças existentes.
2. Integrar canais e ouvintes; implantar com configuração do canal simulado.
3. Executar upgrade de banco V7 com dados existentes e conferir preservação, Flyway e regressões.
4. Reversão de aplicação pode manter V8 aditiva; não apagar trilha para reverter código.
