package br.com.fatec.ecommerce.estoque.aplicacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fatec.ecommerce.TesteDeIntegracao;
import br.com.fatec.ecommerce.compartilhado.dominio.ConflitoException;
import br.com.fatec.ecommerce.compartilhado.dominio.DadoInvalidoException;
import br.com.fatec.ecommerce.estoque.EstoqueParaTeste;
import br.com.fatec.ecommerce.estoque.dominio.ComponenteAReservar;
import br.com.fatec.ecommerce.estoque.dominio.EstoqueInsuficienteException;
import br.com.fatec.ecommerce.estoque.dominio.ReservaEstoque;
import br.com.fatec.ecommerce.estoque.dominio.ReservaEstoqueRepository;
import br.com.fatec.ecommerce.estoque.dominio.SaldoEstoque;
import br.com.fatec.ecommerce.estoque.dominio.SaldoEstoqueRepository;
import br.com.fatec.ecommerce.estoque.dominio.SituacaoReserva;
import br.com.fatec.ecommerce.seguranca.UsuariosParaTeste;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Invariantes do estoque no banco e nos repositorios: saldo nunca negativo (CHECK), reserva unica por pedido e
 * variacao, atualizacoes condicionais e ciclo de vida completo da reserva (ATIVA, LIBERADA, BAIXADA, REPOSTA).
 */
@TesteDeIntegracao
class EstoqueInvariantesTests {

    private static final long A = 821L;
    private static final long B = 822L;
    private static final long C = 823L;

    @Autowired
    ReservarEstoque reservar;
    @Autowired
    AjustarEstoque ajustar;
    @Autowired
    MovimentarReservas movimentar;
    @Autowired
    ConsultarSaldo consultar;
    @Autowired
    SaldoEstoqueRepository saldos;
    @Autowired
    ReservaEstoqueRepository reservas;
    @Autowired
    EstoqueParaTeste estoque;
    @Autowired
    UsuariosParaTeste helper;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    PlatformTransactionManager transacoes;

    @BeforeEach
    void preparar() {
        helper.limpar();
    }

    // ---- CHECK do banco -----------------------------------------------------------------------------------------

    @Test
    void bancoRecusaSaldoDisponivelNegativo() {
        assertThatThrownBy(() -> estoque.definirSaldo(A, -1, 0)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void bancoRecusaSaldoReservadoNegativo() {
        assertThatThrownBy(() -> estoque.definirSaldo(A, 0, -1)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void bancoRecusaReservaComQuantidadeNaoPositivaOuSituacaoDesconhecida() {
        String insert = "INSERT INTO reserva_estoque (pedido_id, variacao_id, quantidade, situacao, criado_em, atualizado_em)"
                + " VALUES (1, 1, ?, ?, now(), now())";
        assertThatThrownBy(() -> jdbc.update(insert, 0, "ATIVA")).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(insert, -3, "ATIVA")).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(insert, 1, "INVENTADA")).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void bancoRecusaDuasReservasDoMesmoPedidoParaAMesmaVariacao() {
        reservas.salvar(ReservaEstoque.ativa(1L, new ComponenteAReservar(A, 1), Instant.now()));

        assertThatThrownBy(() -> reservas.salvar(ReservaEstoque.ativa(1L, new ComponenteAReservar(A, 2), Instant.now())))
                .isInstanceOf(ConflitoException.class);
        assertThat(estoque.quantidadeDeReservas()).isEqualTo(1);
    }

    @Test
    void mesmaVariacaoEmPedidosDiferentesEPedidoComVariacoesDiferentesSaoPermitidos() {
        reservas.salvar(ReservaEstoque.ativa(1L, new ComponenteAReservar(A, 1), Instant.now()));
        reservas.salvar(ReservaEstoque.ativa(2L, new ComponenteAReservar(A, 1), Instant.now()));
        reservas.salvar(ReservaEstoque.ativa(1L, new ComponenteAReservar(B, 1), Instant.now()));

        assertThat(estoque.quantidadeDeReservas()).isEqualTo(3);
    }

    // ---- repositorio de saldos ---------------------------------------------------------------------------------

    @Test
    @Transactional
    void criarSeAusenteCriaZeradoENuncaSobrescreve() {
        saldos.criarSeAusente(A);
        assertThat(estoque.disponivel(A)).isZero();
        assertThat(estoque.reservado(A)).isZero();

        estoque.definirSaldo(A, 9, 3);
        saldos.criarSeAusente(A);
        saldos.criarSeAusente(A);

        assertThat(estoque.disponivel(A)).isEqualTo(9);
        assertThat(estoque.reservado(A)).isEqualTo(3);
    }

    @Test
    void disponivelDeLeDiretoDoBancoEEhVazioSemSaldo() {
        estoque.definirSaldo(A, 6, 1);

        assertThat(saldos.disponivelDe(A)).contains(6);
        assertThat(saldos.disponivelDe(999L)).isEmpty();
        assertThat(saldos.buscarPorVariacaoId(999L)).isEmpty();
    }

    @Test
    @Transactional
    void ajustarDisponivelRetornaFalsoSemSaldoEComSaldoQueFicariaNegativo() {
        estoque.definirSaldo(A, 2, 0);

        assertThat(saldos.ajustarDisponivel(999L, 1)).isFalse();
        assertThat(saldos.ajustarDisponivel(A, -3)).isFalse();
        assertThat(saldos.ajustarDisponivel(A, -2)).isTrue();
        assertThat(saldos.ajustarDisponivel(A, -1)).isFalse();
        assertThat(estoque.disponivel(A)).isZero();
    }

    @Test
    @Transactional
    void reservarRetornaFalsoQuandoNaoHaDisponivelSuficienteENaoAlteraNada() {
        estoque.definirSaldo(A, 2, 1);

        assertThat(saldos.reservar(A, 3)).isFalse();
        assertThat(saldos.reservar(999L, 1)).isFalse();
        assertThat(saldos.reservar(A, 2)).isTrue();

        assertThat(estoque.disponivel(A)).isZero();
        assertThat(estoque.reservado(A)).isEqualTo(3);
    }

    @Test
    void consultaPorVariacoesIgnoraRepetidasInexistentesEOrdenaPorVariacao() {
        estoque.definirSaldo(C, 1, 0);
        estoque.definirSaldo(A, 2, 0);
        estoque.definirSaldo(B, 3, 0);

        List<SaldoEstoque> lista = consultar.porVariacoes(List.of(C, A, A, 999L, B));

        assertThat(lista).extracting(SaldoEstoque::getVariacaoId).containsExactly(A, B, C);
    }

    @Test
    void estoqueBaixoComLimiteZeroOuNegativoNaoListaNingemEComLimiteAltoListaTodos() {
        estoque.definirSaldo(A, 0, 5);
        estoque.definirSaldo(B, 4, 0);

        assertThat(consultar.abaixoDe(0)).isEmpty();
        assertThat(consultar.abaixoDe(-5)).isEmpty();
        assertThat(consultar.abaixoDe(1)).extracting(SaldoEstoque::getVariacaoId).containsExactly(A);
        assertThat(consultar.abaixoDe(Integer.MAX_VALUE)).extracting(SaldoEstoque::getVariacaoId)
                .containsExactly(A, B);
    }

    @Test
    void estoqueBaixoConsideraSoODisponivelENaoOReservado() {
        estoque.definirSaldo(A, 10, 0);
        reservar.para(1L, List.of(new ComponenteAReservar(A, 8)));

        assertThat(consultar.abaixoDe(5)).extracting(SaldoEstoque::getVariacaoId).containsExactly(A);
        assertThat(consultar.porVariacao(A).getReservado()).isEqualTo(8);
    }

    @Test
    void ajusteAtualizaOInstanteDeAtualizacaoENaoMexeNoReservado() {
        estoque.definirSaldo(A, 5, 3);
        Instant antes = consultar.porVariacao(A).getAtualizadoEm();

        SaldoEstoque depois = ajustar.ajustar(A, -2, "perda");

        assertThat(depois.getDisponivel()).isEqualTo(3);
        assertThat(depois.getReservado()).isEqualTo(3);
        assertThat(depois.getAtualizadoEm()).isAfterOrEqualTo(antes);
    }

    @Test
    void ajusteNegativoNaoPodeConsumirOQueEstaReservado() {
        estoque.definirSaldo(A, 2, 5);

        assertThatThrownBy(() -> ajustar.ajustar(A, -3, "perda")).isInstanceOf(ConflitoException.class);

        assertThat(estoque.disponivel(A)).isEqualTo(2);
        assertThat(estoque.reservado(A)).isEqualTo(5);
    }

    // ---- repositorio de reservas -------------------------------------------------------------------------------

    @Test
    void reservasDoPedidoVoltamOrdenadasPorVariacaoEFiltradasPorSituacao() {
        reservas.salvar(ReservaEstoque.ativa(1L, new ComponenteAReservar(C, 1), Instant.now()));
        reservas.salvar(ReservaEstoque.ativa(1L, new ComponenteAReservar(A, 1), Instant.now()));
        reservas.salvar(ReservaEstoque.ativa(1L, new ComponenteAReservar(B, 1), Instant.now()));
        reservas.salvar(ReservaEstoque.ativa(2L, new ComponenteAReservar(A, 1), Instant.now()));

        assertThat(reservas.listarPorPedidoESituacao(1L, SituacaoReserva.ATIVA))
                .extracting(ReservaEstoque::getVariacaoId).containsExactly(A, B, C);
        assertThat(reservas.listarPorPedidoESituacao(1L, SituacaoReserva.BAIXADA)).isEmpty();
        assertThat(reservas.listarPorPedidoESituacao(404L, SituacaoReserva.ATIVA)).isEmpty();
        assertThat(reservas.existePorPedido(1L)).isTrue();
        assertThat(reservas.existePorPedido(404L)).isFalse();
    }

    @Test
    @Transactional
    void transitarSoFuncionaAPartirDaSituacaoEsperadaEUmaUnicaVez() {
        Long id = reservas.salvar(ReservaEstoque.ativa(1L, new ComponenteAReservar(A, 1), Instant.now())).getId();

        assertThat(reservas.transitar(id, SituacaoReserva.BAIXADA, SituacaoReserva.REPOSTA)).isFalse();
        assertThat(reservas.transitar(id, SituacaoReserva.ATIVA, SituacaoReserva.BAIXADA)).isTrue();
        assertThat(reservas.transitar(id, SituacaoReserva.ATIVA, SituacaoReserva.BAIXADA)).isFalse();
        assertThat(reservas.transitar(id, SituacaoReserva.ATIVA, SituacaoReserva.LIBERADA)).isFalse();
        assertThat(reservas.transitar(999999L, SituacaoReserva.ATIVA, SituacaoReserva.BAIXADA)).isFalse();
        assertThat(estoque.situacoesDaReserva(1L)).containsExactly("BAIXADA");
    }

    // ---- ciclo de vida -----------------------------------------------------------------------------------------

    @Test
    void cicloCompletoReservarBaixarReporRestauraOSaldoOriginal() {
        estoque.definirSaldo(A, 10, 0);
        estoque.definirSaldo(B, 4, 0);
        reservar.para(1L, List.of(new ComponenteAReservar(A, 3), new ComponenteAReservar(B, 4)));
        assertThat(estoque.disponivel(B)).isZero();

        movimentar.baixar(1L);
        assertThat(estoque.disponivel(A)).isEqualTo(7);
        assertThat(estoque.reservado(A)).isZero();

        movimentar.repor(1L);
        assertThat(estoque.disponivel(A)).isEqualTo(10);
        assertThat(estoque.disponivel(B)).isEqualTo(4);
        assertThat(estoque.reservado(B)).isZero();
        assertThat(estoque.situacoesDaReserva(1L)).containsOnly("REPOSTA");
    }

    @Test
    void liberadaNaoPodeSerBaixadaNemRepostaEBaixadaNaoPodeSerLiberada() {
        estoque.definirSaldo(A, 10, 0);
        reservar.para(1L, List.of(new ComponenteAReservar(A, 3)));
        reservar.para(2L, List.of(new ComponenteAReservar(A, 2)));

        movimentar.liberar(1L);
        movimentar.baixar(1L);
        movimentar.repor(1L);
        movimentar.baixar(2L);
        movimentar.liberar(2L);

        assertThat(estoque.situacoesDaReserva(1L)).containsExactly("LIBERADA");
        assertThat(estoque.situacoesDaReserva(2L)).containsExactly("BAIXADA");
        assertThat(estoque.disponivel(A)).isEqualTo(8); // 10 - 2 baixados
        assertThat(estoque.reservado(A)).isZero();
    }

    @Test
    void pedidoComReservaLiberadaNaoPodeReservarDeNovo() {
        estoque.definirSaldo(A, 10, 0);
        reservar.para(1L, List.of(new ComponenteAReservar(A, 3)));
        movimentar.liberar(1L);

        assertThatThrownBy(() -> reservar.para(1L, List.of(new ComponenteAReservar(A, 1))))
                .isInstanceOf(ConflitoException.class).isNotInstanceOf(EstoqueInsuficienteException.class);

        assertThat(estoque.disponivel(A)).isEqualTo(10);
    }

    @Test
    void liberarNaoMexeNasReservasDeOutrosPedidosDaMesmaVariacao() {
        estoque.definirSaldo(A, 10, 0);
        reservar.para(1L, List.of(new ComponenteAReservar(A, 3)));
        reservar.para(2L, List.of(new ComponenteAReservar(A, 4)));

        movimentar.liberar(1L);

        assertThat(estoque.disponivel(A)).isEqualTo(6);
        assertThat(estoque.reservado(A)).isEqualTo(4);
        assertThat(estoque.situacoesDaReserva(2L)).containsExactly("ATIVA");
    }

    @Test
    void somaDeDisponivelEReservadoSoMudaPorBaixaReposicaoEAjuste() {
        estoque.definirSaldo(A, 10, 0);
        assertThat(total(A)).isEqualTo(10);

        reservar.para(1L, List.of(new ComponenteAReservar(A, 4)));
        assertThat(total(A)).isEqualTo(10);

        movimentar.liberar(1L);
        assertThat(total(A)).isEqualTo(10);

        reservar.para(2L, List.of(new ComponenteAReservar(A, 4)));
        movimentar.baixar(2L);
        assertThat(total(A)).isEqualTo(6);

        movimentar.repor(2L);
        assertThat(total(A)).isEqualTo(10);

        ajustar.ajustar(A, -3, "perda");
        assertThat(total(A)).isEqualTo(7);
    }

    private int total(long variacao) {
        return estoque.disponivel(variacao) + estoque.reservado(variacao);
    }

    // ---- atomicidade (rollback do chamador) --------------------------------------------------------------------

    @Test
    void reservaFeitaNaTransacaoDoChamadorEDesfeitaNoRollback() {
        estoque.definirSaldo(A, 5, 0);

        new TransactionTemplate(transacoes).executeWithoutResult(tx -> {
            reservar.para(1L, List.of(new ComponenteAReservar(A, 2)));
            tx.setRollbackOnly();
        });

        assertThat(estoque.disponivel(A)).isEqualTo(5);
        assertThat(estoque.reservado(A)).isZero();
        assertThat(estoque.quantidadeDeReservas()).isZero();
    }

    @Test
    void movimentoDeReservaFeitoNaTransacaoDoChamadorEDesfeitoNoRollback() {
        estoque.definirSaldo(A, 5, 0);
        reservar.para(1L, List.of(new ComponenteAReservar(A, 2)));

        new TransactionTemplate(transacoes).executeWithoutResult(tx -> {
            movimentar.liberar(1L);
            tx.setRollbackOnly();
        });

        assertThat(estoque.disponivel(A)).isEqualTo(3);
        assertThat(estoque.reservado(A)).isEqualTo(2);
        assertThat(estoque.situacoesDaReserva(1L)).containsExactly("ATIVA");
    }

    @Test
    void ajusteFeitoNaTransacaoDoChamadorEDesfeitoNoRollback() {
        estoque.definirSaldo(A, 5, 0);

        new TransactionTemplate(transacoes).executeWithoutResult(tx -> {
            ajustar.ajustar(A, 10, "entrada");
            tx.setRollbackOnly();
        });

        assertThat(estoque.disponivel(A)).isEqualTo(5);
    }

    // ---- limites numericos --------------------------------------------------------------------------------------

    @Test
    void ajusteComDeltaMinimoDeIntNaoCorrompeOSaldo() {
        estoque.definirSaldo(A, 5, 0);

        assertThatThrownBy(() -> ajustar.ajustar(A, Integer.MIN_VALUE, "perda")).isInstanceOf(RuntimeException.class);

        assertThat(estoque.disponivel(A)).isEqualTo(5);
    }

    @Test
    void ajusteQueEstouraIntNoBancoEhRejeitadoPorExcecaoDeDominio() {
        estoque.definirSaldo(A, 5, 0);

        assertThatThrownBy(() -> ajustar.ajustar(A, Integer.MAX_VALUE, "entrada"))
                .isInstanceOfAny(ConflitoException.class, DadoInvalidoException.class);

        assertThat(estoque.disponivel(A)).isEqualTo(5);
    }

    @Test
    void reservaComSomaDeComponentesQueEstouraIntEhRejeitadaEnaoReservaQuantidadeTruncada() {
        estoque.definirSaldo(A, 5, 2);

        // 2 x Integer.MAX_VALUE + 3 estoura o int e daria 1 unidade se a soma fosse silenciosamente truncada.
        assertThatThrownBy(() -> reservar.para(1L, List.of(new ComponenteAReservar(A, Integer.MAX_VALUE),
                new ComponenteAReservar(A, Integer.MAX_VALUE), new ComponenteAReservar(A, 3))))
                .isInstanceOf(RuntimeException.class);

        assertThat(estoque.disponivel(A)).isEqualTo(5);
        assertThat(estoque.reservado(A)).isEqualTo(2);
        assertThat(estoque.quantidadeDeReservas()).isZero();
    }

    @Test
    void buscaDeSaldoPorVariacaoRetornaOpcionalVazioParaInexistente() {
        Optional<SaldoEstoque> saldo = saldos.buscarPorVariacaoId(123456L);

        assertThat(saldo).isEmpty();
    }
}
