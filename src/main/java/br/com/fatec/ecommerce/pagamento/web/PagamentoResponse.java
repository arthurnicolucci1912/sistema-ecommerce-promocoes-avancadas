package br.com.fatec.ecommerce.pagamento.web;

import br.com.fatec.ecommerce.pagamento.dominio.*;
import java.math.BigDecimal;
import java.time.Instant;

public record PagamentoResponse(Long pedidoId, String fornecedor, FormaPagamento forma, BigDecimal total,
        String idTransacaoExterna, ResultadoCobranca.Situacao situacao, Instant criadoEm, Instant atualizadoEm,
        EstornoResponse estorno) {
    public static PagamentoResponse de(Cobranca c, Estorno e) {
        return new PagamentoResponse(c.pedidoId(), c.fornecedor(), c.forma(), c.total().valor(), c.idTransacaoExterna(),
                c.situacao(), c.criadoEm(), c.atualizadoEm(), e == null ? null : EstornoResponse.de(e));
    }
    public record EstornoResponse(Long pedidoId, BigDecimal total, Estorno.Situacao situacao,
            String idTransacaoExterna, int tentativas, String ultimoErro, Instant atualizadoEm) {
        public static EstornoResponse de(Estorno e) {
            return new EstornoResponse(e.pedidoId(), e.total().valor(), e.situacao(), e.idTransacaoExterna(),
                    e.tentativas(), e.ultimoErro(), e.atualizadoEm());
        }
    }
}
