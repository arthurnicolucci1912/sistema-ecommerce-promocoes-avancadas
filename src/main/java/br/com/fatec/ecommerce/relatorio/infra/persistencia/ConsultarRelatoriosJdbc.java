package br.com.fatec.ecommerce.relatorio.infra.persistencia;

import br.com.fatec.ecommerce.relatorio.dominio.ConsultarRelatorios;
import br.com.fatec.ecommerce.relatorio.dominio.DescontoPorCampanha;
import br.com.fatec.ecommerce.relatorio.dominio.FiltroProdutosMaisVendidos;
import br.com.fatec.ecommerce.relatorio.dominio.PeriodoRelatorio;
import br.com.fatec.ecommerce.relatorio.dominio.ProdutoMaisVendido;
import br.com.fatec.ecommerce.relatorio.dominio.ResumoVendas;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Consultas de leitura sobre os snapshots dos pedidos, com parâmetros vinculados. Venda: pedido com transição para
 * PAGO no período (inclusivo) cuja situação atual não é CANCELADO.
 */
@Repository
class ConsultarRelatoriosJdbc implements ConsultarRelatorios {
    private static final String VENDAS = """
            WITH vendas AS (
                SELECT p.id, p.total FROM pedido p
                WHERE p.situacao <> 'CANCELADO'
                  AND EXISTS (SELECT 1 FROM pedido_historico h
                              WHERE h.pedido_id = p.id AND h.para = 'PAGO'
                                AND h.ocorrido_em >= :inicio AND h.ocorrido_em <= :fim))
            """;

    private final NamedParameterJdbcTemplate jdbc;

    ConsultarRelatoriosJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public ResumoVendas resumoDeVendas(PeriodoRelatorio periodo) {
        return jdbc.queryForObject(VENDAS + """
                SELECT (SELECT COALESCE(SUM(total), 0) FROM vendas) AS valor_total,
                       (SELECT COUNT(*) FROM vendas) AS quantidade,
                       (SELECT COALESCE(SUM(d.valor), 0) FROM pedido_desconto d JOIN vendas v ON v.id = d.pedido_id) AS descontos,
                       (SELECT COUNT(*) FROM pedido_historico
                         WHERE para = 'CANCELADO' AND ocorrido_em >= :inicio AND ocorrido_em <= :fim) AS cancelamentos
                """, parametros(periodo),
                (rs, linha) -> ResumoVendas.de(rs.getBigDecimal("valor_total"), rs.getLong("quantidade"),
                        rs.getBigDecimal("descontos"), rs.getLong("cancelamentos")));
    }

    @Override
    public List<ProdutoMaisVendido> produtosMaisVendidos(FiltroProdutosMaisVendidos filtro) {
        return jdbc.query(VENDAS + """
                SELECT i.sku, i.nome_produto, i.descricao_variacao,
                       SUM(i.quantidade) AS quantidade, SUM(i.total_item) AS valor
                FROM pedido_item i JOIN vendas v ON v.id = i.pedido_id
                GROUP BY i.sku, i.nome_produto, i.descricao_variacao
                ORDER BY quantidade DESC, i.sku ASC, i.nome_produto ASC, i.descricao_variacao ASC NULLS FIRST
                LIMIT :limite
                """, parametros(filtro.periodo()).addValue("limite", filtro.limite()),
                (rs, linha) -> new ProdutoMaisVendido(rs.getString("sku"), rs.getString("nome_produto"),
                        rs.getString("descricao_variacao"), rs.getLong("quantidade"), rs.getBigDecimal("valor")));
    }

    @Override
    public List<DescontoPorCampanha> descontosPorCampanha(PeriodoRelatorio periodo) {
        return jdbc.query(VENDAS + """
                SELECT d.origem, d.referencia_id, MAX(d.descricao) AS descricao_campanha, MAX(d.codigo) AS codigo_cupom,
                       COUNT(DISTINCT d.pedido_id) AS pedidos, SUM(d.valor) AS valor
                FROM pedido_desconto d JOIN vendas v ON v.id = d.pedido_id
                GROUP BY d.origem, d.referencia_id, CASE WHEN d.referencia_id IS NULL THEN d.descricao END
                ORDER BY pedidos DESC, valor DESC, descricao_campanha ASC, d.origem ASC, d.referencia_id ASC NULLS FIRST
                """, parametros(periodo),
                (rs, linha) -> new DescontoPorCampanha(rs.getString("origem"), (Long) rs.getObject("referencia_id"),
                        rs.getString("descricao_campanha"), rs.getString("codigo_cupom"), rs.getLong("pedidos"),
                        rs.getBigDecimal("valor")));
    }

    private static MapSqlParameterSource parametros(PeriodoRelatorio periodo) {
        return new MapSqlParameterSource()
                .addValue("inicio", Timestamp.from(periodo.inicio()))
                .addValue("fim", Timestamp.from(periodo.fim()));
    }
}
