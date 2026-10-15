package br.com.fatec.ecommerce.pedido.dominio.estado;

import br.com.fatec.ecommerce.compartilhado.dominio.EventoDominio;
import br.com.fatec.ecommerce.pedido.dominio.SituacaoPedido;
import br.com.fatec.ecommerce.pedido.dominio.evento.PedidoCancelado;
import java.util.Optional;

/**
 * Pedido cancelado, com historico. Estado final: rejeita toda operacao. A chegada anuncia {@link
 * PedidoCancelado}, com responsavel e motivo.
 *
 * <p>Pattern: State - papel: estado concreto (CANCELADO).
 *
 * <p>Ver docs/04-design-patterns.md, secao 3.4 (State: ciclo de vida do pedido).
 */
public final class Cancelado extends EstadoPedidoBase {

    @Override
    public SituacaoPedido situacao() {
        return SituacaoPedido.CANCELADO;
    }

    @Override
    public Optional<EventoDominio> eventoDeChegada(DadosDaTransicao t) {
        return Optional.of(new PedidoCancelado(t.pedidoId(), t.clienteId(), t.de(), t.para(), t.ocorridoEm(),
                t.responsavel(), t.motivo()));
    }
}
