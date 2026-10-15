package br.com.fatec.ecommerce.pedido.dominio.estado;

import br.com.fatec.ecommerce.compartilhado.dominio.EventoDominio;
import br.com.fatec.ecommerce.pedido.dominio.Responsavel;
import br.com.fatec.ecommerce.pedido.dominio.SituacaoPedido;
import br.com.fatec.ecommerce.pedido.dominio.evento.PedidoPagamentoRecusado;
import java.util.Optional;

/**
 * Pagamento recusado; o pedido nao e reaberto e so admite cancelamento (T9). A chegada anuncia {@link
 * PedidoPagamentoRecusado}.
 *
 * <p>Pattern: State - papel: estado concreto (PAGAMENTO_RECUSADO).
 *
 * <p>Ver docs/04-design-patterns.md, secao 3.4 (State: ciclo de vida do pedido).
 */
public final class PagamentoRecusado extends EstadoPedidoBase {

    @Override
    public SituacaoPedido situacao() {
        return SituacaoPedido.PAGAMENTO_RECUSADO;
    }

    @Override
    public EstadoPedido cancelar(Responsavel responsavel) {
        return SituacaoPedido.CANCELADO.estado();
    }

    @Override
    public Optional<EventoDominio> eventoDeChegada(DadosDaTransicao t) {
        return Optional.of(new PedidoPagamentoRecusado(t.pedidoId(), t.clienteId(), t.de(), t.para(),
                t.ocorridoEm()));
    }
}
