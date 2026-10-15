package br.com.fatec.ecommerce.pedido.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.ConflitoException;
import java.util.Map;

/**
 * Transicao do ciclo de vida que o estado atual do pedido nao admite (HTTP 409). Expoe a situacao em que o
 * pedido estava e a operacao tentada, para a resposta de erro.
 */
public class TransicaoInvalidaException extends ConflitoException {

    private final SituacaoPedido situacaoAtual;
    private final OperacaoPedido operacao;

    public TransicaoInvalidaException(SituacaoPedido situacaoAtual, OperacaoPedido operacao) {
        this(situacaoAtual, operacao, "Operação " + operacao + " não é permitida para um pedido em "
                + situacaoAtual + ".");
    }

    public TransicaoInvalidaException(SituacaoPedido situacaoAtual, OperacaoPedido operacao, String mensagem) {
        super(mensagem);
        this.situacaoAtual = situacaoAtual;
        this.operacao = operacao;
    }

    public SituacaoPedido getSituacaoAtual() {
        return situacaoAtual;
    }

    public OperacaoPedido getOperacao() {
        return operacao;
    }

    @Override
    public String tipo() {
        return "transicao-invalida";
    }

    @Override
    public String titulo() {
        return "Transição inválida";
    }

    /** {@code situacaoAtual} e {@code operacao}, acrescentadas ao problema da resposta 409. */
    @Override
    public Map<String, Object> propriedades() {
        return Map.of("situacaoAtual", situacaoAtual, "operacao", operacao);
    }
}
