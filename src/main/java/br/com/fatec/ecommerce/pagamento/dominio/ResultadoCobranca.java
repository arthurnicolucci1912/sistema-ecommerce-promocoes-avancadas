package br.com.fatec.ecommerce.pagamento.dominio;

/**
 * Resultado de uma cobranca. Pagamentos como PIX e boleto nascem {@link Situacao#PENDENTE} e sao
 * confirmados depois, por evento externo identificado por {@code idTransacaoExterna}.
 *
 * <p>Contrato provisorio (onda 0): mudancas via PR revisado pelos consumidores e registradas em docs/03-arquitetura.md.
 *
 * @param situacao           aprovado, recusado ou pendente
 * @param idTransacaoExterna identificador da transacao no fornecedor (base da idempotencia)
 */
public record ResultadoCobranca(Situacao situacao, String idTransacaoExterna) {

    public enum Situacao {
        APROVADO,
        RECUSADO,
        PENDENTE
    }
}
