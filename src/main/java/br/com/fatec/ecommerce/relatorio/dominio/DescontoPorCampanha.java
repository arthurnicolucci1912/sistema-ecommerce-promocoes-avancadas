package br.com.fatec.ecommerce.relatorio.dominio;

import java.math.BigDecimal;

/**
 * Desconto concedido por campanha ou cupom nas vendas do período. {@code referenciaId} e {@code codigo} podem ser
 * nulos (descontos registrados antes de o pedido guardar a referência).
 */
public record DescontoPorCampanha(String origem, Long referenciaId, String descricao, String codigo,
        long quantidadePedidos, BigDecimal valorConcedido) {
}
