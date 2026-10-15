package br.com.fatec.ecommerce.promocao.dominio;

/**
 * Como uma promocao convive com as demais no mesmo calculo de preco.
 *
 * <p>Contrato provisorio (onda 0): mudancas via PR revisado pelos consumidores e registradas em docs/03-arquitetura.md.
 */
public enum ModoAcumulacao {
    /** Soma-se aos descontos de outras promocoes. */
    ACUMULAVEL,
    /** Nao se soma a outras: a politica de conflito escolhe entre as nao acumulaveis. */
    NAO_ACUMULAVEL,
    /** Quando elegivel, anula todas as demais. */
    EXCLUSIVA
}
