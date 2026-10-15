package br.com.fatec.ecommerce.compartilhado.web;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/** Fabrica de {@link ProblemDetail} no formato unico da API (type {@code urn:ecommerce:erro:*}). */
public final class Problemas {

    private Problemas() {
    }

    public static ProblemDetail criar(HttpStatus status, String titulo, String detalhe, String tipo) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setTitle(titulo);
        problema.setType(URI.create("urn:ecommerce:erro:" + tipo));
        return problema;
    }

    public static ProblemDetail naoAutenticado(String detalhe) {
        return criar(HttpStatus.UNAUTHORIZED, "Não autenticado", detalhe, "nao-autenticado");
    }

    public static ProblemDetail acessoNegado() {
        return criar(HttpStatus.FORBIDDEN, "Acesso negado",
                "O usuário autenticado não tem permissão para esta operação.", "acesso-negado");
    }
}
