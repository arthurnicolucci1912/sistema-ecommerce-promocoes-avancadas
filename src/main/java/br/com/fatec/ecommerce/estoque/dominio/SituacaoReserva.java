package br.com.fatec.ecommerce.estoque.dominio;

/**
 * Situacao da reserva de estoque de um pedido (persistida como texto): ATIVA (quantidade reservada), LIBERADA
 * (voltou ao disponivel), BAIXADA (saiu do estoque, pagamento aprovado) e REPOSTA (baixa desfeita, voltou ao
 * disponivel). As transicoes validas sao ATIVA para LIBERADA ou BAIXADA, e BAIXADA para REPOSTA.
 */
public enum SituacaoReserva {
    ATIVA,
    LIBERADA,
    BAIXADA,
    REPOSTA
}
