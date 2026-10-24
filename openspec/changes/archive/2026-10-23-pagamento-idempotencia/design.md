# Design

## Context
Ver proposal.md. Existem contratos GatewayPagamento e DTOs desde E1, mas nenhum fornecedor concreto. TransicionarPedido publica eventos sincronamente na transação. Estoque reage antes de cupons (@Order 100/200). A E5 está implementada com 540 testes; suas deltas permanecem ativas e não serão sobrescritas. A simulação temporária tem consumidores em testes de pedido, checkout e promoções.

## Goals / Non-Goals
Garantir consistência transacional verificável no PostgreSQL e integração extensível. Sem dinheiro real, PAN, fila externa, transportadora ou mudança dos totais. Expiração automática permanece pendência separada: não inventar prazo comercial neste incremento.

## Decisions

### APIs e segurança
POST /api/clientes/me/pedidos/{id}/pagamento aceita apenas {forma}; GET no mesmo caminho consulta cobrança e estorno. CLIENTE é resolvido pelo JWT e propriedade validada antes de retornar qualquer dado, inclusive nas repetições. Primeira criação retorna 201 com Location; repetição retorna 200; consulta sem cobrança 404. ADMIN exclusivo para POST /api/admin/pagamentos/confirmacoes {idTransacaoExterna,situacao}, situacao apenas APROVADO/RECUSADO, resposta 200 inclusive replay; e POST /api/admin/pagamentos/{pedidoId}/estorno/tentativas, resposta 200 com situação. Não é webhook público: credencial ADMIN representa a integração acadêmica simulada. Alternativa de callback anônimo descartada porque permitiria fraude por identificador conhecido. Não permitir que cliente selecione aprovação/recusa.

### Fornecedores e cobrança
Implementar AdaptadorCartaoSimulado, AdaptadorPixSimulado e AdaptadorBoletoSimulado em pagamento.infra; adaptees têm APIs deliberadamente diferentes (autorização em centavos, geração de cobrança PIX, emissão de boleto), incluindo tradução de resultado/estorno. Configuração do servidor controla cenários de recusa/falha; padrão cartão aprovado, PIX/boleto pendentes. Preservar contratos existentes salvo ampliação mínima documentada. RegistroGateways resolve seleção por forma e identidade estável do fornecedor; validar seleção ausente/ambígua no startup. Persistir fornecedor para estorno após troca. Adaptees geram IDs prefixados globalmente únicos e reutilizam chave estável por pedido para cobrar e por cobrança para estornar. Nenhum if/switch de fornecedor no núcleo.

### Persistência, ordem de locks e atomicidade
V6 cria cobranca (pedido_id único, fornecedor, forma, total, id externo único, situação, datas), confirmacao_pagamento (id externo único, resultado, data) e estorno (cobrança única, situação PENDENTE/FALHOU/CONCLUIDO, id externo, tentativas, último erro). Relações entre módulos são IDs, sem associação JPA. Repositórios no domínio, implementação infra. Total é Dinheiro do snapshot, Clock injetado.
Serializar operações pelo pedido com SELECT FOR UPDATE escalar antes de materializar o agregado, cobrança, confirmação, cancelamento e estorno; destacar raiz carregada previamente e recarregar sob lock (refresh com coleções SUBSELECT falha nesta versão do Hibernate); a aplicação de cancelamento deve participar da mesma disciplina. Ordem global: pedido -> cobrança/estorno -> estoque ordenado -> cupom, preservando @Order dos ouvintes. Evitar travar cobrança antes do pedido, inclusive no listener. Ao confirmar, buscar identidade inicialmente sem lock, travar pedido e recarregar cobrança; gravar confirmação por INSERT ON CONFLICT DO NOTHING e comparar conteúdo persistido. Evitar capturar violação de unicidade JPA dentro de transação marcada rollback-only. Replays idênticos são reconhecidos antes de validar estado atual do pedido, portanto continuam 200 após cancelamento posterior. Resultado divergente 409. Cobrança única e lock impedem duas chamadas ao fornecedor em início concorrente.
Transição usa TransicionarPedido com Responsavel.sistema e motivo rastreável; efeitos permanecem nos ouvintes existentes. Falha em qualquer efeito reverte confirmação e estado. Simulação síncrona usa o mesmo caso de uso de confirmação, na mesma transação. Não duplicar eventos de pagamento sem consumidor: PedidoPago/PedidoPagamentoRecusado já oferecem a integração necessária.

### Cancelamento, estorno e divergência documental
OuvinteDePedidosDoPagamento reage após estoque/cupons (@Order 300). Para origem PAGO/EM_PREPARACAO, usa cobrança aprovada original e solicita estorno total idempotente. Recusa/exceção do fornecedor simulado é convertida em resultado FALHOU persistido sem abortar cancelamento; erros de persistência não são engolidos. Retentativa administrativa trava pedido, nunca repete transição nem devoluções. Sucesso anterior é no-op; inexistência de cobrança aprovada retorna conflito na retentativa. Pedidos legados pagos pelo atalho sem cobrança não recebem estorno fictício: registrar pendência rastreável de ausência de transação e retornar 409 na tentativa até conciliação manual documentada, sem bloquear cancelamento.
RN-24/25 diz rejeitar transição após cancelamento; UC-S01 E2 sugere registrar confirmação nesse caso. Adotar RN-24/25: 409 e nenhuma confirmação aceita, registrar diagnóstico em log. Reconciliar os textos na implementação. Como só há simuladores, não há valor financeiro externo a compensar; um gateway real exigiria conciliação própria.

### Design Patterns
| Pattern | Papel | Problema do enunciado | Classes participantes | Modificação facilitada |
|---|---|---|---|---|
| Adapter | Target/adapters/adaptees | Interfaces distintas de pagamento | GatewayPagamento, AdaptadorCartaoSimulado, AdaptadorPixSimulado, AdaptadorBoletoSimulado e provedores simulados | Substituir fornecedor sem modificar casos de uso |
| Observer | publicador e ouvinte | Reagir ao cancelamento sem acoplar pedido ao gateway | TransicionarPedido, PublicadorEventos, OuvinteDePedidosDoPagamento | Adicionar reação de estorno |
| State | contexto e estados existentes | Transições válidas dependem do estado | Pedido, EstadoPedido e estados existentes | Reutilizar restrições de pagamento/cancelamento |
Adapter substitui switch de fornecedores, que acoplaria o núcleo a APIs concretas. Observer substitui chamada de gateway no domínio de pedido, que inverteria dependências. State existente substitui duplicação de regras em pagamento. Registro, DTOs, SQL de idempotência e migração não recebem pattern próprio. Documentar participantes reais e papéis no Javadoc e em docs/04.

## Risks / Trade-offs
- Chamada externa e transação SQL não são atomicamente coordenáveis → apenas simuladores determinísticos com chave idempotente; integração real exige outbox/conciliação e não é prometida.
- Deadlock com cancelamento → mesma ordem de locks em todas as entradas e testes concorrentes reais com timeout.
- Pedidos antigos sem cobrança → preservar dados e explicitar pendência sem inventar transação externa.
- Remoção de rota quebra roteiros existentes → migrar testes e exemplos na mesma entrega.
- Pendências de estorno exigem ação administrativa → expor situação e roteiro de retentativa, sem prometer agendamento.

## Migration Plan
Migração V6 aditiva, sem alterar snapshots nem inserir cobranças artificiais para pedidos antigos. Testar upgrade V5->V6 com dados. Atualizar limpeza dos fixtures na ordem de dependências. Remover controller/request temporários somente após migrar consumidores. Rollback operacional exige versão compatível com novas tabelas; não apagar histórico financeiro nem aplicar down migration destrutiva. Atualizar documentação de execução e board, preservar autoria e changes anteriores sem commits/arquivamento.
