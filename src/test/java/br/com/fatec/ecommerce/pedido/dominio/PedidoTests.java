package br.com.fatec.ecommerce.pedido.dominio;

import static br.com.fatec.ecommerce.pedido.PedidosParaTeste.ADMIN;
import static br.com.fatec.ecommerce.pedido.PedidosParaTeste.CLIENTE;
import static br.com.fatec.ecommerce.pedido.PedidosParaTeste.MOMENTO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fatec.ecommerce.compartilhado.dominio.ConflitoException;
import br.com.fatec.ecommerce.compartilhado.dominio.DadoInvalidoException;
import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import br.com.fatec.ecommerce.compartilhado.dominio.EventoDominio;
import br.com.fatec.ecommerce.pedido.PedidosParaTeste;
import br.com.fatec.ecommerce.pedido.dominio.estado.DadosDaTransicao;
import br.com.fatec.ecommerce.pedido.dominio.estado.EstadoPedido;
import br.com.fatec.ecommerce.pedido.dominio.estado.EstadoPedidoBase;
import br.com.fatec.ecommerce.pedido.dominio.evento.PedidoCancelado;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PedidoTests {

    private static final Long CLIENTE_ID = 7L;

    // --- Criacao e snapshot ---

    @Test
    void criaPedidoEmCriadoComSnapshotIgualAoInformado() {
        DadosNovoPedido dados = PedidosParaTeste.validos(CLIENTE_ID);

        Pedido pedido = Pedido.criar(dados, MOMENTO);

        assertThat(pedido.getSituacao()).isEqualTo(SituacaoPedido.CRIADO);
        assertThat(pedido.getClienteId()).isEqualTo(CLIENTE_ID);
        assertThat(pedido.getCriadoEm()).isEqualTo(MOMENTO);
        assertThat(pedido.getItens()).hasSize(2);
        assertThat(pedido.getItens().get(1).getSku()).isEqualTo("TEN-042");
        assertThat(pedido.getItens().get(1).getPrecoUnitario()).isEqualTo(Dinheiro.de("220.00"));
        assertThat(pedido.getItens().get(0).getTotalItem()).isEqualTo(Dinheiro.de("200.00"));
        assertThat(pedido.getDescontos()).singleElement().satisfies(d -> {
            assertThat(d.getDescricao()).isEqualTo("Promo de inverno");
            assertThat(d.getValor()).isEqualTo(Dinheiro.de("20.00"));
        });
        assertThat(pedido.getSubtotal()).isEqualTo(Dinheiro.de("420.00"));
        assertThat(pedido.getTotalDescontos()).isEqualTo(Dinheiro.de("20.00"));
        assertThat(pedido.getFrete()).isEqualTo(Dinheiro.de("15.00"));
        assertThat(pedido.getTotal()).isEqualTo(Dinheiro.de("415.00"));
        assertThat(pedido.getEntrega().getLogradouro()).isEqualTo("Rua das Flores");
    }

    @Test
    void criacaoRegistraEntradaInicialSemOrigemEEmiteNenhumEvento() {
        Pedido pedido = Pedido.criar(PedidosParaTeste.validos(CLIENTE_ID), MOMENTO);

        assertThat(pedido.getHistorico()).singleElement().satisfies(e -> {
            assertThat(e.getDe()).isNull();
            assertThat(e.getPara()).isEqualTo(SituacaoPedido.CRIADO);
            assertThat(e.getResponsavel().tipo()).isEqualTo(TipoResponsavel.SISTEMA);
        });
        assertThat(pedido.consumirEventos()).isEmpty();
    }

    @Test
    void totalInconsistenteRejeitaOSnapshot() {
        DadosNovoPedido base = PedidosParaTeste.validos(CLIENTE_ID);

        assertThatThrownBy(() -> Pedido.criar(PedidosParaTeste.comTotal(base, Dinheiro.de("999.00")), MOMENTO))
                .isInstanceOf(DadoInvalidoException.class)
                .hasMessageContaining("Totais inconsistentes");
    }

    @Test
    void subtotalDiferenteDaSomaDosItensRejeitaOSnapshot() {
        DadosNovoPedido base = PedidosParaTeste.validos(CLIENTE_ID);
        DadosNovoPedido dados = new DadosNovoPedido(CLIENTE_ID, base.endereco(), base.itens(), base.descontos(),
                base.frete(), Dinheiro.de("400.00"), base.totalDescontos(), Dinheiro.de("395.00"));

        assertThatThrownBy(() -> Pedido.criar(dados, MOMENTO))
                .isInstanceOf(DadoInvalidoException.class)
                .hasMessageContaining("subtotal");
    }

    @Test
    void totalDeDescontosDiferenteDaSomaDosDescontosRejeitaOSnapshot() {
        DadosNovoPedido base = PedidosParaTeste.validos(CLIENTE_ID);
        DadosNovoPedido dados = new DadosNovoPedido(CLIENTE_ID, base.endereco(), base.itens(), base.descontos(),
                base.frete(), base.subtotal(), Dinheiro.de("30.00"), Dinheiro.de("405.00"));

        assertThatThrownBy(() -> Pedido.criar(dados, MOMENTO))
                .isInstanceOf(DadoInvalidoException.class)
                .hasMessageContaining("descontos");
    }

    @Test
    void pedidoSemItensOuComQuantidadeInvalidaERejeitado() {
        DadosNovoPedido base = PedidosParaTeste.validos(CLIENTE_ID);
        DadosNovoPedido semItens = new DadosNovoPedido(CLIENTE_ID, base.endereco(), List.of(), List.of(),
                Dinheiro.zero(), Dinheiro.zero(), Dinheiro.zero(), Dinheiro.zero());
        DadosNovoPedido quantidadeZero = new DadosNovoPedido(CLIENTE_ID, base.endereco(),
                List.of(new DadosNovoPedido.Item("X", "Produto", null, 0, Dinheiro.de("10.00"))), List.of(),
                Dinheiro.zero(), Dinheiro.zero(), Dinheiro.zero(), Dinheiro.zero());

        assertThatThrownBy(() -> Pedido.criar(semItens, MOMENTO)).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> Pedido.criar(quantidadeZero, MOMENTO)).isInstanceOf(DadoInvalidoException.class);
    }

    @Test
    void enderecoDeEntregaInvalidoELancaDadoInvalido() {
        assertThatThrownBy(() -> new EnderecoEntrega("Casa", "123", "Rua", "1", null, "Centro", "Campinas", "SP"))
                .isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> new EnderecoEntrega("Casa", "13010000", " ", "1", null, "Centro", "Campinas", "SP"))
                .isInstanceOf(DadoInvalidoException.class);
    }

    @Test
    void snapshotNaoTemSetters() {
        for (Class<?> tipo : List.of(Pedido.class, ItemPedido.class, DescontoPedido.class, EnderecoEntrega.class,
                EntradaHistorico.class)) {
            assertThat(Arrays.stream(tipo.getMethods()).map(Method::getName))
                    .as("metodos publicos de %s", tipo.getSimpleName())
                    .noneMatch(nome -> nome.startsWith("set"));
        }
    }

    @Test
    void colecoesExpostasNaoSaoModificaveis() {
        Pedido pedido = Pedido.criar(PedidosParaTeste.validos(CLIENTE_ID), MOMENTO);

        assertThatThrownBy(() -> pedido.getItens().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> pedido.getDescontos().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> pedido.getHistorico().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    // --- Historico ---

    @Test
    void cadaTransicaoRegistraUmaEntradaDeHistoricoComResponsavelEMotivo() {
        Pedido pedido = Pedido.criar(PedidosParaTeste.validos(CLIENTE_ID), MOMENTO);
        Instant depois = MOMENTO.plusSeconds(60);

        pedido.aguardarPagamento(Responsavel.sistema(), MOMENTO);
        pedido.confirmarPagamento(Responsavel.sistema(), "pagamento simulado", depois);
        pedido.iniciarPreparacao(ADMIN, depois);

        List<EntradaHistorico> historico = pedido.getHistorico();
        assertThat(historico).extracting(EntradaHistorico::getPara).containsExactly(SituacaoPedido.CRIADO,
                SituacaoPedido.AGUARDANDO_PAGAMENTO, SituacaoPedido.PAGO, SituacaoPedido.EM_PREPARACAO);
        assertThat(historico).extracting(EntradaHistorico::getDe).containsExactly(null, SituacaoPedido.CRIADO,
                SituacaoPedido.AGUARDANDO_PAGAMENTO, SituacaoPedido.PAGO);
        assertThat(historico.get(2).getMotivo()).isEqualTo("pagamento simulado");
        assertThat(historico.get(2).getOcorridoEm()).isEqualTo(depois);
        assertThat(historico.get(3).getResponsavel()).isEqualTo(ADMIN);
    }

    // --- Cancelamento ---

    @Test
    void clienteNaoCancelaPedidoPagoEOPedidoFicaIntacto() {
        Pedido pedido = PedidosParaTeste.emSituacao(SituacaoPedido.PAGO, CLIENTE_ID);
        int entradas = pedido.getHistorico().size();

        assertThatThrownBy(() -> pedido.cancelar(CLIENTE, "mudei de ideia", MOMENTO))
                .isInstanceOf(TransicaoInvalidaException.class)
                .isInstanceOf(ConflitoException.class);

        assertThat(pedido.getSituacao()).isEqualTo(SituacaoPedido.PAGO);
        assertThat(pedido.getHistorico()).hasSize(entradas);
        assertThat(pedido.consumirEventos()).isEmpty();
    }

    @Test
    void administradorCancelaPedidoPagoERegistraEstadoAnteriorMotivoEResponsavel() {
        Pedido pedido = PedidosParaTeste.emSituacao(SituacaoPedido.PAGO, CLIENTE_ID);
        ReflectionTestUtils.setField(pedido, "id", 55L);

        pedido.cancelar(ADMIN, "fraude suspeita", MOMENTO);

        assertThat(pedido.getSituacao()).isEqualTo(SituacaoPedido.CANCELADO);
        EntradaHistorico ultima = pedido.getHistorico().get(pedido.getHistorico().size() - 1);
        assertThat(ultima.getDe()).isEqualTo(SituacaoPedido.PAGO);
        assertThat(ultima.getPara()).isEqualTo(SituacaoPedido.CANCELADO);
        assertThat(ultima.getResponsavel()).isEqualTo(ADMIN);
        assertThat(ultima.getMotivo()).isEqualTo("fraude suspeita");
        assertThat(pedido.consumirEventos()).singleElement().isEqualTo(
                new PedidoCancelado(55L, CLIENTE_ID, SituacaoPedido.PAGO, SituacaoPedido.CANCELADO, MOMENTO, ADMIN,
                        "fraude suspeita"));
    }

    @Test
    void motivoDoCancelamentoEObrigatorio() {
        Pedido pedido = PedidosParaTeste.emSituacao(SituacaoPedido.AGUARDANDO_PAGAMENTO, CLIENTE_ID);

        assertThatThrownBy(() -> pedido.cancelar(CLIENTE, null, MOMENTO)).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> pedido.cancelar(CLIENTE, "   ", MOMENTO)).isInstanceOf(DadoInvalidoException.class);

        assertThat(pedido.getSituacao()).isEqualTo(SituacaoPedido.AGUARDANDO_PAGAMENTO);
        assertThat(pedido.consumirEventos()).isEmpty();
    }

    // --- Transicao rejeitada ---

    @Test
    void transicaoRejeitadaNaoAlteraSituacaoNemGeraHistoricoOuEvento() {
        Pedido pedido = PedidosParaTeste.emSituacao(SituacaoPedido.ENVIADO, CLIENTE_ID);
        int entradas = pedido.getHistorico().size();

        assertThatThrownBy(() -> pedido.cancelar(ADMIN, "tarde demais", MOMENTO))
                .isInstanceOfSatisfying(TransicaoInvalidaException.class, e -> {
                    assertThat(e.getSituacaoAtual()).isEqualTo(SituacaoPedido.ENVIADO);
                    assertThat(e.getOperacao()).isEqualTo(OperacaoPedido.CANCELAR);
                });
        assertThatThrownBy(() -> pedido.iniciarPreparacao(ADMIN, MOMENTO))
                .isInstanceOf(TransicaoInvalidaException.class);

        assertThat(pedido.getSituacao()).isEqualTo(SituacaoPedido.ENVIADO);
        assertThat(pedido.getHistorico()).hasSize(entradas);
        assertThat(pedido.consumirEventos()).isEmpty();
    }

    @Test
    void consumirEventosEntregaUmaVezEEsquece() {
        Pedido pedido = Pedido.criar(PedidosParaTeste.validos(CLIENTE_ID), MOMENTO);
        pedido.aguardarPagamento(Responsavel.sistema(), MOMENTO);

        assertThat(pedido.consumirEventos()).hasSize(1);
        assertThat(pedido.consumirEventos()).isEmpty();
    }

    // --- Extensibilidade do State ---

    /** Estado novo, so de teste: antes do pagamento o pedido passa por uma revisao de antifraude. */
    record EventoDeRevisao(Long pedidoId, Instant ocorridoEm) implements EventoDominio {
    }

    static class CriadoComRevisao extends EstadoPedidoBase {
        @Override
        public SituacaoPedido situacao() {
            return SituacaoPedido.CRIADO;
        }

        @Override
        public EstadoPedido aguardarPagamento() {
            return new EmRevisaoAntifraude();
        }
    }

    static class EmRevisaoAntifraude extends EstadoPedidoBase {
        @Override
        public SituacaoPedido situacao() {
            return SituacaoPedido.AGUARDANDO_PAGAMENTO;
        }

        @Override
        public EstadoPedido confirmarPagamento() {
            return SituacaoPedido.PAGO.estado();
        }

        @Override
        public Optional<EventoDominio> eventoDeChegada(DadosDaTransicao transicao) {
            return Optional.of(new EventoDeRevisao(transicao.pedidoId(), transicao.ocorridoEm()));
        }
    }

    @Test
    void estadoNovoImplementandoEstadoPedidoFuncionaSemAlterarOPedido() {
        Pedido pedido = Pedido.criar(PedidosParaTeste.validos(CLIENTE_ID), MOMENTO, new CriadoComRevisao());
        ReflectionTestUtils.setField(pedido, "id", 9L);

        pedido.aguardarPagamento(Responsavel.sistema(), MOMENTO);
        assertThat(pedido.getSituacao()).isEqualTo(SituacaoPedido.AGUARDANDO_PAGAMENTO);
        assertThat(pedido.consumirEventos()).containsExactly(new EventoDeRevisao(9L, MOMENTO));

        // O estado novo nao admite cancelar nem enviar: a rejeicao padrao da base vale para ele tambem.
        assertThatThrownBy(() -> pedido.enviar(ADMIN, MOMENTO)).isInstanceOf(TransicaoInvalidaException.class);
        assertThatThrownBy(() -> pedido.cancelar(ADMIN, "x", MOMENTO)).isInstanceOf(TransicaoInvalidaException.class);

        pedido.confirmarPagamento(Responsavel.sistema(), null, MOMENTO);
        pedido.iniciarPreparacao(ADMIN, MOMENTO);
        assertThat(pedido.getSituacao()).isEqualTo(SituacaoPedido.EM_PREPARACAO);
        assertThat(pedido.getHistorico()).extracting(EntradaHistorico::getPara).containsExactly(
                SituacaoPedido.CRIADO, SituacaoPedido.AGUARDANDO_PAGAMENTO, SituacaoPedido.PAGO,
                SituacaoPedido.EM_PREPARACAO);
    }
}
