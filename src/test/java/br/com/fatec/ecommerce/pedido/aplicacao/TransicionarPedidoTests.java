package br.com.fatec.ecommerce.pedido.aplicacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fatec.ecommerce.TesteDeIntegracao;
import br.com.fatec.ecommerce.cliente.ClientesParaTeste;
import br.com.fatec.ecommerce.cliente.ClientesParaTeste.ClienteDeTeste;
import br.com.fatec.ecommerce.compartilhado.dominio.ConflitoException;
import br.com.fatec.ecommerce.compartilhado.dominio.DadoInvalidoException;
import br.com.fatec.ecommerce.compartilhado.dominio.RecursoNaoEncontradoException;
import br.com.fatec.ecommerce.pedido.CapturaDeEventosDoPedido;
import br.com.fatec.ecommerce.pedido.PedidosParaTeste;
import br.com.fatec.ecommerce.pedido.dominio.EntradaHistorico;
import br.com.fatec.ecommerce.pedido.dominio.Pedido;
import br.com.fatec.ecommerce.pedido.dominio.PedidoRepository;
import br.com.fatec.ecommerce.pedido.dominio.Responsavel;
import br.com.fatec.ecommerce.pedido.dominio.SituacaoPedido;
import br.com.fatec.ecommerce.pedido.dominio.TipoResponsavel;
import br.com.fatec.ecommerce.pedido.dominio.TransicaoInvalidaException;
import br.com.fatec.ecommerce.pedido.dominio.evento.PedidoCancelado;
import br.com.fatec.ecommerce.pedido.dominio.evento.PedidoEmPreparacao;
import br.com.fatec.ecommerce.pedido.dominio.evento.PedidoEntregue;
import br.com.fatec.ecommerce.pedido.dominio.evento.PedidoEnviado;
import br.com.fatec.ecommerce.pedido.dominio.evento.PedidoPago;
import br.com.fatec.ecommerce.seguranca.UsuariosParaTeste;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/** Servicos de transicao: fluxo completo, historico, rejeicao sem efeito e alteracoes concorrentes. */
@TesteDeIntegracao
@Import(CapturaDeEventosDoPedido.class)
class TransicionarPedidoTests {

    private static final Responsavel ADMIN = Responsavel.administrativo(1L);

    @Autowired
    CriarPedido criarPedido;
    @Autowired
    ConfirmarReservaDoPedido confirmarReserva;
    @Autowired
    TransicionarPedido transicionar;
    @Autowired
    PedidoRepository pedidos;
    @Autowired
    UsuariosParaTeste helper;
    @Autowired
    ClientesParaTeste clientesHelper;
    @Autowired
    CapturaDeEventosDoPedido captura;

    ClienteDeTeste cliente;

    @BeforeEach
    void preparar() {
        helper.limpar();
        cliente = clientesHelper.criar("cliente@demo.local", 31);
    }

    private Long pedidoAguardandoPagamento() {
        Long id = criarPedido.executar(PedidosParaTeste.validos(cliente.cliente().getId())).getId();
        confirmarReserva.executar(id);
        captura.limpar();
        return id;
    }

    private Long pedidoEmPreparacao() {
        Long id = pedidoAguardandoPagamento();
        transicionar.aprovarPagamento(id, Responsavel.sistema(), null);
        transicionar.iniciarPreparacao(id, ADMIN);
        captura.limpar();
        return id;
    }

    private Pedido recarregar(Long id) {
        return pedidos.buscarPorId(id).orElseThrow();
    }

    @Test
    void fluxoCompletoTerminaEmEntregueComUmaEntradaDeHistoricoPorTransicao() {
        Long id = pedidoAguardandoPagamento();

        transicionar.aprovarPagamento(id, Responsavel.sistema(), "pagamento simulado");
        transicionar.iniciarPreparacao(id, ADMIN);
        transicionar.enviar(id, ADMIN);
        transicionar.entregar(id, ADMIN);

        Pedido pedido = recarregar(id);
        assertThat(pedido.getSituacao()).isEqualTo(SituacaoPedido.ENTREGUE);
        assertThat(pedido.getHistorico()).extracting(EntradaHistorico::getPara).containsExactly(
                SituacaoPedido.CRIADO, SituacaoPedido.AGUARDANDO_PAGAMENTO, SituacaoPedido.PAGO,
                SituacaoPedido.EM_PREPARACAO, SituacaoPedido.ENVIADO, SituacaoPedido.ENTREGUE);
        assertThat(captura.recebidos()).hasSize(4).satisfiesExactly(
                e -> assertThat(e).isInstanceOf(PedidoPago.class),
                e -> assertThat(e).isInstanceOf(PedidoEmPreparacao.class),
                e -> assertThat(e).isInstanceOf(PedidoEnviado.class),
                e -> assertThat(e).isInstanceOf(PedidoEntregue.class));
    }

    @Test
    void historicoVemEmOrdemCronologicaComResponsavelEMotivo() {
        Long id = pedidoAguardandoPagamento();

        transicionar.aprovarPagamento(id, Responsavel.sistema(), "pagamento simulado");
        transicionar.iniciarPreparacao(id, ADMIN);
        transicionar.enviar(id, Responsavel.administrativo(3L));

        List<EntradaHistorico> historico = recarregar(id).getHistorico();
        assertThat(historico).extracting(EntradaHistorico::getOcorridoEm).isSorted();
        assertThat(historico).extracting(EntradaHistorico::getDe).containsExactly(null, SituacaoPedido.CRIADO,
                SituacaoPedido.AGUARDANDO_PAGAMENTO, SituacaoPedido.PAGO, SituacaoPedido.EM_PREPARACAO);
        assertThat(historico.get(2).getResponsavel()).isEqualTo(Responsavel.sistema());
        assertThat(historico.get(2).getMotivo()).isEqualTo("pagamento simulado");
        assertThat(historico.get(3).getResponsavel()).isEqualTo(ADMIN);
        assertThat(historico.get(4).getResponsavel()).isEqualTo(Responsavel.administrativo(3L));
        assertThat(historico.get(4).getMotivo()).isNull();
    }

    @Test
    void recusaDePagamentoNaoReabreOPedido() {
        Long id = pedidoAguardandoPagamento();

        transicionar.recusarPagamento(id, Responsavel.sistema(), "pagamento simulado");

        assertThat(recarregar(id).getSituacao()).isEqualTo(SituacaoPedido.PAGAMENTO_RECUSADO);
        assertThatThrownBy(() -> transicionar.aprovarPagamento(id, Responsavel.sistema(), null))
                .isInstanceOf(TransicaoInvalidaException.class);
        assertThat(recarregar(id).getSituacao()).isEqualTo(SituacaoPedido.PAGAMENTO_RECUSADO);
    }

    @Test
    void clienteCancelaPedidoAguardandoPagamentoERegistraResponsavelEMotivo() {
        Long id = pedidoAguardandoPagamento();
        Responsavel responsavel = Responsavel.cliente(cliente.usuario().getId());

        transicionar.cancelar(id, responsavel, "desisti da compra");

        Pedido pedido = recarregar(id);
        assertThat(pedido.getSituacao()).isEqualTo(SituacaoPedido.CANCELADO);
        EntradaHistorico ultima = pedido.getHistorico().get(pedido.getHistorico().size() - 1);
        assertThat(ultima.getDe()).isEqualTo(SituacaoPedido.AGUARDANDO_PAGAMENTO);
        assertThat(ultima.getResponsavel()).isEqualTo(responsavel);
        assertThat(ultima.getMotivo()).isEqualTo("desisti da compra");
    }

    @Test
    void clienteNaoCancelaPedidoPagoMasAdministradorCancela() {
        Long id = pedidoAguardandoPagamento();
        transicionar.aprovarPagamento(id, Responsavel.sistema(), null);
        captura.limpar();

        assertThatThrownBy(() -> transicionar.cancelar(id, Responsavel.cliente(cliente.usuario().getId()), "x"))
                .isInstanceOf(TransicaoInvalidaException.class);
        assertThat(recarregar(id).getSituacao()).isEqualTo(SituacaoPedido.PAGO);
        assertThat(captura.recebidos()).isEmpty();

        transicionar.cancelar(id, ADMIN, "fraude");

        assertThat(recarregar(id).getSituacao()).isEqualTo(SituacaoPedido.CANCELADO);
        assertThat(captura.recebidos()).singleElement().isInstanceOfSatisfying(PedidoCancelado.class,
                e -> assertThat(e.de()).isEqualTo(SituacaoPedido.PAGO));
    }

    @Test
    void cancelamentoSemMotivoELancaDadoInvalidoEONaoAlteraOPedido() {
        Long id = pedidoAguardandoPagamento();

        assertThatThrownBy(() -> transicionar.cancelar(id, ADMIN, " ")).isInstanceOf(DadoInvalidoException.class);

        assertThat(recarregar(id).getSituacao()).isEqualTo(SituacaoPedido.AGUARDANDO_PAGAMENTO);
    }

    @Test
    void transicaoInvalidaNaoAlteraHistoricoNemPublicaEvento() {
        Long id = pedidoAguardandoPagamento();
        int entradas = recarregar(id).getHistorico().size();

        assertThatThrownBy(() -> transicionar.enviar(id, ADMIN)).isInstanceOf(TransicaoInvalidaException.class);

        Pedido pedido = recarregar(id);
        assertThat(pedido.getSituacao()).isEqualTo(SituacaoPedido.AGUARDANDO_PAGAMENTO);
        assertThat(pedido.getHistorico()).hasSize(entradas);
        assertThat(captura.recebidos()).isEmpty();
    }

    @Test
    void pedidoInexistenteLancaNaoEncontrado() {
        assertThatThrownBy(() -> transicionar.enviar(-1L, ADMIN)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    // --- Concorrencia (lock otimista) ---

    @Test
    void gravacaoComVersaoDesatualizadaViraConflito() {
        Long id = pedidoEmPreparacao();
        Pedido a = pedidos.buscarPorId(id).orElseThrow();
        Pedido b = pedidos.buscarPorId(id).orElseThrow();
        assertThat(a.getVersao()).isEqualTo(b.getVersao());

        a.enviar(ADMIN, Instant.now());
        pedidos.salvar(a);
        b.cancelar(ADMIN, "cancelamento simultâneo", Instant.now());

        assertThatThrownBy(() -> pedidos.salvar(b)).isInstanceOf(ConflitoException.class);

        Pedido aposDisputa = recarregar(id);
        assertThat(aposDisputa.getSituacao()).isEqualTo(SituacaoPedido.ENVIADO);
        assertThat(aposDisputa.getHistorico()).extracting(EntradaHistorico::getPara)
                .doesNotContain(SituacaoPedido.CANCELADO);
    }

    @Test
    void envioECancelamentoSimultaneosAplicamApenasUmEORestanteResponde409() throws Exception {
        Long id = pedidoEmPreparacao();
        int entradas = recarregar(id).getHistorico().size();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch largada = new CountDownLatch(1);
        try {
            Callable<Pedido> envio = () -> {
                largada.await();
                return transicionar.enviar(id, ADMIN);
            };
            Callable<Pedido> cancelamento = () -> {
                largada.await();
                return transicionar.cancelar(id, ADMIN, "cancelamento simultâneo");
            };
            Future<Pedido> f1 = pool.submit(envio);
            Future<Pedido> f2 = pool.submit(cancelamento);
            largada.countDown();

            List<Throwable> falhas = new ArrayList<>();
            int sucessos = 0;
            for (Future<Pedido> futuro : List.of(f1, f2)) {
                try {
                    futuro.get();
                    sucessos++;
                } catch (ExecutionException e) {
                    falhas.add(e.getCause());
                }
            }

            assertThat(sucessos).isEqualTo(1);
            assertThat(falhas).singleElement().isInstanceOf(ConflitoException.class);
        } finally {
            pool.shutdownNow();
        }

        Pedido pedido = recarregar(id);
        assertThat(pedido.getSituacao()).isIn(SituacaoPedido.ENVIADO, SituacaoPedido.CANCELADO);
        assertThat(pedido.getHistorico()).hasSize(entradas + 1);
        assertThat(captura.recebidos()).hasSize(1);
    }

    @Test
    void responsavelDoSistemaNaoTemUsuario() {
        assertThat(Responsavel.sistema().tipo()).isEqualTo(TipoResponsavel.SISTEMA);
        assertThat(Responsavel.sistema().usuarioId()).isNull();
    }
}
