package br.com.fatec.ecommerce.checkout.aplicacao;

import br.com.fatec.ecommerce.carrinho.aplicacao.ConsultarDisponibilidadeDoCarrinho.Insuficiencia;
import java.util.List;

/** Detalhamento pronto para confirmacao; disponibilidade informativa nao integra a expectativa comercial. */
public record PreviaCheckout(ConfirmacaoCheckout confirmacao, List<Insuficiencia> insuficiencias) {
    public PreviaCheckout { insuficiencias = List.copyOf(insuficiencias); }
}
