package br.com.fatec.ecommerce.notificacao.aplicacao;

import br.com.fatec.ecommerce.pedido.dominio.evento.PedidoCancelado;
import br.com.fatec.ecommerce.pedido.dominio.evento.PedidoCriado;
import br.com.fatec.ecommerce.pedido.dominio.evento.PedidoEnviado;
import br.com.fatec.ecommerce.pedido.dominio.evento.PedidoPagamentoRecusado;
import br.com.fatec.ecommerce.pedido.dominio.evento.PedidoPago;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Observer do pedido: comunica apenas depois do commit, sem modificar pedido ou pagamento.
 * Sem fallback para eventos fora de transacao. Falha de preparacao nao escapa ao chamador comercial.
 * Ver docs/04-design-patterns.md, secao 3.6.
 */
@Component
public class OuvinteDePedidosDaNotificacao {
    private static final Logger LOG = LoggerFactory.getLogger(OuvinteDePedidosDaNotificacao.class);
    private final EnviarNotificacoes emissor;

    public OuvinteDePedidosDaNotificacao(EnviarNotificacoes emissor) {
        this.emissor = emissor;
    }

    @TransactionalEventListener
    public void ao(PedidoCriado evento) {
        comunicar(evento.pedidoId(), evento.clienteId(), "AGUARDANDO_PAGAMENTO");
    }

    @TransactionalEventListener
    public void ao(PedidoPago evento) {
        comunicar(evento.pedidoId(), evento.clienteId(), "PAGO");
    }

    @TransactionalEventListener
    public void ao(PedidoPagamentoRecusado evento) {
        comunicar(evento.pedidoId(), evento.clienteId(), "PAGAMENTO_RECUSADO");
    }

    @TransactionalEventListener
    public void ao(PedidoEnviado evento) {
        comunicar(evento.pedidoId(), evento.clienteId(), "ENVIADO");
    }

    @TransactionalEventListener
    public void ao(PedidoCancelado evento) {
        comunicar(evento.pedidoId(), evento.clienteId(), "CANCELADO");
    }

    private void comunicar(Long pedidoId, Long clienteId, String acontecimento) {
        try {
            emissor.enviar(pedidoId, clienteId, acontecimento);
        } catch (RuntimeException erro) {
            LOG.warn("Falha de preparacao da notificacao pedido={} acontecimento={} categoria={}",
                    pedidoId, acontecimento, erro.getClass().getSimpleName());
        }
    }
}
