package br.com.fatec.ecommerce.auditoria.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.OperacaoAdministrativa;
import java.time.Instant;
import java.util.Map;

/** Snapshot somente leitura da trilha; nenhum objeto persistente escapa para a API. */
public record RegistroAuditoriaInfo(Long id, Long usuarioId, Instant ocorridoEm, String operacao,
        String recursoTipo, Long recursoId, Map<String, Object> detalhes) {
    public RegistroAuditoriaInfo {
        detalhes = new OperacaoAdministrativa(usuarioId, ocorridoEm, operacao, recursoTipo, recursoId, detalhes).detalhes();
    }
}
