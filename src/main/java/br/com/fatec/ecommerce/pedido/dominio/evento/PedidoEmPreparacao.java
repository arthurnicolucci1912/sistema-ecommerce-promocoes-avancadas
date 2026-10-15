package br.com.fatec.ecommerce.pedido.dominio.evento;

import br.com.fatec.ecommerce.compartilhado.dominio.EventoDominio;
import br.com.fatec.ecommerce.pedido.dominio.SituacaoPedido;
import java.time.Instant;

/**
 * O pedido entrou em preparacao (separacao e embalagem).
 *
 * <p>Pattern: Observer - papel: evento; o pedido o publica sem conhecer quem reage. O {@code de} permite ao
 * ouvinte decidir o efeito (ex.: liberar reserva ou repor baixa de estoque).
 *
 * <p>Ver docs/04-design-patterns.md, secao 3.6 (Observer: eventos de dominio).
 */
public record PedidoEmPreparacao(Long pedidoId, Long clienteId, SituacaoPedido de, SituacaoPedido para, Instant ocorridoEm)
        implements EventoDominio {
}
