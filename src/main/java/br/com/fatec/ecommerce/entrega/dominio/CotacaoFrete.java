package br.com.fatec.ecommerce.entrega.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;

/**
 * Cotacao de frete devolvida por um provedor.
 *
 * <p>Contrato provisorio (onda 0): mudancas via PR revisado pelos consumidores e registradas em docs/03-arquitetura.md.
 *
 * @param modalidade modalidade de entrega cotada
 * @param custo      custo do frete
 * @param prazoDias  prazo estimado em dias uteis apos aprovacao do pagamento
 */
public record CotacaoFrete(String modalidade, Dinheiro custo, int prazoDias) {
    public CotacaoFrete {
        if (modalidade == null || !modalidade.matches("[A-Z0-9_-]{1,40}") || custo == null
                || custo.ehNegativo() || custo.valor().precision() > 12 || prazoDias <= 0)
            throw new br.com.fatec.ecommerce.compartilhado.dominio.DadoInvalidoException("Cotação de frete inválida.");
    }
}
