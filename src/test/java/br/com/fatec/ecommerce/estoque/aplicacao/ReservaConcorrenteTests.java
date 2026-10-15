package br.com.fatec.ecommerce.estoque.aplicacao;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.fatec.ecommerce.TesteDeIntegracao;
import br.com.fatec.ecommerce.estoque.EstoqueParaTeste;
import br.com.fatec.ecommerce.estoque.dominio.ComponenteAReservar;
import br.com.fatec.ecommerce.estoque.dominio.EstoqueInsuficienteException;
import br.com.fatec.ecommerce.seguranca.UsuariosParaTeste;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * "Dez pedidos pela ultima unidade": dez threads, liberadas juntas por uma barreira de largada, reservam a unica
 * unidade da mesma variacao. Cada chamada ao servico transacional roda na propria transacao (RN-21).
 */
@TesteDeIntegracao
class ReservaConcorrenteTests {

    private static final long VARIACAO = 501L;
    private static final int TENTATIVAS = 10;

    @Autowired
    ReservarEstoque reservar;
    @Autowired
    EstoqueParaTeste estoque;
    @Autowired
    UsuariosParaTeste helper;

    @BeforeEach
    void preparar() {
        helper.limpar();
        estoque.definirSaldo(VARIACAO, 1, 0);
    }

    @RepeatedTest(3)
    void dezPedidosPelaUltimaUnidadeResultamEmUmSucessoENoveInsuficientes() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(TENTATIVAS);
        CountDownLatch prontas = new CountDownLatch(TENTATIVAS);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<Void>> futuros = new ArrayList<>();
        try {
            for (int i = 0; i < TENTATIVAS; i++) {
                long pedidoId = 9000L + i;
                Callable<Void> tentativa = () -> {
                    prontas.countDown();
                    largada.await();
                    reservar.para(pedidoId, List.of(new ComponenteAReservar(VARIACAO, 1)));
                    return null;
                };
                futuros.add(pool.submit(tentativa));
            }
            assertThat(prontas.await(30, TimeUnit.SECONDS)).isTrue();
            largada.countDown();

            int sucessos = 0;
            List<Throwable> falhas = new ArrayList<>();
            for (Future<Void> futuro : futuros) {
                try {
                    futuro.get(60, TimeUnit.SECONDS);
                    sucessos++;
                } catch (ExecutionException e) {
                    falhas.add(e.getCause());
                }
            }

            assertThat(sucessos).isEqualTo(1);
            assertThat(falhas).hasSize(TENTATIVAS - 1).allSatisfy(f -> assertThat(f)
                    .isInstanceOfSatisfying(EstoqueInsuficienteException.class, e -> {
                        assertThat(e.getVariacaoId()).isEqualTo(VARIACAO);
                        assertThat(e.getDisponivel()).isZero();
                    }));
        } finally {
            pool.shutdownNow();
        }

        assertThat(estoque.disponivel(VARIACAO)).isZero();
        assertThat(estoque.reservado(VARIACAO)).isEqualTo(1);
        assertThat(estoque.quantidadeDeReservas()).isEqualTo(1);
    }
}
