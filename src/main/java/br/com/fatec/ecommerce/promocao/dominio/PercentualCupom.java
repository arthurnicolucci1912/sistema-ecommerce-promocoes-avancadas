package br.com.fatec.ecommerce.promocao.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
/** Strategy concreta de cupom: PercentualCupom. */
public class PercentualCupom implements BeneficioCupom {
    private final java.math.BigDecimal taxa;
    public PercentualCupom(java.util.Map<String,Object> parametros) { taxa=Parametros.percentual(parametros); }
    public AlvoDesconto alvo() { return AlvoDesconto.ITENS; }
    public Dinheiro calcular(Dinheiro itens,Dinheiro frete) { return itens.multiplicar(taxa); }
}
