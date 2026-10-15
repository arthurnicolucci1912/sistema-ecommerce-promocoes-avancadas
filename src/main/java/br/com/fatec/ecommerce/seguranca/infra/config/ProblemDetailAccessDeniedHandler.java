package br.com.fatec.ecommerce.seguranca.infra.config;

import br.com.fatec.ecommerce.compartilhado.web.Problemas;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/** 403 em Problem Details quando o usuario autenticado nao tem o perfil exigido. */
@Component
class ProblemDetailAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        RespostaProblema.escrever(request, response, Problemas.acessoNegado());
    }
}
