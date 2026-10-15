package br.com.fatec.ecommerce.pedido.aplicacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fatec.ecommerce.TesteDeIntegracao;
import br.com.fatec.ecommerce.cliente.ClientesParaTeste;
import br.com.fatec.ecommerce.cliente.ClientesParaTeste.ClienteDeTeste;
import br.com.fatec.ecommerce.pedido.PedidosParaTeste;
import br.com.fatec.ecommerce.pedido.dominio.Responsavel;
import br.com.fatec.ecommerce.pedido.dominio.SituacaoPedido;
import br.com.fatec.ecommerce.pedido.dominio.TransicaoInvalidaException;
import br.com.fatec.ecommerce.pedido.dominio.evento.PedidoCancelado;
import br.com.fatec.ecommerce.seguranca.UsuariosParaTeste;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;

/**
 * Observer: um ouvinte novo, declarado apenas neste teste, recebe o cancelamento sem nenhuma alteracao no modulo
 * pedido; uma transicao rejeitada nao publica nada.
 */
@TesteDeIntegracao
@Import(OuvinteDeCancelamentoTests.NovoInteressado.class)
class OuvinteDeCancelamentoTests {

    @TestConfiguration
    static class NovoInteressado {
        final List<PedidoCancelado> recebidos = new ArrayList<>();

        @EventListener
        void ao(PedidoCancelado evento) {
            recebidos.add(evento);
        }
    }

    private static final Responsavel ADMIN = Responsavel.administrativo(1L);

    @Autowired
    CriarPedido criarPedido;
    @Autowired
    ConfirmarReservaDoPedido confirmarReserva;
    @Autowired
    TransicionarPedido transicionar;
    @Autowired
    NovoInteressado interessado;
    @Autowired
    UsuariosParaTeste helper;
    @Autowired
    ClientesParaTeste clientesHelper;

    ClienteDeTeste cliente;

    @BeforeEach
    void preparar() {
        helper.limpar();
        cliente = clientesHelper.criar("cliente@demo.local", 32);
        interessado.recebidos.clear();
    }

    private Long pedidoPago() {
        Long id = criarPedido.executar(PedidosParaTeste.validos(cliente.cliente().getId())).getId();
        confirmarReserva.executar(id);
        transicionar.aprovarPagamento(id, Responsavel.sistema(), null);
        return id;
    }

    @Test
    void novoInteressadoRecebeOCancelamentoDePedidoPagoComEstadoAnteriorPago() {
        Long id = pedidoPago();

        transicionar.cancelar(id, ADMIN, "produto indisponível");

        assertThat(interessado.recebidos).singleElement().satisfies(evento -> {
            assertThat(evento.pedidoId()).isEqualTo(id);
            assertThat(evento.clienteId()).isEqualTo(cliente.cliente().getId());
            assertThat(evento.de()).isEqualTo(SituacaoPedido.PAGO);
            assertThat(evento.para()).isEqualTo(SituacaoPedido.CANCELADO);
            assertThat(evento.responsavel()).isEqualTo(ADMIN);
            assertThat(evento.motivo()).isEqualTo("produto indisponível");
        });
    }

    @Test
    void transicaoRejeitadaNaoPublicaNenhumEvento() {
        Long id = pedidoPago();
        transicionar.iniciarPreparacao(id, ADMIN);
        transicionar.enviar(id, ADMIN);
        transicionar.entregar(id, ADMIN);

        assertThatThrownBy(() -> transicionar.cancelar(id, ADMIN, "tarde demais"))
                .isInstanceOf(TransicaoInvalidaException.class);

        assertThat(interessado.recebidos).isEmpty();
    }

    @Test
    void cancelamentoRejeitadoPeloPerfilNaoPublicaNenhumEvento() {
        Long id = pedidoPago();

        assertThatThrownBy(() -> transicionar.cancelar(id, Responsavel.cliente(cliente.usuario().getId()), "x"))
                .isInstanceOf(TransicaoInvalidaException.class);

        assertThat(interessado.recebidos).isEmpty();
    }
}
