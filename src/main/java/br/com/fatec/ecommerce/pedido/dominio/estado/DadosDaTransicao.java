package br.com.fatec.ecommerce.pedido.dominio.estado;

import br.com.fatec.ecommerce.pedido.dominio.Responsavel;
import br.com.fatec.ecommerce.pedido.dominio.SituacaoPedido;
import java.time.Instant;

/**
 * Dados de uma transicao ja aplicada, entregues ao estado de destino para montar o evento que a anuncia.
 *
 * <p>Pattern: State - papel: parametro de {@link EstadoPedido#eventoDeChegada}; leva do contexto (Pedido) ao
 * estado o que o evento precisa, sem expor o agregado.
 *
 * <p>Ver docs/04-design-patterns.md, secao 3.4 (State: ciclo de vida do pedido).
 *
 * @param motivo motivo informado, ou nulo quando a operacao nao o exige
 */
public record DadosDaTransicao(Long pedidoId, Long clienteId, SituacaoPedido de, SituacaoPedido para,
        Instant ocorridoEm, Responsavel responsavel, String motivo) {
}
