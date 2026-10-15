package br.com.fatec.ecommerce.pedido.dominio.evento;

import br.com.fatec.ecommerce.compartilhado.dominio.EventoDominio;
import br.com.fatec.ecommerce.pedido.dominio.Responsavel;
import br.com.fatec.ecommerce.pedido.dominio.SituacaoPedido;
import java.time.Instant;

/**
 * O pedido foi cancelado. Alem dos dados comuns, informa quem cancelou e o motivo.
 *
 * <p>Pattern: Observer - papel: evento; o pedido o publica sem conhecer quem reage. O {@code de} permite ao
 * ouvinte decidir o efeito (liberar reserva de estoque em AGUARDANDO_PAGAMENTO, repor baixa e estornar em PAGO ou
 * EM_PREPARACAO).
 *
 * <p>Ver docs/04-design-patterns.md, secao 3.6 (Observer: eventos de dominio).
 */
public record PedidoCancelado(Long pedidoId, Long clienteId, SituacaoPedido de, SituacaoPedido para,
        Instant ocorridoEm, Responsavel responsavel, String motivo) implements EventoDominio {
}
