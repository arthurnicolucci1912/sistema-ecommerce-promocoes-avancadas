package br.com.fatec.ecommerce.estoque.aplicacao;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.fatec.ecommerce.TesteDeIntegracao;
import br.com.fatec.ecommerce.estoque.EstoqueParaTeste;
import br.com.fatec.ecommerce.estoque.dominio.ComponenteAReservar;
import br.com.fatec.ecommerce.seguranca.UsuariosParaTeste;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Liberacao, baixa e reposicao: efeito unico por pedido e sem erro quando nao ha reserva aplicavel (D4). */
@TesteDeIntegracao
class MovimentarReservasTests {

    private static final long CANECA = 101L;
    private static final long CAFE = 102L;

    @Autowired
    ReservarEstoque reservar;
    @Autowired
    MovimentarReservas movimentar;
    @Autowired
    EstoqueParaTeste estoque;
    @Autowired
    UsuariosParaTeste helper;

    @BeforeEach
    void preparar() {
        helper.limpar();
        estoque.definirSaldo(CANECA, 5, 0);
        estoque.definirSaldo(CAFE, 5, 0);
        reservar.para(1L, List.of(new ComponenteAReservar(CANECA, 2), new ComponenteAReservar(CAFE, 1)));
    }

    @Test
    void liberarDuasVezesDevolveAoDisponivelUmaUnicaVez() {
        movimentar.liberar(1L);
        movimentar.liberar(1L);

        assertThat(estoque.disponivel(CANECA)).isEqualTo(5);
        assertThat(estoque.reservado(CANECA)).isZero();
        assertThat(estoque.disponivel(CAFE)).isEqualTo(5);
        assertThat(estoque.situacoesDaReserva(1L)).containsOnly("LIBERADA");
    }

    @Test
    void baixaDiminuiOReservadoSemMudarODisponivelEEhIdempotente() {
        movimentar.baixar(1L);
        movimentar.baixar(1L);

        assertThat(estoque.reservado(CANECA)).isZero();
        assertThat(estoque.disponivel(CANECA)).isEqualTo(3);
        assertThat(estoque.reservado(CAFE)).isZero();
        assertThat(estoque.situacoesDaReserva(1L)).containsOnly("BAIXADA");
    }

    @Test
    void reporDesfazAUmaBaixaUmaUnicaVez() {
        movimentar.baixar(1L);

        movimentar.repor(1L);
        movimentar.repor(1L);

        assertThat(estoque.disponivel(CANECA)).isEqualTo(5);
        assertThat(estoque.disponivel(CAFE)).isEqualTo(5);
        assertThat(estoque.situacoesDaReserva(1L)).containsOnly("REPOSTA");
    }

    @Test
    void operacoesSemReservaAplicavelNaoMudamNada() {
        movimentar.repor(1L);
        movimentar.liberar(1L);
        movimentar.baixar(1L);
        movimentar.repor(1L);
        movimentar.liberar(1L);

        assertThat(estoque.disponivel(CANECA)).isEqualTo(5);
        assertThat(estoque.reservado(CANECA)).isZero();
        assertThat(estoque.situacoesDaReserva(1L)).containsOnly("LIBERADA");
    }

    @Test
    void pedidoSemReservaNaoMudaNadaEnaoLancaErro() {
        movimentar.liberar(999L);
        movimentar.baixar(999L);
        movimentar.repor(999L);

        assertThat(estoque.disponivel(CANECA)).isEqualTo(3);
        assertThat(estoque.reservado(CANECA)).isEqualTo(2);
        assertThat(estoque.situacoesDaReserva(999L)).isEmpty();
    }
}
