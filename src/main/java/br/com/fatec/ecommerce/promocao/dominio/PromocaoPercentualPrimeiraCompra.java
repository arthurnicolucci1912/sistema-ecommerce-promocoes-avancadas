package br.com.fatec.ecommerce.promocao.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
/** Strategy: beneficio PromocaoPercentualPrimeiraCompra, independente das demais campanhas. */
public class PromocaoPercentualPrimeiraCompra extends PromocaoBase {
    private final java.math.BigDecimal taxa;
    public PromocaoPercentualPrimeiraCompra(PromocaoConfig config, CondicaoElegibilidade condicao) { super(config,condicao); taxa=Parametros.percentual(config.parametros()); }
    @Override protected Dinheiro beneficio(ContextoPreco contexto) { return contexto.primeiraCompra() ? contexto.subtotal().multiplicar(taxa) : Dinheiro.zero(); }
    
}
