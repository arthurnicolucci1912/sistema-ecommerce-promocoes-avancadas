package br.com.fatec.ecommerce.compartilhado.dominio;

/** Falha transitoria de uma dependencia necessaria, sem resultado comercial seguro. */
public class ServicoIndisponivelException extends RuntimeException {
    public ServicoIndisponivelException(String mensagem) { super(mensagem); }
}
