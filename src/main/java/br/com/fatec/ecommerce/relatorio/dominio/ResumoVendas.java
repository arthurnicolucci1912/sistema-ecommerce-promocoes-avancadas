package br.com.fatec.ecommerce.relatorio.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Indicadores de vendas de um período. Venda é o pedido pago dentro do período e não cancelado no momento da
 * consulta; o valor é o total do pedido. {@code quantidadeCancelamentos} conta as transições para cancelado ocorridas
 * no período, inclusive de pedidos que não chegaram a ser pagos.
 */
public record ResumoVendas(BigDecimal valorTotalVendas, long quantidadePedidos, BigDecimal ticketMedio,
        BigDecimal totalDescontos, long quantidadeCancelamentos) {

    /** Calcula o ticket médio (total / quantidade, HALF_EVEN com 2 casas; zero sem vendas). */
    public static ResumoVendas de(BigDecimal valorTotal, long quantidade, BigDecimal totalDescontos,
            long cancelamentos) {
        BigDecimal total = valorTotal.setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal ticket = quantidade == 0 ? BigDecimal.ZERO.setScale(2)
                : total.divide(BigDecimal.valueOf(quantidade), 2, RoundingMode.HALF_EVEN);
        return new ResumoVendas(total, quantidade, ticket, totalDescontos.setScale(2, RoundingMode.HALF_EVEN),
                cancelamentos);
    }
}
