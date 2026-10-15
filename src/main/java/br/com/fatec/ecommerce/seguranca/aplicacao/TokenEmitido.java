package br.com.fatec.ecommerce.seguranca.aplicacao;

/** Token de acesso emitido e sua validade em segundos. */
public record TokenEmitido(String valor, long expiraEmSegundos) {
}
