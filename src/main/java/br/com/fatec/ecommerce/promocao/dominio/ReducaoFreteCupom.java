package br.com.fatec.ecommerce.promocao.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
/** Strategy concreta de cupom: ReducaoFreteCupom. */
public class ReducaoFreteCupom implements BeneficioCupom {
    private final Dinheiro valor;
    public ReducaoFreteCupom(java.util.Map<String,Object> parametros) { valor=Parametros.dinheiro(parametros,"valor"); if(!valor.maiorQue(Dinheiro.zero())) { throw Parametros.invalido("valor"); } }
    public AlvoDesconto alvo() { return AlvoDesconto.FRETE; }
    public Dinheiro calcular(Dinheiro itens,Dinheiro frete) { return valor.menorQue(frete) ? valor : frete; }
}
