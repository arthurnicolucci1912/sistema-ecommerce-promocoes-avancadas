package br.com.fatec.ecommerce.cliente.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fatec.ecommerce.compartilhado.dominio.DadoInvalidoException;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Regra do endereco principal e validacoes do endereco, sem Spring. */
class RegraEnderecoPrincipalTests {

    private static Endereco endereco(String apelido) {
        return new Endereco(1L, apelido, "01310-100", "Avenida Paulista", "1000", null, "Bela Vista", "São Paulo", "sp");
    }

    private static Endereco principal(String apelido) {
        Endereco e = endereco(apelido);
        e.marcarPrincipal();
        return e;
    }

    @Test
    void primeiroEnderecoSeriaPrincipal() {
        assertThat(RegraEnderecoPrincipal.novoEnderecoDeveSerPrincipal(List.of())).isTrue();
    }

    @Test
    void enderecoSeguinteNaoEPrincipalQuandoJaExisteUm() {
        assertThat(RegraEnderecoPrincipal.novoEnderecoDeveSerPrincipal(List.of(principal("casa")))).isFalse();
    }

    @Test
    void trocarPrincipalIndicaOAnteriorParaDesmarcar() {
        Endereco casa = principal("casa");
        Endereco trabalho = endereco("trabalho");

        assertThat(RegraEnderecoPrincipal.principalASerSubstituido(List.of(casa, trabalho), trabalho))
                .containsSame(casa);
    }

    @Test
    void naoHaAnteriorQuandoOAlvoJaEOPrincipal() {
        Endereco casa = principal("casa");
        Endereco trabalho = endereco("trabalho");

        assertThat(RegraEnderecoPrincipal.principalASerSubstituido(List.of(casa, trabalho), casa)).isEmpty();
    }

    @Test
    void removidoOPrincipalOMaisAntigoRestanteAssume() {
        Endereco maisAntigo = endereco("antigo");
        Endereco maisNovo = endereco("novo");

        assertThat(RegraEnderecoPrincipal.sucessorAposRemocao(List.of(maisAntigo, maisNovo))).containsSame(maisAntigo);
    }

    @Test
    void semSucessorQuandoNaoSobramEnderecosOuJaHaPrincipal() {
        assertThat(RegraEnderecoPrincipal.sucessorAposRemocao(List.of())).isEmpty();
        assertThat(RegraEnderecoPrincipal.sucessorAposRemocao(List.of(endereco("a"), principal("b")))).isEmpty();
    }

    @Test
    void enderecoInativoNuncaEPrincipal() {
        Endereco e = principal("casa");

        e.inativar();

        assertThat(e.isAtivo()).isFalse();
        assertThat(e.isPrincipal()).isFalse();
    }

    @Test
    void normalizaCepEUf() {
        Endereco e = endereco("casa");

        assertThat(e.getCep()).isEqualTo("01310100");
        assertThat(e.getUf()).isEqualTo("SP");
        assertThat(Endereco.normalizarCep("01310100")).isEqualTo("01310100");
    }

    @Test
    void rejeitaCepEUfInvalidos() {
        assertThatThrownBy(() -> Endereco.normalizarCep("123")).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> new Endereco(1L, "x", "01310100", "r", "1", null, "b", "c", "XX"))
                .isInstanceOf(DadoInvalidoException.class);
    }

    @Test
    void ufAceitaAsVinteESeteSiglas() {
        assertThat(Uf.values()).hasSize(27);
        assertThat(Uf.da("rj")).contains(Uf.RJ);
        assertThat(Uf.da("XX")).isEmpty();
        assertThat(Uf.da(null)).isEmpty();
    }
}
