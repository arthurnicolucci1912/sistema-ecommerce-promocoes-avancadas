package br.com.fatec.ecommerce.pagamento.dominio;

/**
 * Meios de pagamento aceitos.
 *
 * <p>Contrato provisorio (onda 0): mudancas via PR revisado pelos consumidores e registradas em docs/03-arquitetura.md.
 */
public enum FormaPagamento {
    CARTAO_CREDITO,
    PIX,
    BOLETO
}
