package br.com.fatec.ecommerce.pagamento.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;

/**
 * Pedido de estorno de uma cobranca anterior.
 *
 * <p>Contrato provisorio (onda 0): mudancas via PR revisado pelos consumidores e registradas em docs/03-arquitetura.md.
 *
 * @param idTransacaoExterna transacao a estornar
 * @param valor              valor a estornar
 */
public record SolicitacaoEstorno(String idTransacaoExterna, Dinheiro valor) {
}
