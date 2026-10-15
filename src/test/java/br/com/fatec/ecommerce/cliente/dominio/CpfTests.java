package br.com.fatec.ecommerce.cliente.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fatec.ecommerce.cliente.CpfsParaTeste;
import br.com.fatec.ecommerce.compartilhado.dominio.DadoInvalidoException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CpfTests {

    @ParameterizedTest
    @ValueSource(strings = {"52998224725", "529.982.247-25", "111.444.777-35", "11144477735", "12345678909"})
    void aceitaCpfValidoComOuSemMascaraNormalizandoParaDigitos(String entrada) {
        Cpf cpf = Cpf.de(entrada);

        assertThat(cpf.digitos()).hasSize(11).containsOnlyDigits();
        assertThat(cpf.digitos()).isEqualTo(entrada.replaceAll("\\D", ""));
        assertThat(Cpf.valido(entrada)).isTrue();
    }

    @Test
    void aceitaCpfsGeradosComDigitosVerificadoresCalculados() {
        for (int i = 0; i < 200; i++) {
            assertThat(Cpf.valido(CpfsParaTeste.valido(i))).as(CpfsParaTeste.valido(i)).isTrue();
            assertThat(Cpf.valido(CpfsParaTeste.comMascara(i))).isTrue();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"52998224724", "52998224735", "12345678900", "529.982.247-26"})
    void rejeitaDigitoVerificadorIncorreto(String entrada) {
        assertThat(Cpf.valido(entrada)).isFalse();
        assertThatThrownBy(() -> Cpf.de(entrada)).isInstanceOf(DadoInvalidoException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"00000000000", "11111111111", "22222222222", "33333333333", "44444444444", "55555555555",
            "66666666666", "77777777777", "88888888888", "99999999999", "111.111.111-11"})
    void rejeitaSequenciasRepetidas(String entrada) {
        assertThat(Cpf.valido(entrada)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "123", "5299822472", "529982247255", "abcdefghijk", "529.982.247-2a"})
    void rejeitaTamanhoOuCaracteresInvalidos(String entrada) {
        assertThat(Cpf.valido(entrada)).isFalse();
    }

    @Test
    void entradaNulaEInvalida() {
        assertThat(Cpf.valido(null)).isFalse();
        assertThatThrownBy(() -> Cpf.de(null)).isInstanceOf(DadoInvalidoException.class);
    }

    record Dto(@NotBlank @CpfValido String cpf) {
    }

    @Test
    void anotacaoCpfValidoAplicaARegraEAceitaNulo() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        assertThat(validator.validate(new Dto("529.982.247-25"))).isEmpty();
        assertThat(validator.validate(new Dto("52998224724"))).hasSize(1);
        assertThat(validator.validate(new Dto("11111111111"))).hasSize(1);
    }
}
