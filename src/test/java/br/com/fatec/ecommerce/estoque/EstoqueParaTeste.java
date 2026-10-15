package br.com.fatec.ecommerce.estoque;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Apoio aos testes do estoque: grava e le saldos e reservas direto no banco (sem depender do catalogo). */
@Component
public class EstoqueParaTeste {

    private final JdbcTemplate jdbc;

    public EstoqueParaTeste(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Grava (ou substitui) o saldo da variacao. */
    public void definirSaldo(long variacaoId, int disponivel, int reservado) {
        jdbc.update("DELETE FROM saldo_estoque WHERE variacao_id = ?", variacaoId);
        jdbc.update("INSERT INTO saldo_estoque (variacao_id, disponivel, reservado) VALUES (?, ?, ?)", variacaoId,
                disponivel, reservado);
    }

    public int disponivel(long variacaoId) {
        return jdbc.queryForObject("SELECT disponivel FROM saldo_estoque WHERE variacao_id = ?", Integer.class,
                variacaoId);
    }

    public int reservado(long variacaoId) {
        return jdbc.queryForObject("SELECT reservado FROM saldo_estoque WHERE variacao_id = ?", Integer.class,
                variacaoId);
    }

    public boolean existeSaldo(long variacaoId) {
        return jdbc.queryForObject("SELECT count(*) FROM saldo_estoque WHERE variacao_id = ?", Integer.class,
                variacaoId) > 0;
    }

    public List<String> situacoesDaReserva(long pedidoId) {
        return jdbc.queryForList("SELECT situacao FROM reserva_estoque WHERE pedido_id = ? ORDER BY variacao_id",
                String.class, pedidoId);
    }

    public int quantidadeDeReservas() {
        return jdbc.queryForObject("SELECT count(*) FROM reserva_estoque", Integer.class);
    }
}
