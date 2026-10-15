package br.com.fatec.ecommerce.pedido.dominio.estado;

import br.com.fatec.ecommerce.compartilhado.dominio.EventoDominio;
import br.com.fatec.ecommerce.pedido.dominio.Responsavel;
import br.com.fatec.ecommerce.pedido.dominio.SituacaoPedido;
import br.com.fatec.ecommerce.pedido.dominio.evento.PedidoEmPreparacao;
import java.util.Optional;

/**
 * Separacao e embalagem. Aceita o envio (T5) e cancelamento (T11), este so por responsavel administrativo.
 * A chegada anuncia {@link PedidoEmPreparacao}.
 *
 * <p>Pattern: State - papel: estado concreto (EM_PREPARACAO).
 *
 * <p>Ver docs/04-design-patterns.md, secao 3.4 (State: ciclo de vida do pedido).
 */
public final class EmPreparacao extends EstadoPedidoBase {

    @Override
    public SituacaoPedido situacao() {
        return SituacaoPedido.EM_PREPARACAO;
    }

    @Override
    public EstadoPedido enviar() {
        return SituacaoPedido.ENVIADO.estado();
    }

    @Override
    public EstadoPedido cancelar(Responsavel responsavel) {
        exigirResponsavelAdministrativo(responsavel);
        return SituacaoPedido.CANCELADO.estado();
    }

    @Override
    public Optional<EventoDominio> eventoDeChegada(DadosDaTransicao t) {
        return Optional.of(new PedidoEmPreparacao(t.pedidoId(), t.clienteId(), t.de(), t.para(), t.ocorridoEm()));
    }
}
