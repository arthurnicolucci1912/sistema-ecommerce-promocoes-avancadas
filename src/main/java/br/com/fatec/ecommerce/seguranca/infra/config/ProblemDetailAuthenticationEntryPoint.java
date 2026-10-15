package br.com.fatec.ecommerce.seguranca.infra.config;

import br.com.fatec.ecommerce.compartilhado.web.Problemas;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/** 401 em Problem Details quando falta token ou o token e invalido, expirado ou adulterado. */
@Component
class ProblemDetailAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        RespostaProblema.escrever(request, response,
                Problemas.naoAutenticado("Autenticação necessária ou token inválido."));
    }
}
