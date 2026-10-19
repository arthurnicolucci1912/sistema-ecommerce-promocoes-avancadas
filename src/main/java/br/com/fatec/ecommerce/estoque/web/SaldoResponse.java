package br.com.fatec.ecommerce.estoque.web;

import br.com.fatec.ecommerce.estoque.dominio.SaldoEstoque;
import java.time.Instant;

/** Saldo de uma variação: o disponível pode ser vendido; o reservado está comprometido com pedidos em andamento. */
public record SaldoResponse(Long variacaoId, int disponivel, int reservado, Instant atualizadoEm) {

    static SaldoResponse de(SaldoEstoque saldo) {
        return new SaldoResponse(saldo.getVariacaoId(), saldo.getDisponivel(), saldo.getReservado(),
                saldo.getAtualizadoEm());
    }
}
