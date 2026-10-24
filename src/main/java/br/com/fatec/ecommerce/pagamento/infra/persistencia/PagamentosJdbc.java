package br.com.fatec.ecommerce.pagamento.infra.persistencia;

import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import br.com.fatec.ecommerce.pagamento.dominio.*;
import java.sql.*;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Adaptador SQL: compartilha a transacao JPA do pedido pelo mesmo DataSource. */
@Repository
public class PagamentosJdbc implements PagamentosRepository {
    private final JdbcTemplate jdbc;
    public PagamentosJdbc(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Override public Optional<Cobranca> buscarPorPedido(Long id) {
        return jdbc.query("SELECT * FROM cobranca WHERE pedido_id=?", PagamentosJdbc::cobranca, id).stream().findFirst();
    }
    @Override public Optional<Cobranca> buscarPorTransacao(String id) {
        return jdbc.query("SELECT * FROM cobranca WHERE id_transacao_externa=?", PagamentosJdbc::cobranca, id).stream().findFirst();
    }
    @Override public void criar(Cobranca c) {
        jdbc.update("INSERT INTO cobranca(pedido_id,fornecedor,forma,total,id_transacao_externa,situacao,criado_em,atualizado_em) VALUES(?,?,?,?,?,?,?,?)",
                c.pedidoId(), c.fornecedor(), c.forma().name(), c.total().valor(), c.idTransacaoExterna(),
                c.situacao().name(), Timestamp.from(c.criadoEm()), Timestamp.from(c.atualizadoEm()));
    }
    @Override public void atualizar(Cobranca c) {
        jdbc.update("UPDATE cobranca SET situacao=?, atualizado_em=? WHERE pedido_id=?",
                c.situacao().name(), Timestamp.from(c.atualizadoEm()), c.pedidoId());
    }
    @Override public Optional<ConfirmacaoPagamento> buscarConfirmacao(String id) {
        return jdbc.query("SELECT * FROM confirmacao_pagamento WHERE id_transacao_externa=?", (rs,n) ->
                new ConfirmacaoPagamento(rs.getString("id_transacao_externa"), ResultadoCobranca.Situacao.valueOf(rs.getString("resultado")),
                        rs.getTimestamp("recebido_em").toInstant()), id).stream().findFirst();
    }
    @Override public boolean registrar(ConfirmacaoPagamento c) {
        return jdbc.update("INSERT INTO confirmacao_pagamento(id_transacao_externa,resultado,recebido_em) VALUES(?,?,?) ON CONFLICT(id_transacao_externa) DO NOTHING",
                c.idTransacaoExterna(), c.resultado().name(), Timestamp.from(c.recebidoEm())) == 1;
    }
    @Override public Optional<Estorno> buscarEstorno(Long id) {
        return jdbc.query("SELECT * FROM estorno WHERE pedido_id=?", (rs,n) -> new Estorno(rs.getLong("pedido_id"),
                rs.getString("id_cobranca"), Dinheiro.de(rs.getBigDecimal("total")), Estorno.Situacao.valueOf(rs.getString("situacao")),
                rs.getString("id_transacao_externa"), rs.getInt("tentativas"), rs.getString("ultimo_erro"),
                rs.getTimestamp("atualizado_em").toInstant()), id).stream().findFirst();
    }
    @Override public void salvar(Estorno e) {
        jdbc.update("INSERT INTO estorno(pedido_id,id_cobranca,total,situacao,id_transacao_externa,tentativas,ultimo_erro,atualizado_em) VALUES(?,?,?,?,?,?,?,?) ON CONFLICT(pedido_id) DO UPDATE SET situacao=excluded.situacao,id_transacao_externa=excluded.id_transacao_externa,tentativas=excluded.tentativas,ultimo_erro=excluded.ultimo_erro,atualizado_em=excluded.atualizado_em",
                e.pedidoId(), e.idCobranca(), e.total().valor(), e.situacao().name(), e.idTransacaoExterna(),
                e.tentativas(), e.ultimoErro(), Timestamp.from(e.atualizadoEm()));
    }
    private static Cobranca cobranca(ResultSet rs, int n) throws SQLException {
        return new Cobranca(rs.getLong("pedido_id"), rs.getString("fornecedor"), FormaPagamento.valueOf(rs.getString("forma")),
                Dinheiro.de(rs.getBigDecimal("total")), rs.getString("id_transacao_externa"), ResultadoCobranca.Situacao.valueOf(rs.getString("situacao")),
                rs.getTimestamp("criado_em").toInstant(), rs.getTimestamp("atualizado_em").toInstant());
    }
}
