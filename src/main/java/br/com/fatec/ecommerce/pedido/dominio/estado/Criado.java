package br.com.fatec.ecommerce.pedido.dominio.estado;

import br.com.fatec.ecommerce.pedido.dominio.Responsavel;
import br.com.fatec.ecommerce.pedido.dominio.SituacaoPedido;

/**
 * Pedido gerado com o snapshot, ainda sem reserva de estoque. Aceita aguardar pagamento (T1) e cancelamento
 * (T7). E o estado inicial: chegar a ele nao anuncia evento.
 *
 * <p>Pattern: State - papel: estado concreto (CRIADO).
 *
 * <p>Ver docs/04-design-patterns.md, secao 3.4 (State: ciclo de vida do pedido).
 */
public final class Criado extends EstadoPedidoBase {

    @Override
    public SituacaoPedido situacao() {
        return SituacaoPedido.CRIADO;
    }

    @Override
    public EstadoPedido aguardarPagamento() {
        return SituacaoPedido.AGUARDANDO_PAGAMENTO.estado();
    }

    @Override
    public EstadoPedido cancelar(Responsavel responsavel) {
        return SituacaoPedido.CANCELADO.estado();
    }
}
