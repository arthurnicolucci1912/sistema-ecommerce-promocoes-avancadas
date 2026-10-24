package br.com.fatec.ecommerce.relatorio.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.DadoInvalidoException;
import java.time.Instant;

/**
 * Período dos relatórios gerenciais: obrigatório, com as duas pontas inclusivas e {@code inicio} não posterior a
 * {@code fim}. Valida no construtor, para que nenhuma consulta seja feita com período inválido.
 */
public record PeriodoRelatorio(Instant inicio, Instant fim) {
    public PeriodoRelatorio {
        if (inicio == null || fim == null) {
            throw new DadoInvalidoException("inicio e fim são obrigatórios.");
        }
        if (inicio.isAfter(fim)) {
            throw new DadoInvalidoException("inicio não pode ser posterior a fim.");
        }
    }
}
