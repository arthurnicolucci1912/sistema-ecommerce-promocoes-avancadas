package br.com.fatec.ecommerce.pagamento.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.*;
import java.time.Instant;

/** Snapshot da cobranca; nao recalcula o total do pedido nem permite mudar de fornecedor. */
public record Cobranca(Long pedidoId, String fornecedor, FormaPagamento forma, Dinheiro total,
        String idTransacaoExterna, ResultadoCobranca.Situacao situacao, Instant criadoEm, Instant atualizadoEm) {
    public Cobranca {
        if (pedidoId == null || pedidoId <= 0 || fornecedor == null || fornecedor.isBlank()
                || fornecedor.length() > 80 || forma == null || total == null || total.ehNegativo()
                || total.valor().precision() > 12 || idTransacaoExterna == null || idTransacaoExterna.isBlank()
                || idTransacaoExterna.length() > 160 || situacao == null || criadoEm == null
                || atualizadoEm == null || atualizadoEm.isBefore(criadoEm)) {
            throw new DadoInvalidoException("Cobrança inválida.");
        }
    }
    public Cobranca confirmar(ResultadoCobranca.Situacao resultado, Instant momento) {
        new ConfirmacaoPagamento(idTransacaoExterna, resultado, momento);
        if (situacao != ResultadoCobranca.Situacao.PENDENTE && situacao != resultado) {
            throw new ConflitoException("A transação já possui outro resultado.");
        }
        return new Cobranca(pedidoId, fornecedor, forma, total, idTransacaoExterna, resultado, criadoEm, momento);
    }
}
