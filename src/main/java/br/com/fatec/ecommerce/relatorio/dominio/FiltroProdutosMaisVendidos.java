package br.com.fatec.ecommerce.relatorio.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.DadoInvalidoException;

/** Período e limite do ranking de produtos; o limite fica entre 1 e 50 (padrão 10 aplicado pela API). */
public record FiltroProdutosMaisVendidos(PeriodoRelatorio periodo, int limite) {
    public static final int LIMITE_PADRAO = 10;
    public static final int LIMITE_MAXIMO = 50;

    public FiltroProdutosMaisVendidos {
        if (periodo == null) {
            throw new DadoInvalidoException("O período é obrigatório.");
        }
        if (limite < 1 || limite > LIMITE_MAXIMO) {
            throw new DadoInvalidoException("limite deve estar entre 1 e " + LIMITE_MAXIMO + ".");
        }
    }
}
