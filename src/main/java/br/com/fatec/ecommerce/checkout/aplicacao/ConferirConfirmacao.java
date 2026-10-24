package br.com.fatec.ecommerce.checkout.aplicacao;

import br.com.fatec.ecommerce.checkout.aplicacao.CheckoutAlteradoException.Divergencia;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Compara o detalhamento normalizado inteiro; totais iguais nao ocultam mudancas nas linhas. */
final class ConferirConfirmacao {
    private ConferirConfirmacao() { }

    static void conferir(ConfirmacaoCheckout anterior, PreviaCheckout previa) {
        var atual = previa.confirmacao();
        List<Divergencia> diferencas = new ArrayList<>();
        comparar(diferencas, "versaoCarrinho", anterior.versaoCarrinho(), atual.versaoCarrinho());
        comparar(diferencas, "itens", anterior.itens(), atual.itens());
        comparar(diferencas, "descontos", anterior.descontos(), atual.descontos());
        comparar(diferencas, "subtotal", anterior.subtotal(), atual.subtotal());
        comparar(diferencas, "frete", anterior.frete(), atual.frete());
        comparar(diferencas, "total", anterior.total(), atual.total());
        comparar(diferencas, "endereco", anterior.endereco(), atual.endereco());
        comparar(diferencas, "opcaoEntrega", anterior.opcaoEntrega(), atual.opcaoEntrega());
        comparar(diferencas, "freteLiquido", anterior.freteLiquido(), atual.freteLiquido());
        comparar(diferencas, "cupom", anterior.cupom(), atual.cupom());
        if (!diferencas.isEmpty()) { throw new CheckoutAlteradoException(previa, diferencas); }
    }

    private static void comparar(List<Divergencia> diferencas, String campo, Object anterior, Object atual) {
        if (!Objects.equals(anterior, atual)) { diferencas.add(new Divergencia(campo, anterior, atual)); }
    }
}
