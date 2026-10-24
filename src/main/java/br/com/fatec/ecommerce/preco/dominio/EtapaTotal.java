package br.com.fatec.ecommerce.preco.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import br.com.fatec.ecommerce.promocao.dominio.AlvoDesconto;

/** Chain of Responsibility: handler final que totaliza itens e frete, limitando cada alvo a zero. */
public class EtapaTotal implements EtapaPreco {
    @Override
    public void processar(CalculoPreco calculo) {
        Dinheiro itens = calculo.subtotal();
        Dinheiro frete = calculo.frete();
        for (var desconto : calculo.descontos()) {
            if (desconto.alvo() == AlvoDesconto.FRETE) {
                frete = frete.subtrair(desconto.valor());
            } else {
                itens = itens.subtrair(desconto.valor());
            }
        }
        calculo.definirTotal(maximoZero(itens).somar(maximoZero(frete)));
    }

    private static Dinheiro maximoZero(Dinheiro valor) {
        return valor.ehNegativo() ? Dinheiro.zero() : valor;
    }
}
