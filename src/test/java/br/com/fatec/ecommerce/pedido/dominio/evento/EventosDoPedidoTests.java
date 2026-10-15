package br.com.fatec.ecommerce.pedido.dominio.evento;

import static br.com.fatec.ecommerce.pedido.PedidosParaTeste.ADMIN;
import static br.com.fatec.ecommerce.pedido.PedidosParaTeste.CLIENTE;
import static br.com.fatec.ecommerce.pedido.PedidosParaTeste.MOMENTO;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.fatec.ecommerce.compartilhado.dominio.EventoDominio;
import br.com.fatec.ecommerce.pedido.PedidosParaTeste;
import br.com.fatec.ecommerce.pedido.dominio.Pedido;
import br.com.fatec.ecommerce.pedido.dominio.Responsavel;
import br.com.fatec.ecommerce.pedido.dominio.SituacaoPedido;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.util.ReflectionTestUtils;

/** Cada uma das 11 transicoes validas produz o evento esperado, com pedido, cliente, de, para e momento. */
class EventosDoPedidoTests {

    private static final Long CLIENTE_ID = 7L;
    private static final Long PEDIDO_ID = 99L;
    private static final Instant DEPOIS = MOMENTO.plusSeconds(30);

    private static Arguments caso(SituacaoPedido de, SituacaoPedido para, Consumer<Pedido> operacao,
            Class<? extends EventoDominio> tipo) {
        return Arguments.of(de, para, operacao, tipo);
    }

    static Stream<Arguments> transicoesValidas() {
        Responsavel sistema = Responsavel.sistema();
        return Stream.of(
                caso(SituacaoPedido.CRIADO, SituacaoPedido.AGUARDANDO_PAGAMENTO,
                        p -> p.aguardarPagamento(sistema, DEPOIS), PedidoCriado.class),
                caso(SituacaoPedido.AGUARDANDO_PAGAMENTO, SituacaoPedido.PAGO,
                        p -> p.confirmarPagamento(sistema, null, DEPOIS), PedidoPago.class),
                caso(SituacaoPedido.AGUARDANDO_PAGAMENTO, SituacaoPedido.PAGAMENTO_RECUSADO,
                        p -> p.recusarPagamento(sistema, null, DEPOIS), PedidoPagamentoRecusado.class),
                caso(SituacaoPedido.PAGO, SituacaoPedido.EM_PREPARACAO,
                        p -> p.iniciarPreparacao(ADMIN, DEPOIS), PedidoEmPreparacao.class),
                caso(SituacaoPedido.EM_PREPARACAO, SituacaoPedido.ENVIADO,
                        p -> p.enviar(ADMIN, DEPOIS), PedidoEnviado.class),
                caso(SituacaoPedido.ENVIADO, SituacaoPedido.ENTREGUE,
                        p -> p.entregar(ADMIN, DEPOIS), PedidoEntregue.class),
                caso(SituacaoPedido.CRIADO, SituacaoPedido.CANCELADO,
                        p -> p.cancelar(CLIENTE, "desistência", DEPOIS), PedidoCancelado.class),
                caso(SituacaoPedido.AGUARDANDO_PAGAMENTO, SituacaoPedido.CANCELADO,
                        p -> p.cancelar(CLIENTE, "desistência", DEPOIS), PedidoCancelado.class),
                caso(SituacaoPedido.PAGAMENTO_RECUSADO, SituacaoPedido.CANCELADO,
                        p -> p.cancelar(CLIENTE, "desistência", DEPOIS), PedidoCancelado.class),
                caso(SituacaoPedido.PAGO, SituacaoPedido.CANCELADO,
                        p -> p.cancelar(ADMIN, "desistência", DEPOIS), PedidoCancelado.class),
                caso(SituacaoPedido.EM_PREPARACAO, SituacaoPedido.CANCELADO,
                        p -> p.cancelar(ADMIN, "desistência", DEPOIS), PedidoCancelado.class));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("transicoesValidas")
    void transicaoValidaProduzOEventoEsperado(SituacaoPedido de, SituacaoPedido para, Consumer<Pedido> operacao,
            Class<? extends EventoDominio> tipo) {
        Pedido pedido = PedidosParaTeste.emSituacao(de, CLIENTE_ID);
        ReflectionTestUtils.setField(pedido, "id", PEDIDO_ID);

        operacao.accept(pedido);

        List<EventoDominio> eventos = pedido.consumirEventos();
        assertThat(eventos).hasSize(1);
        EventoDominio evento = eventos.get(0);
        assertThat(evento).isInstanceOf(tipo);
        assertThat(evento.ocorridoEm()).isEqualTo(DEPOIS);
        assertThat(campos(evento)).containsExactly(PEDIDO_ID, CLIENTE_ID, de, para);
    }

    @Test
    void eventoDeCancelamentoLevaResponsavelEMotivo() {
        Pedido pedido = PedidosParaTeste.emSituacao(SituacaoPedido.AGUARDANDO_PAGAMENTO, CLIENTE_ID);

        pedido.cancelar(CLIENTE, "não quero mais", DEPOIS);

        assertThat(pedido.consumirEventos()).singleElement().isInstanceOfSatisfying(PedidoCancelado.class, e -> {
            assertThat(e.responsavel()).isEqualTo(CLIENTE);
            assertThat(e.motivo()).isEqualTo("não quero mais");
        });
    }

    private static List<Object> campos(EventoDominio evento) {
        return switch (evento) {
            case PedidoCriado e -> List.of(e.pedidoId(), e.clienteId(), e.de(), e.para());
            case PedidoPago e -> List.of(e.pedidoId(), e.clienteId(), e.de(), e.para());
            case PedidoPagamentoRecusado e -> List.of(e.pedidoId(), e.clienteId(), e.de(), e.para());
            case PedidoEmPreparacao e -> List.of(e.pedidoId(), e.clienteId(), e.de(), e.para());
            case PedidoEnviado e -> List.of(e.pedidoId(), e.clienteId(), e.de(), e.para());
            case PedidoEntregue e -> List.of(e.pedidoId(), e.clienteId(), e.de(), e.para());
            case PedidoCancelado e -> List.of(e.pedidoId(), e.clienteId(), e.de(), e.para());
            default -> throw new IllegalArgumentException("evento inesperado: " + evento);
        };
    }
}
