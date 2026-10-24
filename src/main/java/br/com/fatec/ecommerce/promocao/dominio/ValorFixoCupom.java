package br.com.fatec.ecommerce.promocao.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
/** Strategy concreta de cupom: ValorFixoCupom. */
public class ValorFixoCupom implements BeneficioCupom {
    private final Dinheiro valor;
    public ValorFixoCupom(java.util.Map<String,Object> parametros) { valor=Parametros.dinheiro(parametros,"valor"); if(!valor.maiorQue(Dinheiro.zero())) { throw Parametros.invalido("valor"); } }
    public AlvoDesconto alvo() { return AlvoDesconto.ITENS; }
    public Dinheiro calcular(Dinheiro itens,Dinheiro frete) { return valor.menorQue(itens) ? valor : itens; }
}
