package br.com.fatec.ecommerce.pedido.dominio.estado;

import br.com.fatec.ecommerce.compartilhado.dominio.EventoDominio;
import br.com.fatec.ecommerce.pedido.dominio.Responsavel;
import br.com.fatec.ecommerce.pedido.dominio.SituacaoPedido;
import br.com.fatec.ecommerce.pedido.dominio.evento.PedidoPago;
import java.util.Optional;

/**
 * Pagamento aprovado. Aceita iniciar a preparacao (T4) e cancelamento (T10), este so por responsavel
 * administrativo (politica de cancelamento por perfil). A chegada anuncia {@link PedidoPago}.
 *
 * <p>Pattern: State - papel: estado concreto (PAGO).
 *
 * <p>Ver docs/04-design-patterns.md, secao 3.4 (State: ciclo de vida do pedido).
 */
public final class Pago extends EstadoPedidoBase {

    @Override
    public SituacaoPedido situacao() {
        return SituacaoPedido.PAGO;
    }

    @Override
    public EstadoPedido iniciarPreparacao() {
        return SituacaoPedido.EM_PREPARACAO.estado();
    }

    @Override
    public EstadoPedido cancelar(Responsavel responsavel) {
        exigirResponsavelAdministrativo(responsavel);
        return SituacaoPedido.CANCELADO.estado();
    }

    @Override
    public Optional<EventoDominio> eventoDeChegada(DadosDaTransicao t) {
        return Optional.of(new PedidoPago(t.pedidoId(), t.clienteId(), t.de(), t.para(), t.ocorridoEm()));
    }
}
