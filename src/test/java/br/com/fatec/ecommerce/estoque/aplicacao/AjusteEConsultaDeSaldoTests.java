package br.com.fatec.ecommerce.estoque.aplicacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fatec.ecommerce.TesteDeIntegracao;
import br.com.fatec.ecommerce.compartilhado.dominio.ConflitoException;
import br.com.fatec.ecommerce.compartilhado.dominio.DadoInvalidoException;
import br.com.fatec.ecommerce.compartilhado.dominio.RecursoNaoEncontradoException;
import br.com.fatec.ecommerce.estoque.EstoqueParaTeste;
import br.com.fatec.ecommerce.estoque.dominio.SaldoEstoque;
import br.com.fatec.ecommerce.seguranca.UsuariosParaTeste;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Ajuste administrativo (condicional, sem ficar negativo) e consultas de saldo. */
@TesteDeIntegracao
class AjusteEConsultaDeSaldoTests {

    @Autowired
    AjustarEstoque ajustar;
    @Autowired
    ConsultarSaldo consultar;
    @Autowired
    EstoqueParaTeste estoque;
    @Autowired
    UsuariosParaTeste helper;

    @BeforeEach
    void preparar() {
        helper.limpar();
    }

    @Test
    void entradaDeMercadoriaAumentaODisponivel() {
        estoque.definirSaldo(1L, 3, 0);

        SaldoEstoque saldo = ajustar.ajustar(1L, 10, "compra do fornecedor");

        assertThat(saldo.getDisponivel()).isEqualTo(13);
        assertThat(estoque.disponivel(1L)).isEqualTo(13);
    }

    @Test
    void saidaAteZeroEhPermitida() {
        estoque.definirSaldo(1L, 3, 0);

        assertThat(ajustar.ajustar(1L, -3, "perda").getDisponivel()).isZero();
    }

    @Test
    void ajusteQueDeixariaNegativoEhConflitoEOSaldoNaoMuda() {
        estoque.definirSaldo(1L, 3, 0);

        assertThatThrownBy(() -> ajustar.ajustar(1L, -5, "inventário")).isInstanceOf(ConflitoException.class);

        assertThat(estoque.disponivel(1L)).isEqualTo(3);
    }

    @Test
    void ajusteExigeMotivoEDeltaDiferenteDeZero() {
        estoque.definirSaldo(1L, 3, 0);

        assertThatThrownBy(() -> ajustar.ajustar(1L, 1, " ")).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> ajustar.ajustar(1L, 1, null)).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> ajustar.ajustar(1L, 0, "nada")).isInstanceOf(DadoInvalidoException.class);
        assertThat(estoque.disponivel(1L)).isEqualTo(3);
    }

    @Test
    void ajusteDeVariacaoSemSaldoEhNaoEncontradoENaoCriaSaldo() {
        assertThatThrownBy(() -> ajustar.ajustar(77L, 5, "entrada")).isInstanceOf(RecursoNaoEncontradoException.class);

        assertThat(estoque.existeSaldo(77L)).isFalse();
    }

    @Test
    void consultaPorVariacaoEPorVariacoes() {
        estoque.definirSaldo(1L, 3, 2);
        estoque.definirSaldo(2L, 8, 0);

        SaldoEstoque saldo = consultar.porVariacao(1L);
        assertThat(saldo.getDisponivel()).isEqualTo(3);
        assertThat(saldo.getReservado()).isEqualTo(2);
        assertThat(consultar.porVariacoes(List.of(2L, 1L, 99L))).extracting(SaldoEstoque::getVariacaoId)
                .containsExactly(1L, 2L);
        assertThat(consultar.porVariacoes(List.of())).isEmpty();
        assertThatThrownBy(() -> consultar.porVariacao(99L)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void estoqueBaixoListaApenasOsComDisponivelAbaixoDoLimite() {
        estoque.definirSaldo(1L, 4, 0);
        estoque.definirSaldo(2L, 5, 0);
        estoque.definirSaldo(3L, 0, 9);

        assertThat(consultar.abaixoDe(5)).extracting(SaldoEstoque::getVariacaoId).containsExactly(1L, 3L);
    }
}
