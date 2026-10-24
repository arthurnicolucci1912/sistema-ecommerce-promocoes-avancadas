package br.com.fatec.ecommerce.promocao.aplicacao;

import br.com.fatec.ecommerce.pedido.dominio.evento.*;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Observer: devolve uso por recusa/cancelamento sem acoplar o pedido a cupom; estoque adquire locks antes. */
@Component
public class OuvinteDePedidosDoCupom {
    private final ConfirmarUsoCupom usos;
    public OuvinteDePedidosDoCupom(ConfirmarUsoCupom usos) { this.usos=usos; }
    @EventListener @Order(200) public void ao(PedidoPagamentoRecusado evento) { usos.devolver(evento.pedidoId()); }
    @EventListener @Order(200) public void ao(PedidoCancelado evento) { usos.devolver(evento.pedidoId()); }
}
