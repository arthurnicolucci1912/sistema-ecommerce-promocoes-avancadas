package br.com.fatec.ecommerce.estoque.aplicacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fatec.ecommerce.TesteDeIntegracao;
import br.com.fatec.ecommerce.compartilhado.dominio.ConflitoException;
import br.com.fatec.ecommerce.compartilhado.dominio.DadoInvalidoException;
import br.com.fatec.ecommerce.estoque.EstoqueParaTeste;
import br.com.fatec.ecommerce.estoque.dominio.ComponenteAReservar;
import br.com.fatec.ecommerce.estoque.dominio.EstoqueInsuficienteException;
import br.com.fatec.ecommerce.seguranca.UsuariosParaTeste;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Reserva tudo-ou-nada por pedido (RN-21): sucesso, estoque insuficiente, reserva duplicada e agregacao. */
@TesteDeIntegracao
class ReservarEstoqueTests {

    private static final long CANECA = 101L;
    private static final long CAFE = 102L;

    @Autowired
    ReservarEstoque reservar;
    @Autowired
    EstoqueParaTeste estoque;
    @Autowired
    UsuariosParaTeste helper;

    @BeforeEach
    void preparar() {
        helper.limpar();
    }

    @Test
    void reservaBemSucedidaMoveDoDisponivelParaOReservado() {
        estoque.definirSaldo(CANECA, 5, 0);

        reservar.para(1L, List.of(new ComponenteAReservar(CANECA, 2)));

        assertThat(estoque.disponivel(CANECA)).isEqualTo(3);
        assertThat(estoque.reservado(CANECA)).isEqualTo(2);
        assertThat(estoque.situacoesDaReserva(1L)).containsExactly("ATIVA");
    }

    @Test
    void estoqueInsuficienteInformaOComponenteENaoAlteraNenhumSaldo() {
        estoque.definirSaldo(CANECA, 10, 0);
        estoque.definirSaldo(CAFE, 3, 0);

        assertThatThrownBy(() -> reservar.para(1L,
                List.of(new ComponenteAReservar(CANECA, 1), new ComponenteAReservar(CAFE, 4))))
                .isInstanceOfSatisfying(EstoqueInsuficienteException.class, e -> {
                    assertThat(e.getVariacaoId()).isEqualTo(CAFE);
                    assertThat(e.getDisponivel()).isEqualTo(3);
                    assertThat(e.tipo()).isEqualTo("estoque-insuficiente");
                    assertThat(e.propriedades()).containsEntry("variacaoId", CAFE).containsEntry("disponivel", 3);
                });

        assertThat(estoque.disponivel(CANECA)).isEqualTo(10);
        assertThat(estoque.reservado(CANECA)).isZero();
        assertThat(estoque.disponivel(CAFE)).isEqualTo(3);
        assertThat(estoque.reservado(CAFE)).isZero();
        assertThat(estoque.quantidadeDeReservas()).isZero();
    }

    @Test
    void variacaoSemSaldoEhInsuficienteComDisponivelZero() {
        assertThatThrownBy(() -> reservar.para(1L, List.of(new ComponenteAReservar(999L, 1))))
                .isInstanceOfSatisfying(EstoqueInsuficienteException.class, e -> {
                    assertThat(e.getVariacaoId()).isEqualTo(999L);
                    assertThat(e.getDisponivel()).isZero();
                });
    }

    @Test
    void reservarDuasVezesParaOMesmoPedidoEhRejeitadoESemEfeito() {
        estoque.definirSaldo(CANECA, 5, 0);
        reservar.para(1L, List.of(new ComponenteAReservar(CANECA, 2)));

        assertThatThrownBy(() -> reservar.para(1L, List.of(new ComponenteAReservar(CANECA, 1))))
                .isInstanceOf(ConflitoException.class)
                .isNotInstanceOf(EstoqueInsuficienteException.class);

        assertThat(estoque.disponivel(CANECA)).isEqualTo(3);
        assertThat(estoque.reservado(CANECA)).isEqualTo(2);
    }

    @Test
    void componentesRepetidosDaMesmaVariacaoSaoSomados() {
        estoque.definirSaldo(CANECA, 5, 0);

        reservar.para(1L, List.of(new ComponenteAReservar(CANECA, 2), new ComponenteAReservar(CANECA, 1)));

        assertThat(estoque.disponivel(CANECA)).isEqualTo(2);
        assertThat(estoque.reservado(CANECA)).isEqualTo(3);
        assertThat(estoque.situacoesDaReserva(1L)).containsExactly("ATIVA");
    }

    @Test
    void somaDosComponentesRepetidosAcimaDoDisponivelEhInsuficiente() {
        estoque.definirSaldo(CANECA, 2, 0);

        assertThatThrownBy(() -> reservar.para(1L,
                List.of(new ComponenteAReservar(CANECA, 2), new ComponenteAReservar(CANECA, 1))))
                .isInstanceOf(EstoqueInsuficienteException.class);

        assertThat(estoque.disponivel(CANECA)).isEqualTo(2);
    }

    @Test
    void componentesInvalidosSaoRejeitados() {
        assertThatThrownBy(() -> reservar.para(1L, List.of())).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> new ComponenteAReservar(CANECA, 0)).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> new ComponenteAReservar(null, 1)).isInstanceOf(DadoInvalidoException.class);
    }
}
