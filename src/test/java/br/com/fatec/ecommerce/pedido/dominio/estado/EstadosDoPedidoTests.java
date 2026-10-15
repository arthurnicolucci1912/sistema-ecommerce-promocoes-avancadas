package br.com.fatec.ecommerce.pedido.dominio.estado;

import static br.com.fatec.ecommerce.pedido.dominio.OperacaoPedido.AGUARDAR_PAGAMENTO;
import static br.com.fatec.ecommerce.pedido.dominio.OperacaoPedido.CANCELAR;
import static br.com.fatec.ecommerce.pedido.dominio.OperacaoPedido.CONFIRMAR_PAGAMENTO;
import static br.com.fatec.ecommerce.pedido.dominio.OperacaoPedido.ENTREGAR;
import static br.com.fatec.ecommerce.pedido.dominio.OperacaoPedido.ENVIAR;
import static br.com.fatec.ecommerce.pedido.dominio.OperacaoPedido.INICIAR_PREPARACAO;
import static br.com.fatec.ecommerce.pedido.dominio.OperacaoPedido.RECUSAR_PAGAMENTO;
import static br.com.fatec.ecommerce.pedido.dominio.SituacaoPedido.AGUARDANDO_PAGAMENTO;
import static br.com.fatec.ecommerce.pedido.dominio.SituacaoPedido.CANCELADO;
import static br.com.fatec.ecommerce.pedido.dominio.SituacaoPedido.CRIADO;
import static br.com.fatec.ecommerce.pedido.dominio.SituacaoPedido.EM_PREPARACAO;
import static br.com.fatec.ecommerce.pedido.dominio.SituacaoPedido.ENTREGUE;
import static br.com.fatec.ecommerce.pedido.dominio.SituacaoPedido.ENVIADO;
import static br.com.fatec.ecommerce.pedido.dominio.SituacaoPedido.PAGAMENTO_RECUSADO;
import static br.com.fatec.ecommerce.pedido.dominio.SituacaoPedido.PAGO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fatec.ecommerce.pedido.dominio.OperacaoPedido;
import br.com.fatec.ecommerce.pedido.dominio.Responsavel;
import br.com.fatec.ecommerce.pedido.dominio.SituacaoPedido;
import br.com.fatec.ecommerce.pedido.dominio.TransicaoInvalidaException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/** Os 8 estados x 7 operacoes: 11 transicoes validas levam ao estado certo; as demais lancam TransicaoInvalida. */
class EstadosDoPedidoTests {

    private static final Responsavel ADMIN = Responsavel.administrativo(1L);
    private static final Responsavel CLIENTE = Responsavel.cliente(2L);

    private static final Map<OperacaoPedido, Function<EstadoPedido, EstadoPedido>> OPERACOES =
            new EnumMap<>(OperacaoPedido.class);

    static {
        OPERACOES.put(AGUARDAR_PAGAMENTO, EstadoPedido::aguardarPagamento);
        OPERACOES.put(CONFIRMAR_PAGAMENTO, EstadoPedido::confirmarPagamento);
        OPERACOES.put(RECUSAR_PAGAMENTO, EstadoPedido::recusarPagamento);
        OPERACOES.put(INICIAR_PREPARACAO, EstadoPedido::iniciarPreparacao);
        OPERACOES.put(ENVIAR, EstadoPedido::enviar);
        OPERACOES.put(ENTREGAR, EstadoPedido::entregar);
        OPERACOES.put(CANCELAR, estado -> estado.cancelar(ADMIN));
    }

    /** As 11 transicoes validas (T1 a T11): estado, operacao e estado de destino. */
    private static final List<Object[]> VALIDAS = List.of(
            new Object[] {CRIADO, AGUARDAR_PAGAMENTO, AGUARDANDO_PAGAMENTO},
            new Object[] {AGUARDANDO_PAGAMENTO, CONFIRMAR_PAGAMENTO, PAGO},
            new Object[] {AGUARDANDO_PAGAMENTO, RECUSAR_PAGAMENTO, PAGAMENTO_RECUSADO},
            new Object[] {PAGO, INICIAR_PREPARACAO, EM_PREPARACAO},
            new Object[] {EM_PREPARACAO, ENVIAR, ENVIADO},
            new Object[] {ENVIADO, ENTREGAR, ENTREGUE},
            new Object[] {CRIADO, CANCELAR, CANCELADO},
            new Object[] {AGUARDANDO_PAGAMENTO, CANCELAR, CANCELADO},
            new Object[] {PAGAMENTO_RECUSADO, CANCELAR, CANCELADO},
            new Object[] {PAGO, CANCELAR, CANCELADO},
            new Object[] {EM_PREPARACAO, CANCELAR, CANCELADO});

    private static SituacaoPedido destinoValido(SituacaoPedido de, OperacaoPedido operacao) {
        return VALIDAS.stream().filter(v -> v[0] == de && v[1] == operacao)
                .map(v -> (SituacaoPedido) v[2]).findFirst().orElse(null);
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> todosOsPares() {
        return Stream.of(SituacaoPedido.values()).flatMap(situacao -> Stream.of(OperacaoPedido.values())
                .map(operacao -> org.junit.jupiter.params.provider.Arguments.of(situacao, operacao)));
    }

    @Test
    void hOnzeTransicoesValidasEmCinquentaESeisPares() {
        assertThat(todosOsPares().count()).isEqualTo(56);
        assertThat(VALIDAS).hasSize(11);
    }

    @ParameterizedTest(name = "{0} x {1}")
    @MethodSource("todosOsPares")
    void validasLevamAoProximoEstadoEDemaisLancamTransicaoInvalida(SituacaoPedido situacao, OperacaoPedido operacao) {
        EstadoPedido estado = situacao.estado();
        SituacaoPedido esperado = destinoValido(situacao, operacao);

        if (esperado != null) {
            assertThat(OPERACOES.get(operacao).apply(estado).situacao()).isEqualTo(esperado);
        } else {
            assertThatThrownBy(() -> OPERACOES.get(operacao).apply(estado))
                    .isInstanceOfSatisfying(TransicaoInvalidaException.class, e -> {
                        assertThat(e.getSituacaoAtual()).isEqualTo(situacao);
                        assertThat(e.getOperacao()).isEqualTo(operacao);
                        assertThat(e.getMessage()).contains(situacao.name(), operacao.name());
                    });
        }
    }

    @ParameterizedTest
    @EnumSource(SituacaoPedido.class)
    void cadaSituacaoConheceOProprioEstado(SituacaoPedido situacao) {
        assertThat(situacao.estado().situacao()).isEqualTo(situacao);
    }

    @ParameterizedTest
    @EnumSource(value = SituacaoPedido.class, names = {"PAGO", "EM_PREPARACAO"})
    void clienteNaoCancelaPedidoPagoOuEmPreparacao(SituacaoPedido situacao) {
        assertThatThrownBy(() -> situacao.estado().cancelar(CLIENTE))
                .isInstanceOfSatisfying(TransicaoInvalidaException.class, e -> {
                    assertThat(e.getSituacaoAtual()).isEqualTo(situacao);
                    assertThat(e.getOperacao()).isEqualTo(CANCELAR);
                });
        assertThatThrownBy(() -> situacao.estado().cancelar(Responsavel.sistema()))
                .isInstanceOf(TransicaoInvalidaException.class);
        assertThat(situacao.estado().cancelar(ADMIN).situacao()).isEqualTo(CANCELADO);
    }

    @ParameterizedTest
    @EnumSource(value = SituacaoPedido.class, names = {"CRIADO", "AGUARDANDO_PAGAMENTO", "PAGAMENTO_RECUSADO"})
    void clienteCancelaPedidoAindaNaoPago(SituacaoPedido situacao) {
        assertThat(situacao.estado().cancelar(CLIENTE).situacao()).isEqualTo(CANCELADO);
    }

    @ParameterizedTest
    @EnumSource(value = SituacaoPedido.class, names = {"ENTREGUE", "CANCELADO"})
    void estadosFinaisRejeitamTodasAsOperacoes(SituacaoPedido situacao) {
        for (OperacaoPedido operacao : OperacaoPedido.values()) {
            assertThatThrownBy(() -> OPERACOES.get(operacao).apply(situacao.estado()))
                    .isInstanceOf(TransicaoInvalidaException.class);
        }
    }
}
