package br.com.fatec.ecommerce.compartilhado.dominio;

/**
 * Excecao base para recurso inexistente (ou inacessivel ao solicitante). Resulta em HTTP 404.
 * Cada modulo cria excecoes proprias que estendem esta classe.
 */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
