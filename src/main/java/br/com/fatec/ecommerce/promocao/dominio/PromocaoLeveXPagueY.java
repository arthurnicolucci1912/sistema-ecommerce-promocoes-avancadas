package br.com.fatec.ecommerce.promocao.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
/** Strategy: beneficio PromocaoLeveXPagueY, independente das demais campanhas. */
public class PromocaoLeveXPagueY extends PromocaoBase {
    private final String sku; private final int leve, pague;
    public PromocaoLeveXPagueY(PromocaoConfig config, CondicaoElegibilidade condicao) { super(config,condicao); sku=Parametros.texto(config.parametros(),"sku"); leve=Parametros.inteiro(config.parametros(),"leve");
        pague=Parametros.inteiro(config.parametros(),"pague"); if(pague>=leve) { throw Parametros.invalido("leve/pague"); } }
    @Override protected Dinheiro beneficio(ContextoPreco contexto) { return contexto.itens().stream().filter(i -> i.sku().equals(sku))
            .map(i -> i.precoUnitario().multiplicar((i.quantidade()/leve)*(leve-pague)))
            .reduce(Dinheiro.zero(), Dinheiro::somar); }
    
}
