package br.com.fatec.ecommerce.promocao.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
/** Strategy concreta de cupom: FreteGratisCupom. */
public class FreteGratisCupom implements BeneficioCupom {
    
    public FreteGratisCupom(java.util.Map<String,Object> parametros) {  }
    public AlvoDesconto alvo() { return AlvoDesconto.FRETE; }
    public Dinheiro calcular(Dinheiro itens,Dinheiro frete) { return frete; }
}
