package br.com.fatec.ecommerce.seguranca.infra.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;

/** Escreve um {@link ProblemDetail} como {@code application/problem+json} (usado fora do MVC, nos filtros). */
final class RespostaProblema {

    private static final JacksonJsonHttpMessageConverter CONVERSOR = new JacksonJsonHttpMessageConverter();

    private RespostaProblema() {
    }

    static void escrever(HttpServletRequest request, HttpServletResponse response, ProblemDetail problema)
            throws IOException {
        problema.setInstance(URI.create(request.getRequestURI()));
        response.setStatus(problema.getStatus());
        CONVERSOR.write(problema, MediaType.APPLICATION_PROBLEM_JSON, new ServletServerHttpResponse(response));
    }
}
