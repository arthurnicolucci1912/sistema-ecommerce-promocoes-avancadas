package br.com.fatec.ecommerce.promocao.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
/** Strategy: beneficio PromocaoPercentualPorCategoria, independente das demais campanhas. */
public class PromocaoPercentualPorCategoria extends PromocaoBase {
    private final Long categoria; private final java.math.BigDecimal taxa;
    public PromocaoPercentualPorCategoria(PromocaoConfig config, CondicaoElegibilidade condicao) { super(config,condicao); categoria=Parametros.id(config.parametros(),"categoriaId"); taxa=Parametros.percentual(config.parametros()); }
    @Override protected Dinheiro beneficio(ContextoPreco contexto) { return contexto.itens().stream().filter(i -> i.categoriaIds().contains(categoria))
            .map(ItemPreco::subtotal).reduce(Dinheiro.zero(),Dinheiro::somar).multiplicar(taxa); }
    
}
