package br.com.fatec.ecommerce.pedido.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.Pagina;
import java.util.Optional;

/** Porta de persistencia de pedidos (implementada em {@code infra.persistencia}). */
public interface PedidoRepository {

    /**
     * Grava o pedido e faz flush imediato. Uma alteracao concorrente do mesmo pedido (lock otimista) resulta em
     * {@link br.com.fatec.ecommerce.compartilhado.dominio.ConflitoException}.
     */
    Pedido salvar(Pedido pedido);

    Optional<Pedido> buscarPorId(Long id);

    /** Serializa pagamento/cancelamento; exige transacao ativa e precede locks de estoque/cupom. */
    default Optional<Pedido> bloquearPorId(Long id) {
        throw new UnsupportedOperationException("Bloqueio de pedido não implementado neste adaptador.");
    }

    /** Busca sempre pelo par (id, clienteId); pedido de outro cliente nao e encontrado. */
    Optional<Pedido> buscarPorIdECliente(Long id, Long clienteId);

    /** Resumos de todos os pedidos, do mais recente para o mais antigo; {@code situacao} nula nao filtra. */
    Pagina<ResumoPedido> listarResumos(SituacaoPedido situacao, int pagina, int tamanho);

    /** Resumos dos pedidos de um cliente, do mais recente para o mais antigo. */
    Pagina<ResumoPedido> listarResumosDoCliente(Long clienteId, int pagina, int tamanho);

    boolean existeDoCliente(Long clienteId);

    /** Historico de aprovacao, incluindo pedidos posteriormente cancelados (primeira compra). */
    default boolean existePagamentoAprovadoAte(Long clienteId, java.time.Instant momento) {
        throw new UnsupportedOperationException("Consulta de pagamentos não implementada neste adaptador.");
    }
}
