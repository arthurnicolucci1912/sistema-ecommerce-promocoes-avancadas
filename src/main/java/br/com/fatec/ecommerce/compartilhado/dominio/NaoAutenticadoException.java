package br.com.fatec.ecommerce.compartilhado.dominio;

/**
 * Excecao base para falha de autenticacao (ex.: credenciais invalidas). Resulta em HTTP 401.
 * Cada modulo cria excecoes proprias que estendem esta classe.
 */
public class NaoAutenticadoException extends RuntimeException {

    public NaoAutenticadoException(String mensagem) {
        super(mensagem);
    }
}
