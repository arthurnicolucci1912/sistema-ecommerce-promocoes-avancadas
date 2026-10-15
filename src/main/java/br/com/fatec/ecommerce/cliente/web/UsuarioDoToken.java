package br.com.fatec.ecommerce.cliente.web;

import org.springframework.security.oauth2.jwt.Jwt;

/** A identidade do cliente vem sempre do token ({@code sub} = id do usuario), nunca da URL ou do corpo. */
final class UsuarioDoToken {

    private UsuarioDoToken() {
    }

    static Long id(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
