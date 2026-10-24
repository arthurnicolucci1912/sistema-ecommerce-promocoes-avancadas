package br.com.fatec.ecommerce.pagamento.infra.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Cenarios definidos pelo servidor; nunca pelo cliente HTTP. */
@Component
public class CenariosPagamento {
    private final boolean cartaoAprovado;
    private final boolean falharEstorno;
    public CenariosPagamento(@Value("${ecommerce.pagamento.simulado.cartao-aprovado:true}") boolean cartaoAprovado,
            @Value("${ecommerce.pagamento.simulado.falhar-estorno:false}") boolean falharEstorno) {
        this.cartaoAprovado = cartaoAprovado; this.falharEstorno = falharEstorno;
    }
    public boolean cartaoAprovado() { return cartaoAprovado; }
    public boolean falharEstorno() { return falharEstorno; }
}
