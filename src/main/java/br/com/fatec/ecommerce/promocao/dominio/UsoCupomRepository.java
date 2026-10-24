package br.com.fatec.ecommerce.promocao.dominio;

import java.time.Instant;

/** Porta transacional: uma utilizacao por pedido; consumo e devolucao usam a mesma trava por cupom. */
public interface UsoCupomRepository {
    void consumir(Long cupomId, Long versaoEsperada, Long pedidoId, Long clienteId, Instant momento);
    void devolver(Long pedidoId, Instant momento);
}
