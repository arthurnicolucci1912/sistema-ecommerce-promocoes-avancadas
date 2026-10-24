package br.com.fatec.ecommerce.promocao.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
/** Strategy: beneficio PromocaoValorFixoAcimaDe, independente das demais campanhas. */
public class PromocaoValorFixoAcimaDe extends PromocaoBase {
    private final Dinheiro minimo, valor;
    public PromocaoValorFixoAcimaDe(PromocaoConfig config, CondicaoElegibilidade condicao) { super(config,condicao); minimo=Parametros.dinheiro(config.parametros(),"minimo"); valor=Parametros.dinheiro(config.parametros(),"valor");
        if(!valor.maiorQue(Dinheiro.zero())) { throw Parametros.invalido("valor"); } }
    @Override protected Dinheiro beneficio(ContextoPreco contexto) { return contexto.subtotal().maiorOuIgualA(minimo) ? valor : Dinheiro.zero(); }
    
}
