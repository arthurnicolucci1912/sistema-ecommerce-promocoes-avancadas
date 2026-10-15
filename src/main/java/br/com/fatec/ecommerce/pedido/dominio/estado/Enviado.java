package br.com.fatec.ecommerce.pedido.dominio.estado;

import br.com.fatec.ecommerce.compartilhado.dominio.EventoDominio;
import br.com.fatec.ecommerce.pedido.dominio.SituacaoPedido;
import br.com.fatec.ecommerce.pedido.dominio.evento.PedidoEnviado;
import java.util.Optional;

/**
 * Entregue a transportadora. So admite a entrega (T6); nao pode ser cancelado. A chegada anuncia {@link
 * PedidoEnviado}.
 *
 * <p>Pattern: State - papel: estado concreto (ENVIADO).
 *
 * <p>Ver docs/04-design-patterns.md, secao 3.4 (State: ciclo de vida do pedido).
 */
public final class Enviado extends EstadoPedidoBase {

    @Override
    public SituacaoPedido situacao() {
        return SituacaoPedido.ENVIADO;
    }

    @Override
    public EstadoPedido entregar() {
        return SituacaoPedido.ENTREGUE.estado();
    }

    @Override
    public Optional<EventoDominio> eventoDeChegada(DadosDaTransicao t) {
        return Optional.of(new PedidoEnviado(t.pedidoId(), t.clienteId(), t.de(), t.para(), t.ocorridoEm()));
    }
}
