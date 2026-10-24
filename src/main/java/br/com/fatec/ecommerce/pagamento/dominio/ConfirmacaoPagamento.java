package br.com.fatec.ecommerce.pagamento.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.DadoInvalidoException;
import java.time.Instant;

/** Resultado terminal recebido do fornecedor; sua identidade externa e unica. */
public record ConfirmacaoPagamento(String idTransacaoExterna, ResultadoCobranca.Situacao resultado, Instant recebidoEm) {
    public ConfirmacaoPagamento {
        if (idTransacaoExterna == null || idTransacaoExterna.isBlank() || idTransacaoExterna.length() > 160
                || resultado == null || resultado == ResultadoCobranca.Situacao.PENDENTE || recebidoEm == null) {
            throw new DadoInvalidoException("Confirmação de pagamento inválida.");
        }
    }
}
