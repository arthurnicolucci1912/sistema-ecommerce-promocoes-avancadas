package br.com.fatec.ecommerce.auditoria.infra.persistencia;

import br.com.fatec.ecommerce.auditoria.dominio.ConsultarRegistrosAuditoria;
import br.com.fatec.ecommerce.auditoria.dominio.FiltroAuditoria;
import br.com.fatec.ecommerce.auditoria.dominio.RegistroAuditoriaInfo;
import br.com.fatec.ecommerce.compartilhado.dominio.Pagina;
import java.sql.Timestamp;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/** Predicados, COUNT, LIMIT e OFFSET executados no PostgreSQL, com parâmetros vinculados. */
@Repository
class ConsultarRegistrosAuditoriaJdbc implements ConsultarRegistrosAuditoria {
    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper mapper;

    ConsultarRegistrosAuditoriaJdbc(NamedParameterJdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    @Override
    public Pagina<RegistroAuditoriaInfo> consultar(FiltroAuditoria filtro) {
        var parametros = new MapSqlParameterSource();
        var where = new StringBuilder(" FROM auditoria_operacao WHERE 1=1");
        if (filtro.usuarioId() != null) {
            where.append(" AND usuario_id = :usuarioId");
            parametros.addValue("usuarioId", filtro.usuarioId());
        }
        if (filtro.operacao() != null) {
            where.append(" AND operacao = :operacao");
            parametros.addValue("operacao", filtro.operacao());
        }
        if (filtro.inicio() != null) {
            where.append(" AND ocorrido_em >= :inicio");
            parametros.addValue("inicio", Timestamp.from(filtro.inicio()));
        }
        if (filtro.fim() != null) {
            where.append(" AND ocorrido_em <= :fim");
            parametros.addValue("fim", Timestamp.from(filtro.fim()));
        }
        long total = jdbc.queryForObject("SELECT COUNT(*)" + where, parametros, Long.class);
        parametros.addValue("limite", filtro.size());
        parametros.addValue("deslocamento", (long) filtro.page() * filtro.size());
        var conteudo = jdbc.query("SELECT id, usuario_id, ocorrido_em, operacao, recurso_tipo, recurso_id, detalhes"
                + where + " ORDER BY ocorrido_em DESC, id DESC LIMIT :limite OFFSET :deslocamento", parametros,
                (rs, linha) -> new RegistroAuditoriaInfo(rs.getLong("id"), rs.getLong("usuario_id"),
                        rs.getTimestamp("ocorrido_em").toInstant(), rs.getString("operacao"),
                        rs.getString("recurso_tipo"), rs.getLong("recurso_id"),
                        mapper.readValue(rs.getString("detalhes"), new TypeReference<Map<String, Object>>() {})));
        return new Pagina<>(conteudo, filtro.page(), filtro.size(), total);
    }
}
