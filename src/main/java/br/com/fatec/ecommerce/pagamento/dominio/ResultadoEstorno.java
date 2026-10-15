package br.com.fatec.ecommerce.pagamento.dominio;

/**
 * Resultado de um estorno.
 *
 * <p>Contrato provisorio (onda 0): mudancas via PR revisado pelos consumidores e registradas em docs/03-arquitetura.md.
 *
 * @param estornado          se o fornecedor aceitou o estorno
 * @param idTransacaoExterna identificador do estorno no fornecedor
 */
public record ResultadoEstorno(boolean estornado, String idTransacaoExterna) {
}
