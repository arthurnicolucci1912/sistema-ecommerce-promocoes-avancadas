package br.com.fatec.ecommerce.promocao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fatec.ecommerce.compartilhado.dominio.ConflitoException;
import br.com.fatec.ecommerce.compartilhado.dominio.DadoInvalidoException;
import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import br.com.fatec.ecommerce.promocao.dominio.CondicaoConfig;
import br.com.fatec.ecommerce.promocao.dominio.Cupom;
import br.com.fatec.ecommerce.promocao.dominio.CupomRejeitadoException;
import br.com.fatec.ecommerce.promocao.dominio.ModoAcumulacao;
import br.com.fatec.ecommerce.promocao.dominio.Parametros;
import br.com.fatec.ecommerce.promocao.dominio.PromocaoConfig;
import br.com.fatec.ecommerce.promocao.dominio.SituacaoCupom;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Conversao exata dos parametros declarativos, configuracao de campanha/cupom e normalizacao de codigos. */
class ParametrosEConfiguracaoTests {

    private static final Instant INICIO = Instant.parse("2026-10-22T10:00:00Z");
    private static final Instant FIM = Instant.parse("2026-10-22T11:00:00Z");

    private static Map<String, Object> mapa(String chave, Object valor) {
        Map<String, Object> mapa = new HashMap<>();
        mapa.put(chave, valor);
        return mapa;
    }

    // --- Parametros.texto ---

    @Test
    void textoApaOsEspacosEExigeConteudo() {
        assertThat(Parametros.texto(Map.of("sku", "  ABC  "), "sku")).isEqualTo("ABC");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void textoVazioOuNuloEhInvalido(String valor) {
        assertThatThrownBy(() -> Parametros.texto(mapa("sku", valor), "sku")).isInstanceOf(DadoInvalidoException.class);
    }

    @Test
    void textoAusenteOuDeTipoErradoEhInvalido() {
        assertThatThrownBy(() -> Parametros.texto(Map.of(), "sku")).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> Parametros.texto(Map.of("sku", 10), "sku")).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> Parametros.texto(Map.of("sku", List.of("A")), "sku"))
                .isInstanceOf(DadoInvalidoException.class);
    }

    // --- Parametros.numero / id / inteiro ---

    @Test
    void numeroAceitaNumerosETextoNumericoEPreservaPrecisao() {
        assertThat(Parametros.numero(Map.of("n", 10), "n")).isEqualByComparingTo("10");
        assertThat(Parametros.numero(Map.of("n", 10.5), "n")).isEqualByComparingTo("10.5");
        assertThat(Parametros.numero(Map.of("n", "12.345"), "n")).isEqualByComparingTo("12.345");
        assertThat(Parametros.numero(Map.of("n", new BigDecimal("0.1")), "n")).isEqualByComparingTo("0.1");
        assertThat(Parametros.numero(Map.of("n", 5L), "n")).isEqualByComparingTo("5");
    }

    @Test
    void numeroInvalidoAusenteOuDeOutroTipoEhRejeitado() {
        assertThatThrownBy(() -> Parametros.numero(Map.of("n", "abc"), "n")).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> Parametros.numero(Map.of("n", ""), "n")).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> Parametros.numero(Map.of(), "n")).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> Parametros.numero(mapa("n", null), "n")).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> Parametros.numero(Map.of("n", true), "n")).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> Parametros.numero(Map.of("n", List.of(1)), "n"))
                .isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> Parametros.numero(Map.of("n", Map.of()), "n"))
                .isInstanceOf(DadoInvalidoException.class);
    }

    @Test
    void idExigeInteiroPositivo() {
        assertThat(Parametros.id(Map.of("id", 7), "id")).isEqualTo(7L);
        assertThat(Parametros.id(Map.of("id", "7"), "id")).isEqualTo(7L);
        assertThat(Parametros.id(Map.of("id", 7.0), "id")).isEqualTo(7L);
        assertThat(Parametros.id(Map.of("id", Long.MAX_VALUE), "id")).isEqualTo(Long.MAX_VALUE);
        for (Object invalido : List.of(0, -1, 1.5, "1.5", "x", Double.MAX_VALUE, new BigDecimal("9223372036854775808"))) {
            assertThatThrownBy(() -> Parametros.id(Map.of("id", invalido), "id"))
                    .as("id %s", invalido).isInstanceOf(DadoInvalidoException.class);
        }
    }

    @Test
    void inteiroRespeitaOLimiteDoTipoInt() {
        assertThat(Parametros.inteiro(Map.of("q", Integer.MAX_VALUE), "q")).isEqualTo(Integer.MAX_VALUE);
        assertThatThrownBy(() -> Parametros.inteiro(Map.of("q", 2147483648L), "q"))
                .isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> Parametros.inteiro(Map.of("q", 0), "q")).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> Parametros.inteiro(Map.of("q", -3), "q")).isInstanceOf(DadoInvalidoException.class);
    }

    @Test
    void mensagemDeErroNomeiaOCampoInvalido() {
        assertThatThrownBy(() -> Parametros.id(Map.of("categoriaId", 0), "categoriaId"))
                .hasMessageContaining("categoriaId");
        assertThat(Parametros.invalido("x").getMessage()).isEqualTo("Parâmetro inválido: x.");
    }

    // --- Parametros.dinheiro ---

    @Test
    void dinheiroAceitaZeroEValoresComAteDuasCasas() {
        assertThat(Parametros.dinheiro(Map.of("v", 0), "v")).isEqualTo(Dinheiro.zero());
        assertThat(Parametros.dinheiro(Map.of("v", "0.01"), "v")).isEqualTo(Dinheiro.deCentavos(1));
        assertThat(Parametros.dinheiro(Map.of("v", 59.9), "v")).isEqualTo(Dinheiro.de("59.90"));
        assertThat(Parametros.dinheiro(Map.of("v", "100.10"), "v")).isEqualTo(Dinheiro.de("100.10"));
        assertThat(Parametros.dinheiro(Map.of("v", "1E+2"), "v")).isEqualTo(Dinheiro.de("100"));
        assertThat(Parametros.dinheiro(Map.of("v", "10.100"), "v")).isEqualTo(Dinheiro.de("10.10"));
    }

    @Test
    void dinheiroRejeitaNegativoTerceiraCasaEValorGrandeDemais() {
        for (Object invalido : List.of(-1, "-0.01", "1.001", 0.001, "9999999999.99", "12345678901")) {
            if (invalido.equals("9999999999.99")) {
                // 10 digitos inteiros ainda cabem no limite (precision - scale <= 10).
                assertThat(Parametros.dinheiro(Map.of("v", invalido), "v")).isEqualTo(Dinheiro.de("9999999999.99"));
                continue;
            }
            assertThatThrownBy(() -> Parametros.dinheiro(Map.of("v", invalido), "v"))
                    .as("valor %s", invalido).isInstanceOf(DadoInvalidoException.class);
        }
    }

    // --- Parametros.percentual ---

    @Test
    void percentualVaiDeZeroExclusivoACemInclusivoEViraFracao() {
        assertThat(Parametros.percentual(Map.of("percentual", 100))).isEqualByComparingTo("1");
        assertThat(Parametros.percentual(Map.of("percentual", 10))).isEqualByComparingTo("0.1");
        assertThat(Parametros.percentual(Map.of("percentual", "0.01"))).isEqualByComparingTo("0.0001");
        assertThat(Parametros.percentual(Map.of("percentual", 12.5))).isEqualByComparingTo("0.125");
    }

    @Test
    void percentualForaDaFaixaOuAusenteEhInvalido() {
        for (Object invalido : List.of(0, -5, 100.01, 101, "abc", "0")) {
            assertThatThrownBy(() -> Parametros.percentual(Map.of("percentual", invalido)))
                    .as("percentual %s", invalido).isInstanceOf(DadoInvalidoException.class);
        }
        assertThatThrownBy(() -> Parametros.percentual(Map.of())).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> Parametros.percentual(mapa("percentual", null)))
                .isInstanceOf(DadoInvalidoException.class);
    }

    // --- CondicaoConfig ---

    @Test
    void condicaoConfigNormalizaNulosEImutabilizaColecoes() {
        var folha = new CondicaoConfig("PRIMEIRA_COMPRA", null, null);
        var origem = new HashMap<String, Object>();
        origem.put("a", 1);
        var filhos = new java.util.ArrayList<>(List.of(folha));
        var composto = new CondicaoConfig("NAO", origem, filhos);
        origem.put("b", 2);
        filhos.clear();

        assertThat(folha.parametros()).isEmpty();
        assertThat(folha.filhos()).isEmpty();
        assertThat(composto.parametros()).containsOnlyKeys("a");
        assertThat(composto.filhos()).hasSize(1);
        assertThatThrownBy(() -> composto.filhos().add(folha)).isInstanceOf(UnsupportedOperationException.class);
    }

    // --- PromocaoConfig ---

    private static PromocaoConfig campanha() {
        return new PromocaoConfig("  Black Friday  ", "FRETE_GRATIS", INICIO, FIM, 5, ModoAcumulacao.ACUMULAVEL,
                Map.of(), null, INICIO);
    }

    @Test
    void campanhaApaONomeENasceAtiva() {
        PromocaoConfig campanha = campanha();

        assertThat(campanha.nome()).isEqualTo("Black Friday");
        assertThat(campanha.ativo()).isTrue();
        assertThat(campanha.prioridade()).isEqualTo(5);
        assertThat(campanha.modo()).isEqualTo(ModoAcumulacao.ACUMULAVEL);
        assertThat(campanha.criadoEm()).isEqualTo(INICIO);
        assertThat(campanha.condicao()).isNull();
        assertThat(campanha.parametros()).isEmpty();
    }

    @Test
    void vigenciaIncluiOInicioEExcluiOFim() {
        PromocaoConfig campanha = campanha();

        assertThat(campanha.vigenteEm(INICIO.minusNanos(1))).isFalse();
        assertThat(campanha.vigenteEm(INICIO)).isTrue();
        assertThat(campanha.vigenteEm(FIM.minusNanos(1))).isTrue();
        assertThat(campanha.vigenteEm(FIM)).isFalse();
        assertThat(campanha.vigenteEm(FIM.plusSeconds(1))).isFalse();
    }

    @Test
    void campanhaDesativadaNuncaEstaVigenteEPodeSerReativada() {
        PromocaoConfig campanha = campanha();

        campanha.definirAtivo(false);
        assertThat(campanha.vigenteEm(INICIO.plusSeconds(1))).isFalse();
        campanha.definirAtivo(true);
        assertThat(campanha.vigenteEm(INICIO.plusSeconds(1))).isTrue();
    }

    @Test
    void periodoInvertidoOuIgualEhInvalido() {
        assertThatThrownBy(() -> new PromocaoConfig("N", "T", FIM, INICIO, 1, ModoAcumulacao.ACUMULAVEL, Map.of(),
                null, INICIO)).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> new PromocaoConfig("N", "T", INICIO, INICIO, 1, ModoAcumulacao.ACUMULAVEL, Map.of(),
                null, INICIO)).isInstanceOf(DadoInvalidoException.class);
    }

    @Test
    void camposObrigatoriosDaCampanha() {
        assertThatThrownBy(() -> new PromocaoConfig(null, "T", INICIO, FIM, 1, ModoAcumulacao.ACUMULAVEL, Map.of(),
                null, INICIO)).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> new PromocaoConfig(" ", "T", INICIO, FIM, 1, ModoAcumulacao.ACUMULAVEL, Map.of(),
                null, INICIO)).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> new PromocaoConfig("N", null, INICIO, FIM, 1, ModoAcumulacao.ACUMULAVEL, Map.of(),
                null, INICIO)).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> new PromocaoConfig("N", " ", INICIO, FIM, 1, ModoAcumulacao.ACUMULAVEL, Map.of(),
                null, INICIO)).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> new PromocaoConfig("N", "T", null, FIM, 1, ModoAcumulacao.ACUMULAVEL, Map.of(),
                null, INICIO)).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> new PromocaoConfig("N", "T", INICIO, null, 1, ModoAcumulacao.ACUMULAVEL, Map.of(),
                null, INICIO)).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> new PromocaoConfig("N", "T", INICIO, FIM, 1, null, Map.of(), null, INICIO))
                .isInstanceOf(DadoInvalidoException.class);
    }

    @Test
    void nomeDaCampanhaAte255CaracteresAposAparar() {
        assertThat(new PromocaoConfig(" " + "a".repeat(255) + " ", "T", INICIO, FIM, 1, ModoAcumulacao.ACUMULAVEL,
                Map.of(), null, INICIO).nome()).hasSize(255);
        assertThatThrownBy(() -> new PromocaoConfig("a".repeat(256), "T", INICIO, FIM, 1, ModoAcumulacao.ACUMULAVEL,
                Map.of(), null, INICIO)).isInstanceOf(DadoInvalidoException.class);
    }

    @Test
    void parametrosNulosViramMapaVazioEMapaDeEntradaEhCopiado() {
        Map<String, Object> origem = new HashMap<>(Map.of("a", 1));
        PromocaoConfig comParametros = new PromocaoConfig("N", "T", INICIO, FIM, 1, ModoAcumulacao.ACUMULAVEL, origem,
                null, INICIO);
        origem.put("b", 2);
        PromocaoConfig semParametros = new PromocaoConfig("N", "T", INICIO, FIM, 1, ModoAcumulacao.ACUMULAVEL, null,
                null, INICIO);

        assertThat(comParametros.parametros()).containsOnlyKeys("a");
        assertThat(semParametros.parametros()).isEmpty();
        assertThatThrownBy(() -> comParametros.parametros().put("x", 1)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void prioridadeNegativaOuZeroEhAceita() {
        assertThat(new PromocaoConfig("N", "T", INICIO, FIM, -10, ModoAcumulacao.ACUMULAVEL, Map.of(), null, INICIO)
                .prioridade()).isEqualTo(-10);
        assertThat(new PromocaoConfig("N", "T", INICIO, FIM, 0, ModoAcumulacao.EXCLUSIVA, Map.of(), null, INICIO)
                .modo()).isEqualTo(ModoAcumulacao.EXCLUSIVA);
    }

    @Test
    void alterarInvalidoNaoMudaNenhumCampoDaCampanha() {
        PromocaoConfig campanha = campanha();

        assertThatThrownBy(() -> campanha.alterar("Outro", "OUTRO_TIPO", FIM, INICIO, 99, ModoAcumulacao.EXCLUSIVA,
                Map.of("x", 1), null)).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> campanha.alterar("Outro", "OUTRO_TIPO", INICIO, FIM, 99, null, Map.of("x", 1), null))
                .isInstanceOf(DadoInvalidoException.class);

        assertThat(campanha.nome()).isEqualTo("Black Friday");
        assertThat(campanha.tipo()).isEqualTo("FRETE_GRATIS");
        assertThat(campanha.prioridade()).isEqualTo(5);
        assertThat(campanha.modo()).isEqualTo(ModoAcumulacao.ACUMULAVEL);
        assertThat(campanha.parametros()).isEmpty();
    }

    @Test
    void alterarValidoSubstituiTudoMasPreservaAtivacaoECriacao() {
        PromocaoConfig campanha = campanha();
        campanha.definirAtivo(false);

        campanha.alterar("Nova", "PERCENTUAL_CATEGORIA", FIM, FIM.plusSeconds(60), 1, ModoAcumulacao.EXCLUSIVA,
                Map.of("percentual", 10), null);

        assertThat(campanha.nome()).isEqualTo("Nova");
        assertThat(campanha.tipo()).isEqualTo("PERCENTUAL_CATEGORIA");
        assertThat(campanha.inicio()).isEqualTo(FIM);
        assertThat(campanha.modo()).isEqualTo(ModoAcumulacao.EXCLUSIVA);
        assertThat(campanha.ativo()).isFalse();
        assertThat(campanha.criadoEm()).isEqualTo(INICIO);
    }

    @Test
    void parametroComValorNuloNoMapaDaCampanhaNaoVira500() {
        // JSON {"parametros": {"sku": null}} chega como Map com valor nulo; Map.copyOf rejeitaria com NPE.
        Map<String, Object> comNulo = new HashMap<>();
        comNulo.put("sku", null);

        assertThatThrownBy(() -> new PromocaoConfig("N", "T", INICIO, FIM, 1, ModoAcumulacao.ACUMULAVEL, comNulo, null,
                INICIO)).isInstanceOf(DadoInvalidoException.class);
    }

    // --- Cupom ---

    private static Cupom cupom(String codigo) {
        return new Cupom(codigo, "Cupom", "PERCENTUAL", INICIO, FIM, true, 10, null, Map.of("percentual", 10), null,
                INICIO);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ABC", "abc", " abc ", "bemvindo10", "A-B", "A_B", "a1b2c3", "---", "___", "123"})
    void codigosValidosSaoNormalizadosParaMaiusculasSemEspacosExternos(String bruto) {
        assertThat(Cupom.normalizar(bruto)).isEqualTo(bruto.trim().toUpperCase());
        assertThat(cupom(bruto).codigo()).isEqualTo(bruto.trim().toUpperCase());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "AB", " AB ", "A B C", "ABC!", "AB.C", "ABC@", "CÓDIGO", "ÀBC", "A/B", "ABC\nDEF",
            "你好吗"})
    void codigosInvalidosSaoRejeitados(String bruto) {
        assertThatThrownBy(() -> Cupom.normalizar(bruto)).isInstanceOf(DadoInvalidoException.class);
    }

    @Test
    void codigoTemNoMinimo3ENoMaximo40Caracteres() {
        assertThat(Cupom.normalizar("A".repeat(40))).hasSize(40);
        assertThatThrownBy(() -> Cupom.normalizar("A".repeat(41))).isInstanceOf(DadoInvalidoException.class);
        assertThat(Cupom.normalizar("ABC")).isEqualTo("ABC");
        assertThatThrownBy(() -> Cupom.normalizar("AB")).isInstanceOf(DadoInvalidoException.class);
        // O limite vale para o codigo ja aparado.
        assertThat(Cupom.normalizar("  " + "a".repeat(40) + "  ")).hasSize(40);
    }

    @Test
    void codigoComLetraNaoAsciiQueMaiusculaParaAsciiDeveSerRejeitado() {
        assertThatThrownBy(() -> Cupom.normalizar("ßaa")).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> Cupom.normalizar("ﬁxo")).isInstanceOf(DadoInvalidoException.class);
    }

    @Test
    void cupomInvalidoNaoEhCriadoLimitesNaoPositivos() {
        assertThatThrownBy(() -> new Cupom("ABC", "N", "PERCENTUAL", INICIO, FIM, true, 0, null, Map.of(), null, INICIO))
                .isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> new Cupom("ABC", "N", "PERCENTUAL", INICIO, FIM, true, -1, null, Map.of(), null, INICIO))
                .isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> new Cupom("ABC", "N", "PERCENTUAL", INICIO, FIM, true, 1, 0, Map.of(), null, INICIO))
                .isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> new Cupom("ABC", "N", "PERCENTUAL", INICIO, FIM, true, 1, -2, Map.of(), null, INICIO))
                .isInstanceOf(DadoInvalidoException.class);
    }

    @Test
    void cupomAceitaLimiteIndividualNuloEMaiorQueOTotal() {
        Cupom semLimite = new Cupom("ABC", "N", "PERCENTUAL", INICIO, FIM, false, 1, null, Map.of(), null, INICIO);
        Cupom maiorQueTotal = new Cupom("ABC", "N", "PERCENTUAL", INICIO, FIM, false, 1, 5, Map.of(), null, INICIO);

        assertThat(semLimite.limiteCliente()).isNull();
        assertThat(semLimite.acumulavel()).isFalse();
        assertThat(maiorQue(maiorQueTotal)).isTrue();
    }

    private static boolean maiorQue(Cupom cupom) {
        return cupom.limiteCliente() > cupom.limiteTotal();
    }

    @Test
    void cupomTemMesmaVigenciaSemiAbertaDaCampanha() {
        Cupom cupom = cupom("ABC");

        assertThat(cupom.vigenteEm(INICIO.minusNanos(1))).isFalse();
        assertThat(cupom.vigenteEm(INICIO)).isTrue();
        assertThat(cupom.vigenteEm(FIM)).isFalse();
        cupom.definirAtivo(false);
        assertThat(cupom.vigenteEm(INICIO)).isFalse();
    }

    @Test
    void alterarCupomValidaNomePeriodoELimitesSemTocarNoCodigo() {
        Cupom cupom = cupom("abc");

        assertThatThrownBy(() -> cupom.alterar(" ", "PERCENTUAL", INICIO, FIM, true, 5, null, Map.of(), null))
                .isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> cupom.alterar("N", "PERCENTUAL", FIM, INICIO, true, 5, null, Map.of(), null))
                .isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> cupom.alterar("N", "PERCENTUAL", INICIO, FIM, true, 0, null, Map.of(), null))
                .isInstanceOf(DadoInvalidoException.class);
        assertThat(cupom.nome()).isEqualTo("Cupom");
        assertThat(cupom.limiteTotal()).isEqualTo(10);

        cupom.alterar("Novo nome", "VALOR_FIXO", INICIO, FIM, false, 5, 2, Map.of("valor", 5), null);

        assertThat(cupom.codigo()).isEqualTo("ABC");
        assertThat(cupom.nome()).isEqualTo("Novo nome");
        assertThat(cupom.tipo()).isEqualTo("VALOR_FIXO");
        assertThat(cupom.acumulavel()).isFalse();
        assertThat(cupom.limiteTotal()).isEqualTo(5);
        assertThat(cupom.limiteCliente()).isEqualTo(2);
    }

    @Test
    void cupomApaNomeELimitaA255() {
        assertThat(new Cupom("ABC", "  Nome  ", "P", INICIO, FIM, true, 1, null, Map.of(), null, INICIO).nome())
                .isEqualTo("Nome");
        assertThatThrownBy(() -> new Cupom("ABC", "n".repeat(256), "P", INICIO, FIM, true, 1, null, Map.of(), null,
                INICIO)).isInstanceOf(DadoInvalidoException.class);
    }

    // --- SituacaoCupom / CupomRejeitadoException ---

    @Test
    void situacaoDeCupomSoEhValidaSemMotivo() {
        assertThat(new SituacaoCupom("X", 1L, 0L, null).valido()).isTrue();
        assertThat(new SituacaoCupom("X", 1L, 0L, "ESGOTADO").valido()).isFalse();
    }

    @Test
    void cupomRejeitadoEhConflitoComMotivoEstavelEmPropriedades() {
        var erro = new CupomRejeitadoException("FORA_DA_VIGENCIA");

        assertThat(erro).isInstanceOf(ConflitoException.class);
        assertThat(erro.tipo()).isEqualTo("cupom-rejeitado");
        assertThat(erro.propriedades()).containsEntry("motivo", "FORA_DA_VIGENCIA");
        assertThat(erro.getMessage()).contains("FORA_DA_VIGENCIA");
    }
}
