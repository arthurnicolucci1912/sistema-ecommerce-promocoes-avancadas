package br.com.fatec.ecommerce.pagamento.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.*;
import java.time.Instant;

/** Registro recuperavel de devolucao integral, inclusive pendencia legada sem cobranca conhecida. */
public record Estorno(Long pedidoId, String idCobranca, Dinheiro total, Situacao situacao,
        String idTransacaoExterna, int tentativas, String ultimoErro, Instant atualizadoEm) {
    public enum Situacao { PENDENTE, FALHOU, CONCLUIDO }
    public Estorno {
        if (pedidoId == null || pedidoId <= 0 || total == null || total.ehNegativo()
                || total.valor().precision() > 12 || situacao == null || tentativas < 0 || atualizadoEm == null
                || (idCobranca != null && (idCobranca.isBlank() || idCobranca.length() > 160))
                || (idTransacaoExterna != null && idTransacaoExterna.length() > 160)
                || (ultimoErro != null && ultimoErro.length() > 500)
                || (situacao == Situacao.CONCLUIDO && (idCobranca == null || idTransacaoExterna == null
                    || idTransacaoExterna.isBlank() || tentativas == 0 || ultimoErro != null))
                || (situacao == Situacao.FALHOU && (ultimoErro == null || ultimoErro.isBlank()))) {
            throw new DadoInvalidoException("Estorno inválido.");
        }
    }
    public Estorno registrar(ResultadoEstorno resultado, String erro, Instant momento) {
        if (situacao == Situacao.CONCLUIDO) return this;
        boolean sucesso = resultado != null && resultado.estornado();
        return new Estorno(pedidoId, idCobranca, total, sucesso ? Situacao.CONCLUIDO : Situacao.FALHOU,
                sucesso ? resultado.idTransacaoExterna() : null, Math.addExact(tentativas, 1),
                sucesso ? null : (erro == null || erro.isBlank() ? "Estorno recusado pelo fornecedor." : erro), momento);
    }
}
