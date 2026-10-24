package br.com.fatec.ecommerce.preco.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;

/** Chain of Responsibility: handler que soma as linhas, incluindo o preco proprio dos combos. */
public class EtapaSubtotal implements EtapaPreco {
    @Override
    public void processar(CalculoPreco calculo) {
        Dinheiro subtotal = Dinheiro.zero();
        for (var item : calculo.contexto().itens()) {
            subtotal = subtotal.somar(item.subtotal());
        }
        calculo.definirSubtotal(subtotal);
    }
}
