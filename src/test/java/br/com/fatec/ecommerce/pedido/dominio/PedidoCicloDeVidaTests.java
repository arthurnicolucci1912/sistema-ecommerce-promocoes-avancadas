package br.com.fatec.ecommerce.pedido.dominio;

import static br.com.fatec.ecommerce.pedido.PedidosParaTeste.ADMIN;
import static br.com.fatec.ecommerce.pedido.PedidosParaTeste.CLIENTE;
import static br.com.fatec.ecommerce.pedido.PedidosParaTeste.MOMENTO;
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

import br.com.fatec.ecommerce.compartilhado.dominio.ConflitoException;
import br.com.fatec.ecommerce.compartilhado.dominio.DadoInvalidoException;
import br.com.fatec.ecommerce.compartilhado.dominio.EventoDominio;
import br.com.fatec.ecommerce.pedido.PedidosParaTeste;
import br.com.fatec.ecommerce.pedido.dominio.evento.PedidoCancelado;
import br.com.fatec.ecommerce.pedido.dominio.evento.PedidoCriado;
import br.com.fatec.ecommerce.pedido.dominio.evento.PedidoEmPreparacao;
import br.com.fatec.ecommerce.pedido.dominio.evento.PedidoEntregue;
import br.com.fatec.ecommerce.pedido.dominio.evento.PedidoEnviado;
import br.com.fatec.ecommerce.pedido.dominio.evento.PedidoPagamentoRecusado;
import br.com.fatec.ecommerce.pedido.dominio.evento.PedidoPago;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Pedido em memoria (State + Observer): matriz completa de transicoes invalidas, quem pode cancelar em cada estado,
 * motivo do cancelamento, historico encadeado e eventos acumulados. Complementa PedidoTests e EstadosDoPedidoTests,
 * que exercitam os objetos de estado isolados.
 */
class PedidoCicloDeVidaTests {

    private static final Long CLIENTE_ID = 7L;
    private static final Instant T1 = Instant.parse("2026-02-01T10:00:00Z");
    private static final Responsavel SISTEMA = Responsavel.sistema();

    private enum Op {
        AGUARDAR(OperacaoPedido.AGUARDAR_PAGAMENTO, (p, r) -> p.aguardarPagamento(r, T1)),
        CONFIRMAR(OperacaoPedido.CONFIRMAR_PAGAMENTO, (p, r) -> p.confirmarPagamento(r, "ok", T1)),
        RECUSAR(OperacaoPedido.RECUSAR_PAGAMENTO, (p, r) -> p.recusarPagamento(r, "negado", T1)),
        PREPARAR(OperacaoPedido.INICIAR_PREPARACAO, (p, r) -> p.iniciarPreparacao(r, T1)),
        ENVIAR(OperacaoPedido.ENVIAR, (p, r) -> p.enviar(r, T1)),
        ENTREGAR(OperacaoPedido.ENTREGAR, (p, r) -> p.entregar(r, T1)),
        CANCELAR(OperacaoPedido.CANCELAR, (p, r) -> p.cancelar(r, "motivo", T1));

        final OperacaoPedido operacao;
        final BiConsumer<Pedido, Responsavel> acao;

        Op(OperacaoPedido operacao, BiConsumer<Pedido, Responsavel> acao) {
            this.operacao = operacao;
            this.acao = acao;
        }
    }

    private static final Set<String> VALIDAS = Set.of(
            CRIADO + "/AGUARDAR", AGUARDANDO_PAGAMENTO + "/CONFIRMAR", AGUARDANDO_PAGAMENTO + "/RECUSAR",
            PAGO + "/PREPARAR", EM_PREPARACAO + "/ENVIAR", ENVIADO + "/ENTREGAR");

    private static final Set<SituacaoPedido> CANCELAVEIS_POR_QUALQUER =
            EnumSet.of(CRIADO, AGUARDANDO_PAGAMENTO, PAGAMENTO_RECUSADO);
    private static final Set<SituacaoPedido> CANCELAVEIS_SO_ADMIN = EnumSet.of(PAGO, EM_PREPARACAO);

    static Stream<Arguments> operacoesInvalidas() {
        return Stream.of(SituacaoPedido.values()).flatMap(situacao -> Stream.of(Op.values())
                .filter(op -> op != Op.CANCELAR)
                .filter(op -> !VALIDAS.contains(situacao + "/" + op))
                .map(op -> Arguments.of(situacao, op)));
    }

    static Stream<Arguments> cancelamentoPorResponsavel() {
        return Stream.of(SituacaoPedido.values()).flatMap(situacao -> Stream.of(
                Arguments.of(situacao, "CLIENTE", CANCELAVEIS_POR_QUALQUER.contains(situacao)),
                Arguments.of(situacao, "SISTEMA", CANCELAVEIS_POR_QUALQUER.contains(situacao)),
                Arguments.of(situacao, "ADMINISTRATIVO", CANCELAVEIS_POR_QUALQUER.contains(situacao)
                        || CANCELAVEIS_SO_ADMIN.contains(situacao))));
    }

    static Stream<Arguments> estadosEMotivosVazios() {
        return Stream.of(SituacaoPedido.values()).flatMap(situacao -> Stream.of(null, "", "   ", "\t\n")
                .map(motivo -> Arguments.of(situacao, motivo)));
    }

    private static Responsavel responsavel(String tipo) {
        return switch (tipo) {
            case "CLIENTE" -> CLIENTE;
            case "ADMINISTRATIVO" -> ADMIN;
            default -> SISTEMA;
        };
    }

    // --- Transicoes invalidas: nada muda ---

    @ParameterizedTest(name = "{0} rejeita {1} sem efeito colateral")
    @MethodSource("operacoesInvalidas")
    void operacaoInvalidaLancaTransicaoInvalidaENaoAlteraPedidoHistoricoNemEventos(SituacaoPedido situacao, Op op) {
        Pedido pedido = PedidosParaTeste.emSituacao(situacao, CLIENTE_ID);
        int tamanhoHistorico = pedido.getHistorico().size();

        assertThatThrownBy(() -> op.acao.accept(pedido, ADMIN))
                .isInstanceOf(ConflitoException.class)
                .isInstanceOfSatisfying(TransicaoInvalidaException.class, e -> {
                    assertThat(e.getSituacaoAtual()).isEqualTo(situacao);
                    assertThat(e.getOperacao()).isEqualTo(op.operacao);
                    assertThat(e.getMessage()).contains(situacao.name());
                });

        assertThat(pedido.getSituacao()).isEqualTo(situacao);
        assertThat(pedido.getHistorico()).hasSize(tamanhoHistorico);
        assertThat(pedido.consumirEventos()).isEmpty();
    }

    @ParameterizedTest(name = "excecao de {0} no estado ENTREGUE expoe situacao e operacao")
    @EnumSource(Op.class)
    void excecaoExpoeSituacaoEOperacaoParaARespostaDeErro(Op op) {
        Pedido entregue = PedidosParaTeste.emSituacao(ENTREGUE, CLIENTE_ID);
        assertThatThrownBy(() -> op.acao.accept(entregue, ADMIN))
                .isInstanceOfSatisfying(TransicaoInvalidaException.class, e -> {
                    assertThat(e.getOperacao()).isEqualTo(op.operacao);
                    assertThat(e.propriedades()).containsEntry("situacaoAtual", ENTREGUE)
                            .containsEntry("operacao", op.operacao);
                    assertThat(e.tipo()).isEqualTo("transicao-invalida");
                });
    }

    // --- Cancelamento: quem pode, em que estado ---

    @ParameterizedTest(name = "{0} cancelado por {1}: permitido={2}")
    @MethodSource("cancelamentoPorResponsavel")
    void cancelamentoObedeceAoEstadoEAoPerfilDoResponsavel(SituacaoPedido situacao, String tipo, boolean permitido) {
        Pedido pedido = PedidosParaTeste.emSituacao(situacao, CLIENTE_ID);
        int tamanho = pedido.getHistorico().size();
        Responsavel quem = responsavel(tipo);

        if (permitido) {
            pedido.cancelar(quem, "mudou de ideia", T1);
            assertThat(pedido.getSituacao()).isEqualTo(CANCELADO);
            assertThat(pedido.getHistorico()).hasSize(tamanho + 1);
            EntradaHistorico ultima = pedido.getHistorico().getLast();
            assertThat(ultima.getDe()).isEqualTo(situacao);
            assertThat(ultima.getPara()).isEqualTo(CANCELADO);
            assertThat(ultima.getResponsavel()).isEqualTo(quem);
            assertThat(ultima.getMotivo()).isEqualTo("mudou de ideia");
            assertThat(pedido.consumirEventos()).singleElement().isInstanceOfSatisfying(PedidoCancelado.class, e -> {
                assertThat(e.de()).isEqualTo(situacao);
                assertThat(e.para()).isEqualTo(CANCELADO);
                assertThat(e.responsavel()).isEqualTo(quem);
                assertThat(e.motivo()).isEqualTo("mudou de ideia");
            });
        } else {
            assertThatThrownBy(() -> pedido.cancelar(quem, "mudou de ideia", T1))
                    .isInstanceOf(TransicaoInvalidaException.class);
            assertThat(pedido.getSituacao()).isEqualTo(situacao);
            assertThat(pedido.getHistorico()).hasSize(tamanho);
            assertThat(pedido.consumirEventos()).isEmpty();
        }
    }

    @Test
    void mensagemDeCancelamentoRestritoCitaOEstadoEOPerfilExigido() {
        Pedido pago = PedidosParaTeste.emSituacao(PAGO, CLIENTE_ID);
        assertThatThrownBy(() -> pago.cancelar(CLIENTE, "x", T1))
                .isInstanceOfSatisfying(TransicaoInvalidaException.class, e -> {
                    assertThat(e.getMessage()).contains("PAGO").contains("administrativo");
                    assertThat(e.getOperacao()).isEqualTo(OperacaoPedido.CANCELAR);
                });
    }

    // --- Motivo ---

    @ParameterizedTest(name = "cancelar {0} com motivo [{1}] lanca DadoInvalido")
    @MethodSource("estadosEMotivosVazios")
    void motivoVazioEhRejeitadoEmQualquerEstadoAntesDeConsultarOEstado(SituacaoPedido situacao, String motivo) {
        Pedido pedido = PedidosParaTeste.emSituacao(situacao, CLIENTE_ID);
        int tamanho = pedido.getHistorico().size();
        assertThatThrownBy(() -> pedido.cancelar(ADMIN, motivo, T1)).isInstanceOf(DadoInvalidoException.class);
        assertThat(pedido.getSituacao()).isEqualTo(situacao);
        assertThat(pedido.getHistorico()).hasSize(tamanho);
        assertThat(pedido.consumirEventos()).isEmpty();
    }

    @Test
    void motivoVazioTemPrecedenciaSobreARegraDeQuemPodeCancelar() {
        Pedido pago = PedidosParaTeste.emSituacao(PAGO, CLIENTE_ID);
        assertThatThrownBy(() -> pago.cancelar(CLIENTE, " ", T1)).isInstanceOf(DadoInvalidoException.class);
    }

    @Test
    void motivoDoCancelamentoEAparadoNoHistorico() {
        Pedido pedido = PedidosParaTeste.emSituacao(AGUARDANDO_PAGAMENTO, CLIENTE_ID);
        pedido.cancelar(CLIENTE, "  desistência da compra  ", T1);
        assertThat(pedido.getHistorico().getLast().getMotivo()).isEqualTo("desistência da compra");
    }

    @Test
    void motivoOpcionalVazioViraNuloNoHistorico() {
        Pedido pedido = PedidosParaTeste.emSituacao(AGUARDANDO_PAGAMENTO, CLIENTE_ID);
        pedido.confirmarPagamento(SISTEMA, "   ", T1);
        assertThat(pedido.getHistorico().getLast().getMotivo()).isNull();
    }

    @Test
    void motivoDeAprovacaoERecusaEhRegistradoNoHistorico() {
        Pedido aprovado = PedidosParaTeste.emSituacao(AGUARDANDO_PAGAMENTO, CLIENTE_ID);
        aprovado.confirmarPagamento(SISTEMA, "pagamento simulado: pix / TX-1", T1);
        assertThat(aprovado.getHistorico().getLast().getMotivo()).isEqualTo("pagamento simulado: pix / TX-1");

        Pedido recusado = PedidosParaTeste.emSituacao(AGUARDANDO_PAGAMENTO, CLIENTE_ID);
        recusado.recusarPagamento(SISTEMA, "saldo insuficiente", T1);
        assertThat(recusado.getHistorico().getLast().getMotivo()).isEqualTo("saldo insuficiente");
    }

    // --- Historico ---

    @Test
    void historicoDoFluxoCompletoEEncadeadoCadaOrigemEIgualAoDestinoAnterior() {
        Pedido pedido = Pedido.criar(PedidosParaTeste.validos(CLIENTE_ID), MOMENTO);
        pedido.aguardarPagamento(SISTEMA, MOMENTO.plusSeconds(1));
        pedido.confirmarPagamento(SISTEMA, null, MOMENTO.plusSeconds(2));
        pedido.iniciarPreparacao(ADMIN, MOMENTO.plusSeconds(3));
        pedido.enviar(ADMIN, MOMENTO.plusSeconds(4));
        pedido.entregar(ADMIN, MOMENTO.plusSeconds(5));

        List<EntradaHistorico> historico = pedido.getHistorico();
        assertThat(historico).extracting(EntradaHistorico::getPara)
                .containsExactly(CRIADO, AGUARDANDO_PAGAMENTO, PAGO, EM_PREPARACAO, ENVIADO, ENTREGUE);
        assertThat(historico.getFirst().getDe()).isNull();
        for (int i = 1; i < historico.size(); i++) {
            assertThat(historico.get(i).getDe()).isEqualTo(historico.get(i - 1).getPara());
            assertThat(historico.get(i).getOcorridoEm()).isAfter(historico.get(i - 1).getOcorridoEm());
        }
        assertThat(historico).extracting(h -> h.getResponsavel().tipo()).containsExactly(TipoResponsavel.SISTEMA,
                TipoResponsavel.SISTEMA, TipoResponsavel.SISTEMA, TipoResponsavel.ADMINISTRATIVO,
                TipoResponsavel.ADMINISTRATIVO, TipoResponsavel.ADMINISTRATIVO);
        assertThat(pedido.getSituacao()).isEqualTo(ENTREGUE);
    }

    @Test
    void historicoRegistraOMomentoInformadoEOResponsavelComUsuario() {
        Pedido pedido = PedidosParaTeste.emSituacao(PAGO, CLIENTE_ID);
        Instant quando = Instant.parse("2026-03-05T08:30:00Z");
        pedido.iniciarPreparacao(Responsavel.administrativo(99L), quando);
        EntradaHistorico entrada = pedido.getHistorico().getLast();
        assertThat(entrada.getOcorridoEm()).isEqualTo(quando);
        assertThat(entrada.getResponsavel().usuarioId()).isEqualTo(99L);
        assertThat(entrada.getResponsavel().ehAdministrativo()).isTrue();
    }

    @Test
    void entradaDeCriacaoTemOrigemNulaResponsavelSistemaESemUsuario() {
        Pedido pedido = Pedido.criar(PedidosParaTeste.validos(CLIENTE_ID), MOMENTO);
        EntradaHistorico entrada = pedido.getHistorico().getFirst();
        assertThat(entrada.getDe()).isNull();
        assertThat(entrada.getPara()).isEqualTo(CRIADO);
        assertThat(entrada.getResponsavel().tipo()).isEqualTo(TipoResponsavel.SISTEMA);
        assertThat(entrada.getResponsavel().usuarioId()).isNull();
        assertThat(entrada.getOcorridoEm()).isEqualTo(MOMENTO);
    }

    // --- Eventos (Observer) ---

    @Test
    void fluxoCompletoAcumulaUmEventoPorTransicaoNaOrdemEEsqueceAoConsumir() {
        Pedido pedido = Pedido.criar(PedidosParaTeste.validos(CLIENTE_ID), MOMENTO);
        pedido.aguardarPagamento(SISTEMA, T1);
        pedido.confirmarPagamento(SISTEMA, null, T1);
        pedido.iniciarPreparacao(ADMIN, T1);
        pedido.enviar(ADMIN, T1);
        pedido.entregar(ADMIN, T1);

        List<EventoDominio> eventos = pedido.consumirEventos();
        assertThat(eventos).extracting(Object::getClass).containsExactly(PedidoCriado.class, PedidoPago.class,
                PedidoEmPreparacao.class, PedidoEnviado.class, PedidoEntregue.class);
        assertThat(pedido.consumirEventos()).isEmpty();
    }

    @Test
    void criacaoDoPedidoNaoAnunciaEventoMasARecusaAnunciaPedidoPagamentoRecusado() {
        Pedido pedido = Pedido.criar(PedidosParaTeste.validos(CLIENTE_ID), MOMENTO);
        assertThat(pedido.consumirEventos()).isEmpty();
        pedido.aguardarPagamento(SISTEMA, T1);
        pedido.recusarPagamento(SISTEMA, null, T1);
        assertThat(pedido.consumirEventos()).extracting(Object::getClass)
                .containsExactly(PedidoCriado.class, PedidoPagamentoRecusado.class);
    }

    @Test
    void eventosTrazemClienteEstadoAnteriorNovoEstadoEMomento() {
        Pedido pedido = PedidosParaTeste.emSituacao(AGUARDANDO_PAGAMENTO, CLIENTE_ID);
        pedido.confirmarPagamento(SISTEMA, null, T1);
        assertThat(pedido.consumirEventos()).singleElement().isInstanceOfSatisfying(PedidoPago.class, e -> {
            assertThat(e.clienteId()).isEqualTo(CLIENTE_ID);
            assertThat(e.de()).isEqualTo(AGUARDANDO_PAGAMENTO);
            assertThat(e.para()).isEqualTo(PAGO);
            assertThat(e.ocorridoEm()).isEqualTo(T1);
        });
    }

    @Test
    void transicaoRejeitadaNoMeioDoFluxoPreservaOsEventosJaAcumulados() {
        Pedido pedido = PedidosParaTeste.emSituacao(AGUARDANDO_PAGAMENTO, CLIENTE_ID);
        pedido.confirmarPagamento(SISTEMA, null, T1);
        assertThatThrownBy(() -> pedido.enviar(ADMIN, T1)).isInstanceOf(TransicaoInvalidaException.class);
        assertThat(pedido.consumirEventos()).extracting(Object::getClass).containsExactly(PedidoPago.class);
    }

    @ParameterizedTest(name = "evento de cancelamento a partir de {0} informa o estado de origem")
    @EnumSource(value = SituacaoPedido.class, names = {"CRIADO", "AGUARDANDO_PAGAMENTO", "PAGAMENTO_RECUSADO",
            "PAGO", "EM_PREPARACAO"})
    void eventoDeCancelamentoInformaOEstadoDeOrigemParaOsOuvintes(SituacaoPedido origem) {
        Pedido pedido = PedidosParaTeste.emSituacao(origem, CLIENTE_ID);
        pedido.cancelar(ADMIN, "teste", T1);
        assertThat(pedido.consumirEventos()).singleElement()
                .isInstanceOfSatisfying(PedidoCancelado.class, e -> assertThat(e.de()).isEqualTo(origem));
    }

    // --- Singletons / enum / isolamento ---

    @ParameterizedTest
    @EnumSource(SituacaoPedido.class)
    void estadosSaoSemEstadoEPartilhamUmaUnicaInstanciaPorSituacao(SituacaoPedido situacao) {
        assertThat(situacao.estado()).isSameAs(situacao.estado());
        assertThat(situacao.estado().situacao()).isEqualTo(situacao);
    }

    @Test
    void sao8SituacoesEAsDoEnunciado() {
        assertThat(SituacaoPedido.values()).extracting(Enum::name).containsExactly("CRIADO", "AGUARDANDO_PAGAMENTO",
                "PAGO", "PAGAMENTO_RECUSADO", "EM_PREPARACAO", "ENVIADO", "ENTREGUE", "CANCELADO");
    }

    @Test
    void doisPedidosNaMesmaSituacaoNaoCompartilhamHistoricoNemEventos() {
        Pedido a = PedidosParaTeste.emSituacao(AGUARDANDO_PAGAMENTO, 1L);
        Pedido b = PedidosParaTeste.emSituacao(AGUARDANDO_PAGAMENTO, 2L);
        a.confirmarPagamento(SISTEMA, null, T1);
        assertThat(b.getSituacao()).isEqualTo(AGUARDANDO_PAGAMENTO);
        assertThat(b.getHistorico()).hasSize(2);
        assertThat(b.consumirEventos()).isEmpty();
        assertThat(a.consumirEventos()).hasSize(1);
    }

    @Test
    void responsavelExigeTipoEIdentificaPerfilAdministrativo() {
        assertThatThrownBy(() -> new Responsavel(null, 1L)).isInstanceOf(NullPointerException.class);
        assertThat(Responsavel.sistema().ehAdministrativo()).isFalse();
        assertThat(Responsavel.sistema().usuarioId()).isNull();
        assertThat(Responsavel.cliente(1L).ehAdministrativo()).isFalse();
        assertThat(Responsavel.administrativo(1L).ehAdministrativo()).isTrue();
    }

    @Test
    void transicaoComResponsavelNuloNaoDeveAlterarOPedido() {
        Pedido pedido = PedidosParaTeste.emSituacao(AGUARDANDO_PAGAMENTO, CLIENTE_ID);
        assertThatThrownBy(() -> pedido.confirmarPagamento(null, null, T1)).isInstanceOf(RuntimeException.class);
        assertThat(pedido.getSituacao()).isEqualTo(AGUARDANDO_PAGAMENTO);
        assertThat(pedido.getHistorico()).hasSize(2);
        assertThat(pedido.consumirEventos()).isEmpty();
    }
}
