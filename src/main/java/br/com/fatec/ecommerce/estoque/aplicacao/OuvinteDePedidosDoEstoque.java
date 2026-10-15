package br.com.fatec.ecommerce.estoque.aplicacao;

import br.com.fatec.ecommerce.pedido.dominio.SituacaoPedido;
import br.com.fatec.ecommerce.pedido.dominio.evento.PedidoCancelado;
import br.com.fatec.ecommerce.pedido.dominio.evento.PedidoPagamentoRecusado;
import br.com.fatec.ecommerce.pedido.dominio.evento.PedidoPago;
import java.util.Map;
import java.util.function.Consumer;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Mantem o estoque coerente com o ciclo de vida do pedido sem que o modulo de pedidos o conheca: pagamento
 * aprovado baixa a reserva, pagamento recusado a libera e o cancelamento libera (a partir de
 * AGUARDANDO_PAGAMENTO) ou repoe a baixa (a partir de PAGO ou EM_PREPARACAO). A acao do cancelamento e escolhida por
 * um mapa da situacao de origem ({@code de}) para a acao, sem {@code switch}; origens fora do mapa nao tem efeito.
 * Os ouvintes sao sincronos (na transacao de quem publicou), para pedido e estoque nunca divergirem, e idempotentes.
 *
 * <p>Pattern: Observer - papel: ouvinte (observer) de {@link PedidoPago}, {@link PedidoPagamentoRecusado} e
 * {@link PedidoCancelado}; um novo efeito de estoque e um novo metodo ouvinte, sem tocar o pedido.
 *
 * <p>Ver docs/04-design-patterns.md, secao 3.6 (Observer: eventos de dominio).
 */
@Component
public class OuvinteDePedidosDoEstoque {

    private final MovimentarReservas reservas;
    private final Map<SituacaoPedido, Consumer<Long>> acaoDoCancelamentoPorOrigem;

    public OuvinteDePedidosDoEstoque(MovimentarReservas reservas) {
        this.reservas = reservas;
        this.acaoDoCancelamentoPorOrigem = Map.of(
                SituacaoPedido.AGUARDANDO_PAGAMENTO, reservas::liberar,
                SituacaoPedido.PAGO, reservas::repor,
                SituacaoPedido.EM_PREPARACAO, reservas::repor);
    }

    @EventListener
    public void ao(PedidoPago evento) {
        reservas.baixar(evento.pedidoId());
    }

    @EventListener
    public void ao(PedidoPagamentoRecusado evento) {
        reservas.liberar(evento.pedidoId());
    }

    @EventListener
    public void ao(PedidoCancelado evento) {
        acaoDoCancelamentoPorOrigem.getOrDefault(evento.de(), pedidoId -> { }).accept(evento.pedidoId());
    }
}
