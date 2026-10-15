package br.com.fatec.ecommerce.seguranca.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.fatec.ecommerce.compartilhado.dominio.SenhaForte;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Politica de senha sem Spring. */
class SenhaForteTests {

    record Entrada(@SenhaForte String senha) {
    }

    private final Validator validador = Validation.buildDefaultValidatorFactory().getValidator();

    private boolean valida(String senha) {
        return validador.validate(new Entrada(senha)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"abcdefgh", "1234567", "12345678", "abc1", "Abcdef1"})
    void rejeitaSenhasFracas(String senha) {
        assertThat(valida(senha)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Demo1234", "abcdefg1", "1abcdefg", "senha segura 9"})
    void aceitaSenhasQueAtendemAPolitica(String senha) {
        assertThat(valida(senha)).isTrue();
    }

    @Test
    void mensagemEmPortugues() {
        var violacoes = validador.validate(new Entrada("abcdefgh"));
        assertThat(violacoes).singleElement().satisfies(v -> {
            assertThat(v.getMessage()).contains("mínimo 8 caracteres").contains("letra").contains("dígito");
            assertThat(v.getPropertyPath().toString()).isEqualTo("senha");
        });
    }
}
