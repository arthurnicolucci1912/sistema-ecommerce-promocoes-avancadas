package br.com.fatec.ecommerce.pedido.dominio.estado;

import br.com.fatec.ecommerce.compartilhado.dominio.EventoDominio;
import br.com.fatec.ecommerce.pedido.dominio.Responsavel;
import br.com.fatec.ecommerce.pedido.dominio.SituacaoPedido;
import br.com.fatec.ecommerce.pedido.dominio.evento.PedidoCriado;
import java.util.Optional;

/**
 * Estoque reservado, esperando o pagamento. Aceita confirmar (T2) e recusar (T3) o pagamento e o
 * cancelamento (T8). A chegada anuncia {@link PedidoCriado}.
 *
 * <p>Pattern: State - papel: estado concreto (AGUARDANDO_PAGAMENTO).
 *
 * <p>Ver docs/04-design-patterns.md, secao 3.4 (State: ciclo de vida do pedido).
 */
public final class AguardandoPagamento extends EstadoPedidoBase {

    @Override
    public SituacaoPedido situacao() {
        return SituacaoPedido.AGUARDANDO_PAGAMENTO;
    }

    @Override
    public EstadoPedido confirmarPagamento() {
        return SituacaoPedido.PAGO.estado();
    }

    @Override
    public EstadoPedido recusarPagamento() {
        return SituacaoPedido.PAGAMENTO_RECUSADO.estado();
    }

    @Override
    public EstadoPedido cancelar(Responsavel responsavel) {
        return SituacaoPedido.CANCELADO.estado();
    }

    @Override
    public Optional<EventoDominio> eventoDeChegada(DadosDaTransicao t) {
        return Optional.of(new PedidoCriado(t.pedidoId(), t.clienteId(), t.de(), t.para(), t.ocorridoEm()));
    }
}
