package br.com.fatec.ecommerce.pedido.dominio.estado;

import br.com.fatec.ecommerce.compartilhado.dominio.EventoDominio;
import br.com.fatec.ecommerce.pedido.dominio.SituacaoPedido;
import br.com.fatec.ecommerce.pedido.dominio.evento.PedidoEntregue;
import java.util.Optional;

/**
 * Recebido pelo cliente. Estado final: rejeita toda operacao. A chegada anuncia {@link PedidoEntregue}.
 *
 * <p>Pattern: State - papel: estado concreto (ENTREGUE).
 *
 * <p>Ver docs/04-design-patterns.md, secao 3.4 (State: ciclo de vida do pedido).
 */
public final class Entregue extends EstadoPedidoBase {

    @Override
    public SituacaoPedido situacao() {
        return SituacaoPedido.ENTREGUE;
    }

    @Override
    public Optional<EventoDominio> eventoDeChegada(DadosDaTransicao t) {
        return Optional.of(new PedidoEntregue(t.pedidoId(), t.clienteId(), t.de(), t.para(), t.ocorridoEm()));
    }
}
