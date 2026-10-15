package br.com.fatec.ecommerce.pedido.web;

import org.springframework.security.oauth2.jwt.Jwt;

/** A identidade de quem age vem sempre do token ({@code sub} = id do usuario), nunca da URL ou do corpo. */
final class UsuarioDoToken {

    private UsuarioDoToken() {
    }

    static Long id(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
