package br.com.fatec.ecommerce.pedido.dominio;

import br.com.fatec.ecommerce.pedido.dominio.estado.AguardandoPagamento;
import br.com.fatec.ecommerce.pedido.dominio.estado.Cancelado;
import br.com.fatec.ecommerce.pedido.dominio.estado.Criado;
import br.com.fatec.ecommerce.pedido.dominio.estado.EmPreparacao;
import br.com.fatec.ecommerce.pedido.dominio.estado.Entregue;
import br.com.fatec.ecommerce.pedido.dominio.estado.Enviado;
import br.com.fatec.ecommerce.pedido.dominio.estado.EstadoPedido;
import br.com.fatec.ecommerce.pedido.dominio.estado.PagamentoRecusado;
import br.com.fatec.ecommerce.pedido.dominio.estado.Pago;

/**
 * Situacao do pedido, persistida como texto, e ponte para o objeto de estado que a implementa.
 *
 * <p>Pattern: State - papel: mapeamento entre o valor persistido e o {@link EstadoPedido} correspondente
 * ({@link #estado()}); os estados nao guardam dados, entao cada constante mantem uma unica instancia.
 *
 * <p>Ver docs/04-design-patterns.md, secao 3.4 (State: ciclo de vida do pedido).
 */
public enum SituacaoPedido {
    CRIADO(new Criado()),
    AGUARDANDO_PAGAMENTO(new AguardandoPagamento()),
    PAGO(new Pago()),
    PAGAMENTO_RECUSADO(new PagamentoRecusado()),
    EM_PREPARACAO(new EmPreparacao()),
    ENVIADO(new Enviado()),
    ENTREGUE(new Entregue()),
    CANCELADO(new Cancelado());

    private final EstadoPedido estado;

    SituacaoPedido(EstadoPedido estado) {
        this.estado = estado;
    }

    public EstadoPedido estado() {
        return estado;
    }
}
