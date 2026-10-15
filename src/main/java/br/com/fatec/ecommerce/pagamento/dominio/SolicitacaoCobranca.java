package br.com.fatec.ecommerce.pagamento.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;

/**
 * Pedido de cobranca enviado ao gateway.
 *
 * <p>Contrato provisorio (onda 0): mudancas via PR revisado pelos consumidores e registradas em docs/03-arquitetura.md.
 *
 * @param pedidoId  pedido a cobrar
 * @param forma     meio de pagamento
 * @param valor     valor a cobrar
 */
public record SolicitacaoCobranca(Long pedidoId, FormaPagamento forma, Dinheiro valor) {
}
