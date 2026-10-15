package br.com.fatec.ecommerce.compartilhado.dominio;

import java.util.Map;

/**
 * Excecao base para conflito com o estado atual (ex.: e-mail ou CPF ja cadastrado). Resulta em HTTP 409.
 * Cada modulo cria excecoes proprias que estendem esta classe e, se quiserem, enriquecem a resposta de erro
 * sobrescrevendo {@link #tipo()}, {@link #titulo()} e {@link #propriedades()} (sem depender de web nem de Spring).
 */
public class ConflitoException extends RuntimeException {

    public ConflitoException(String mensagem) {
        super(mensagem);
    }

    /** Slug do {@code type} do problema; {@code "conflito"} por padrao. */
    public String tipo() {
        return "conflito";
    }

    /** Titulo do problema; {@code "Conflito"} por padrao. */
    public String titulo() {
        return "Conflito";
    }

    /** Propriedades extras do problema (imutavel); vazio por padrao. */
    public Map<String, Object> propriedades() {
        return Map.of();
    }
}
