package br.com.fatec.ecommerce.auditoria.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.DadoInvalidoException;
import java.time.Instant;

/** Filtros combinados por igualdade e intervalo inclusivo; paginação sempre limitada. */
public record FiltroAuditoria(Long usuarioId, String operacao, Instant inicio, Instant fim, int page, int size) {
    public FiltroAuditoria {
        if (usuarioId != null && usuarioId <= 0) {
            throw new DadoInvalidoException("usuarioId deve ser positivo.");
        }
        if (operacao != null && !operacao.matches("[A-Z][A-Z0-9_]{0,99}")) {
            throw new DadoInvalidoException("operacao deve ter até 100 caracteres: letras maiúsculas, números e sublinhado.");
        }
        if (inicio != null && fim != null && inicio.isAfter(fim)) {
            throw new DadoInvalidoException("inicio não pode ser posterior a fim.");
        }
        if (page < 0 || size < 1 || size > 100) {
            throw new DadoInvalidoException("page deve ser não negativo e size deve estar entre 1 e 100.");
        }
    }
}
