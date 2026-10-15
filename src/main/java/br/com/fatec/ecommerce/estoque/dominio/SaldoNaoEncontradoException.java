package br.com.fatec.ecommerce.estoque.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.RecursoNaoEncontradoException;

/** Nao existe saldo de estoque para a variacao informada. Resulta em HTTP 404. */
public class SaldoNaoEncontradoException extends RecursoNaoEncontradoException {

    public SaldoNaoEncontradoException(Long variacaoId) {
        super("Não há saldo de estoque para a variação " + variacaoId + ".");
    }
}
