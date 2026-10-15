package br.com.fatec.ecommerce.pedido;

import br.com.fatec.ecommerce.pedido.aplicacao.ConfirmarReservaDoPedido;
import br.com.fatec.ecommerce.pedido.aplicacao.CriarPedido;
import br.com.fatec.ecommerce.pedido.aplicacao.TransicionarPedido;
import br.com.fatec.ecommerce.pedido.dominio.Responsavel;
import br.com.fatec.ecommerce.pedido.dominio.SituacaoPedido;
import org.springframework.stereotype.Component;

/**
 * Apoio aos testes de integracao do pedido: grava um pedido do cliente e o leva a uma situacao pelos servicos de
 * aplicacao (sem HTTP). Quem avanca o andamento e um responsavel administrativo; a aprovacao e do sistema.
 */
@Component
public class PedidosPersistidosParaTeste {

    private static final Responsavel ADMIN = Responsavel.administrativo(1L);

    private final CriarPedido criarPedido;
    private final ConfirmarReservaDoPedido confirmarReserva;
    private final TransicionarPedido transicionar;

    public PedidosPersistidosParaTeste(CriarPedido criarPedido, ConfirmarReservaDoPedido confirmarReserva,
            TransicionarPedido transicionar) {
        this.criarPedido = criarPedido;
        this.confirmarReserva = confirmarReserva;
        this.transicionar = transicionar;
    }

    /** Devolve o id do pedido, criado com o snapshot padrao e levado a {@code alvo} pelo caminho valido mais curto. */
    public Long criar(Long clienteId, SituacaoPedido alvo) {
        Long id = criarPedido.executar(PedidosParaTeste.validos(clienteId)).getId();
        if (alvo == SituacaoPedido.CRIADO) {
            return id;
        }
        confirmarReserva.executar(id);
        switch (alvo) {
            case AGUARDANDO_PAGAMENTO -> { }
            case PAGAMENTO_RECUSADO -> transicionar.recusarPagamento(id, Responsavel.sistema(), null);
            case CANCELADO -> transicionar.cancelar(id, ADMIN, "cancelado no teste");
            default -> {
                transicionar.aprovarPagamento(id, Responsavel.sistema(), null);
                if (alvo != SituacaoPedido.PAGO) {
                    transicionar.iniciarPreparacao(id, ADMIN);
                }
                if (alvo == SituacaoPedido.ENVIADO || alvo == SituacaoPedido.ENTREGUE) {
                    transicionar.enviar(id, ADMIN);
                }
                if (alvo == SituacaoPedido.ENTREGUE) {
                    transicionar.entregar(id, ADMIN);
                }
            }
        }
        return id;
    }
}
