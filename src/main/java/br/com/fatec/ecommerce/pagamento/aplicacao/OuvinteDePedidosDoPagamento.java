package br.com.fatec.ecommerce.pagamento.aplicacao;

import br.com.fatec.ecommerce.pedido.dominio.SituacaoPedido;
import br.com.fatec.ecommerce.pedido.dominio.evento.PedidoCancelado;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Pattern Observer, papel ConcreteObserver: estorna cancelamentos pagos sem acoplar pedido ao gateway. */
@Component
public class OuvinteDePedidosDoPagamento {
    private final EstornarPagamento estornar;
    public OuvinteDePedidosDoPagamento(EstornarPagamento estornar) { this.estornar = estornar; }
    @EventListener @Order(300)
    public void aoCancelar(PedidoCancelado evento) {
        if (evento.de() == SituacaoPedido.PAGO || evento.de() == SituacaoPedido.EM_PREPARACAO)
            estornar.executar(evento.pedidoId(), false);
    }
}
