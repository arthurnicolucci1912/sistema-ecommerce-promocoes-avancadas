package br.com.fatec.ecommerce.relatorio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fatec.ecommerce.compartilhado.dominio.DadoInvalidoException;
import br.com.fatec.ecommerce.relatorio.dominio.FiltroProdutosMaisVendidos;
import br.com.fatec.ecommerce.relatorio.dominio.PeriodoRelatorio;
import br.com.fatec.ecommerce.relatorio.dominio.ResumoVendas;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RelatoriosDominioTests {
    static final Instant T0 = Instant.parse("2026-10-01T00:00:00Z");
    static final Instant T1 = Instant.parse("2026-10-31T23:59:59Z");

    @Test void periodoExigeAsDuasPontasENaoAceitaIntervaloInvertido() {
        assertThatThrownBy(() -> new PeriodoRelatorio(null, T1)).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> new PeriodoRelatorio(T0, null)).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> new PeriodoRelatorio(T1, T0)).isInstanceOf(DadoInvalidoException.class);
    }

    @Test void periodoComInicioIgualAoFimEAceito() {
        assertThatCode(() -> new PeriodoRelatorio(T0, T0)).doesNotThrowAnyException();
    }

    @ParameterizedTest @ValueSource(ints = {1, 10, 50})
    void limiteDentroDaFaixaEAceito(int limite) {
        assertThat(new FiltroProdutosMaisVendidos(new PeriodoRelatorio(T0, T1), limite).limite()).isEqualTo(limite);
    }

    @ParameterizedTest @ValueSource(ints = {Integer.MIN_VALUE, -1, 0, 51, 1000})
    void limiteForaDaFaixaEhRejeitado(int limite) {
        var periodo = new PeriodoRelatorio(T0, T1);
        assertThatThrownBy(() -> new FiltroProdutosMaisVendidos(periodo, limite)).isInstanceOf(DadoInvalidoException.class);
    }

    @Test void filtroDeProdutosExigePeriodo() {
        assertThatThrownBy(() -> new FiltroProdutosMaisVendidos(null, 10)).isInstanceOf(DadoInvalidoException.class);
    }

    @Test void ticketMedioEhTotalSobreQuantidadeComArredondamentoBancario() {
        var resumo = ResumoVendas.de(new BigDecimal("615.00"), 2, new BigDecimal("30"), 3);
        assertThat(resumo.ticketMedio()).isEqualByComparingTo("307.50");
        assertThat(resumo.valorTotalVendas()).isEqualByComparingTo("615.00");
        assertThat(resumo.quantidadeCancelamentos()).isEqualTo(3);
        // 100.01 / 2 = 50.005 -> HALF_EVEN = 50.00 ; 100.03 / 2 = 50.015 -> 50.02
        assertThat(ResumoVendas.de(new BigDecimal("100.01"), 2, BigDecimal.ZERO, 0).ticketMedio()).isEqualByComparingTo("50.00");
        assertThat(ResumoVendas.de(new BigDecimal("100.03"), 2, BigDecimal.ZERO, 0).ticketMedio()).isEqualByComparingTo("50.02");
    }

    @Test void semVendasTudoEhZero() {
        var resumo = ResumoVendas.de(BigDecimal.ZERO, 0, BigDecimal.ZERO, 0);
        assertThat(resumo.ticketMedio()).isEqualByComparingTo("0.00");
        assertThat(resumo.valorTotalVendas()).isEqualByComparingTo("0");
        assertThat(resumo.quantidadePedidos()).isZero();
    }
}
