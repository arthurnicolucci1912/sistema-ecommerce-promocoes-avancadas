package br.com.fatec.ecommerce.estoque.aplicacao;

import br.com.fatec.ecommerce.estoque.dominio.ReservaEstoque;
import br.com.fatec.ecommerce.estoque.dominio.ReservaEstoqueRepository;
import br.com.fatec.ecommerce.estoque.dominio.SaldoEstoqueRepository;
import br.com.fatec.ecommerce.estoque.dominio.SituacaoReserva;
import java.util.function.BiConsumer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de movimentacao das reservas de um pedido, idempotentes (D4): liberar (ATIVA para LIBERADA, o
 * reservado volta ao disponivel), baixar (ATIVA para BAIXADA, o reservado sai) e repor (BAIXADA para REPOSTA, o
 * disponivel aumenta). Cada reserva troca de situacao por atualizacao condicional; o saldo so e ajustado quando a
 * troca de fato aconteceu, na mesma transacao. Repeticao ou pedido sem reserva aplicavel nao tem efeito nem erro.
 */
@Service
@Transactional
public class MovimentarReservas {

    private final SaldoEstoqueRepository saldos;
    private final ReservaEstoqueRepository reservas;

    public MovimentarReservas(SaldoEstoqueRepository saldos, ReservaEstoqueRepository reservas) {
        this.saldos = saldos;
        this.reservas = reservas;
    }

    public void liberar(Long pedidoId) {
        movimentar(pedidoId, SituacaoReserva.ATIVA, SituacaoReserva.LIBERADA, saldos::devolverReservado);
    }

    public void baixar(Long pedidoId) {
        movimentar(pedidoId, SituacaoReserva.ATIVA, SituacaoReserva.BAIXADA, saldos::baixarReservado);
    }

    public void repor(Long pedidoId) {
        movimentar(pedidoId, SituacaoReserva.BAIXADA, SituacaoReserva.REPOSTA, saldos::reporDisponivel);
    }

    private void movimentar(Long pedidoId, SituacaoReserva de, SituacaoReserva para,
            BiConsumer<Long, Integer> efeitoNoSaldo) {
        for (ReservaEstoque reserva : reservas.listarPorPedidoESituacao(pedidoId, de)) {
            if (reservas.transitar(reserva.getId(), de, para)) {
                efeitoNoSaldo.accept(reserva.getVariacaoId(), reserva.getQuantidade());
            }
        }
    }
}
