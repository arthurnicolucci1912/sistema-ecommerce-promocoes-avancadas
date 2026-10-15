package br.com.fatec.ecommerce.pedido.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import java.time.Instant;

/**
 * Visao enxuta do pedido para listagens (id, cliente, situacao, total e data), lida sem carregar itens, descontos
 * nem historico.
 */
public record ResumoPedido(Long id, Long clienteId, SituacaoPedido situacao, Dinheiro total, Instant criadoEm) {
}
