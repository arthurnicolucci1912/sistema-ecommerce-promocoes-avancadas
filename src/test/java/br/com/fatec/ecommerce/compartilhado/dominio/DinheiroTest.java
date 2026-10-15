package br.com.fatec.ecommerce.compartilhado.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class DinheiroTest {

    @Test
    void somaDuasQuantias() {
        assertThat(Dinheiro.de("10.10").somar(Dinheiro.de("0.20"))).isEqualTo(Dinheiro.de("10.30"));
    }

    @Test
    void subtraiEPermiteResultadoNegativo() {
        Dinheiro resultado = Dinheiro.de("5.00").subtrair(Dinheiro.de("7.50"));
        assertThat(resultado).isEqualTo(Dinheiro.de("-2.50"));
        assertThat(resultado.ehNegativo()).isTrue();
    }

    @Test
    void multiplicaPorInteiroEPorDecimal() {
        assertThat(Dinheiro.de("19.99").multiplicar(3)).isEqualTo(Dinheiro.de("59.97"));
        assertThat(Dinheiro.de("100.00").multiplicar(new BigDecimal("0.15"))).isEqualTo(Dinheiro.de("15.00"));
    }

    @Test
    void arredondaHalfEven() {
        assertThat(Dinheiro.de("0.125")).isEqualTo(Dinheiro.de("0.12"));
        assertThat(Dinheiro.de("0.135")).isEqualTo(Dinheiro.de("0.14"));
        assertThat(Dinheiro.de(new BigDecimal("0.125")).valor()).isEqualByComparingTo("0.12");
        assertThat(Dinheiro.de("10.00").multiplicar(new BigDecimal("0.0125"))).isEqualTo(Dinheiro.de("0.12"));
    }

    @Test
    void escalaSempreDois() {
        assertThat(Dinheiro.de("10").valor().scale()).isEqualTo(2);
        assertThat(Dinheiro.deCentavos(1999).valor()).isEqualTo(new BigDecimal("19.99"));
        assertThat(Dinheiro.zero().valor().scale()).isEqualTo(2);
        assertThat(Dinheiro.de("10").toString()).isEqualTo("10.00");
    }

    @Test
    void operacoesNaoAlteramAsInstanciasOriginais() {
        Dinheiro a = Dinheiro.de("10.00");
        Dinheiro b = Dinheiro.de("2.00");
        a.somar(b);
        a.subtrair(b);
        a.multiplicar(5);
        assertThat(a).isEqualTo(Dinheiro.de("10.00"));
        assertThat(b).isEqualTo(Dinheiro.de("2.00"));
    }

    @Test
    void igualdadePorValorNormalizado() {
        assertThat(Dinheiro.de("10")).isEqualTo(Dinheiro.de("10.00"));
        assertThat(Dinheiro.de(new BigDecimal("10.0"))).isEqualTo(Dinheiro.de("10.00"));
        assertThat(Dinheiro.de("10")).hasSameHashCodeAs(Dinheiro.de("10.00"));
        assertThat(Dinheiro.de("10")).isNotEqualTo(Dinheiro.de("10.01"));
    }

    @Test
    void comparacoes() {
        Dinheiro dez = Dinheiro.de("10.00");
        Dinheiro vinte = Dinheiro.de("20.00");
        assertThat(vinte.maiorQue(dez)).isTrue();
        assertThat(dez.maiorQue(vinte)).isFalse();
        assertThat(dez.menorQue(vinte)).isTrue();
        assertThat(dez.maiorOuIgualA(Dinheiro.de("10"))).isTrue();
        assertThat(dez.menorOuIgualA(Dinheiro.de("10"))).isTrue();
        assertThat(dez.maiorQue(Dinheiro.de("10"))).isFalse();
    }

    @Test
    void zeroENegativo() {
        assertThat(Dinheiro.zero().ehZero()).isTrue();
        assertThat(Dinheiro.de("0.004").ehZero()).isTrue();
        assertThat(Dinheiro.de("0.01").ehZero()).isFalse();
        assertThat(Dinheiro.zero().ehNegativo()).isFalse();
        assertThat(Dinheiro.de("-0.01").ehNegativo()).isTrue();
    }

    @Test
    void rejeitaNulosEValoresInvalidos() {
        assertThatThrownBy(() -> Dinheiro.de((String) null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Dinheiro.de((BigDecimal) null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Dinheiro.de("abc")).isInstanceOf(NumberFormatException.class);
    }
}
