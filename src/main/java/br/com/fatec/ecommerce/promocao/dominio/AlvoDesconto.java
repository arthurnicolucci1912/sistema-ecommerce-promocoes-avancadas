package br.com.fatec.ecommerce.promocao.dominio;

/**
 * Onde um desconto incide. Com {@link #FRETE}, o frete gratis entra no mesmo mecanismo das demais promocoes.
 *
 * <p>Contrato provisorio (onda 0): mudancas via PR revisado pelos consumidores e registradas em docs/03-arquitetura.md.
 */
public enum AlvoDesconto {
    /** Abate o subtotal dos itens. */
    ITENS,
    /** Abate o custo do frete. */
    FRETE
}
