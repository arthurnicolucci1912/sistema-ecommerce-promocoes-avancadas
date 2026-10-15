package br.com.fatec.ecommerce.pedido.web;

import br.com.fatec.ecommerce.pedido.dominio.ResumoPedido;
import br.com.fatec.ecommerce.pedido.dominio.SituacaoPedido;
import java.math.BigDecimal;
import java.time.Instant;

/** Linha de listagem de pedidos: sem itens, descontos nem histórico. */
public record ResumoPedidoResponse(Long id, Long clienteId, SituacaoPedido situacao, BigDecimal total,
        Instant criadoEm) {

    static ResumoPedidoResponse de(ResumoPedido resumo) {
        return new ResumoPedidoResponse(resumo.id(), resumo.clienteId(), resumo.situacao(), resumo.total().valor(),
                resumo.criadoEm());
    }
}
