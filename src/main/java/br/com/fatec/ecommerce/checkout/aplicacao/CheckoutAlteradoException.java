package br.com.fatec.ecommerce.checkout.aplicacao;

import br.com.fatec.ecommerce.compartilhado.dominio.ConflitoException;
import java.util.List;
import java.util.Map;

/** Conflito comercial recuperavel: o cliente recebe o detalhamento corrente para confirmar novamente. */
public class CheckoutAlteradoException extends ConflitoException {
    public record Divergencia(String campo, Object anterior, Object atual) { }
    private final PreviaCheckout previa;
    private final List<Divergencia> divergencias;

    CheckoutAlteradoException(PreviaCheckout previa, List<Divergencia> divergencias) {
        super("A compra mudou. Confira o detalhamento atualizado e confirme novamente.");
        this.previa = previa;
        this.divergencias = List.copyOf(divergencias);
    }

    @Override public String tipo() { return "checkout-alterado"; }
    @Override public String titulo() { return "Confirmação desatualizada"; }
    @Override public Map<String, Object> propriedades() {
        return Map.of("divergencias", divergencias, "previaAtualizada", previa);
    }
}
