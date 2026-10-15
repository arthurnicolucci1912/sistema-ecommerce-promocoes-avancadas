package br.com.fatec.ecommerce.compartilhado.dominio;

/**
 * Excecao base para requisicao que viola uma regra de negocio de entrada (ex.: perfil nao permitido
 * na operacao). Resulta em HTTP 400. Cada modulo cria excecoes proprias que estendem esta classe.
 */
public class DadoInvalidoException extends RuntimeException {

    public DadoInvalidoException(String mensagem) {
        super(mensagem);
    }
}
