package br.com.fatec.ecommerce.seguranca.web;

import br.com.fatec.ecommerce.seguranca.aplicacao.TokenEmitido;

/** Resposta do login. {@code expiresIn} em segundos. */
public record TokenResponse(String accessToken, String tokenType, long expiresIn) {

    static TokenResponse de(TokenEmitido token) {
        return new TokenResponse(token.valor(), "Bearer", token.expiraEmSegundos());
    }
}
