package br.com.fatec.ecommerce.promocao.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
/** Strategy: beneficio PromocaoFreteGratis, independente das demais campanhas. */
public class PromocaoFreteGratis extends PromocaoBase {
    
    public PromocaoFreteGratis(PromocaoConfig config, CondicaoElegibilidade condicao) { super(config,condicao);  }
    @Override protected Dinheiro beneficio(ContextoPreco contexto) { return contexto.freteBruto(); }
    @Override protected AlvoDesconto alvo() { return AlvoDesconto.FRETE; }
}
