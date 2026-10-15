package br.com.fatec.ecommerce.estoque.dominio;

import java.util.List;

/** Porta de persistencia das reservas de estoque. */
public interface ReservaEstoqueRepository {

    /** Grava a reserva; ja existir reserva do pedido para a variacao vira {@code ConflitoException}. */
    ReservaEstoque salvar(ReservaEstoque reserva);

    boolean existePorPedido(Long pedidoId);

    /** Reservas do pedido na situacao dada, ordenadas pela variacao (ordem estavel evita deadlock). */
    List<ReservaEstoque> listarPorPedidoESituacao(Long pedidoId, SituacaoReserva situacao);

    /** Troca a situacao so se ainda for {@code de}; devolve se a troca aconteceu (base da idempotencia). */
    boolean transitar(Long reservaId, SituacaoReserva de, SituacaoReserva para);
}
