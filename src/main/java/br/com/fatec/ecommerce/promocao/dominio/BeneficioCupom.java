package br.com.fatec.ecommerce.promocao.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
/** Strategy de beneficio de cupom, independente do cadastro e do consumo. */
public interface BeneficioCupom {
    AlvoDesconto alvo();
    Dinheiro calcular(Dinheiro itensLiquidos,Dinheiro freteLiquido);
}
