package br.com.fatec.ecommerce.checkout.aplicacao;

import static org.assertj.core.api.Assertions.*;
import br.com.fatec.ecommerce.checkout.aplicacao.ConfirmacaoCheckout.*;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConfirmacaoCheckoutTests {
    Endereco endereco = new Endereco("Casa", "01001000", "Rua", "1", null, "Centro", "SP", "SP");
    Linha linha(String sku, String preco) {
        return new Linha(sku, sku, null, 1, new BigDecimal(preco), new BigDecimal(preco));
    }
    ConfirmacaoCheckout confirmacao(List<Linha> itens, long versao, Endereco destino) {
        return new ConfirmacaoCheckout(versao, itens, List.of(), new BigDecimal("30.00"),
                new BigDecimal("0.00"), new BigDecimal("30.00"), destino);
    }

    @Test
    void ordemDasLinhasEEscalaMonetariaNaoGeramFalsoConflito() {
        var anterior = confirmacao(List.of(linha("B", "20.00"), linha("A", "10.0")), 1, endereco);
        var atual = confirmacao(List.of(linha("A", "10"), linha("B", "20")), 1, endereco);
        assertThatCode(() -> ConferirConfirmacao.conferir(anterior, new PreviaCheckout(atual, List.of())))
                .doesNotThrowAnyException();
    }

    @Test
    void precosCompensadosComMesmoTotalExigemNovaConfirmacao() {
        var anterior = confirmacao(List.of(linha("A", "10"), linha("B", "20")), 1, endereco);
        var atual = confirmacao(List.of(linha("A", "15"), linha("B", "15")), 1, endereco);
        assertThatThrownBy(() -> ConferirConfirmacao.conferir(anterior, new PreviaCheckout(atual, List.of())))
                .isInstanceOfSatisfying(CheckoutAlteradoException.class, e -> {
                    assertThat(e.tipo()).isEqualTo("checkout-alterado");
                    assertThat(e.propriedades()).containsKeys("divergencias", "previaAtualizada");
                    assertThat(e.propriedades().get("divergencias").toString()).contains("itens");
                });
    }

    @Test
    void versaoSelecaoEDestinoSaoConferidos() {
        var anterior = confirmacao(List.of(linha("A", "30")), 1, endereco);
        var destino = new Endereco("Casa", "01001000", "Rua", "2", null, "Centro", "SP", "SP");
        var atual = confirmacao(List.of(linha("B", "30")), 2, destino);
        assertThatThrownBy(() -> ConferirConfirmacao.conferir(anterior, new PreviaCheckout(atual, List.of())))
                .isInstanceOfSatisfying(CheckoutAlteradoException.class,
                        e -> assertThat(e.propriedades().get("divergencias").toString())
                                .contains("versaoCarrinho", "itens", "endereco"));
        assertThatCode(() -> ConferirConfirmacao.conferir(atual, new PreviaCheckout(atual, List.of())))
                .doesNotThrowAnyException();
    }
}
