package br.com.fatec.ecommerce.compartilhado.web;

import br.com.fatec.ecommerce.compartilhado.dominio.ConflitoException;
import br.com.fatec.ecommerce.compartilhado.dominio.DadoInvalidoException;
import br.com.fatec.ecommerce.compartilhado.dominio.NaoAutenticadoException;
import br.com.fatec.ecommerce.compartilhado.dominio.RecursoNaoEncontradoException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.databind.exc.InvalidFormatException;

/**
 * Traduz excecoes em respostas Problem Details (RFC 9457): type, title, status, detail e instance.
 * Estende {@link ResponseEntityExceptionHandler}, que ja cobre as excecoes padrao do Spring MVC.
 */
@RestControllerAdvice
public class TratadorGlobalDeErros extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(TratadorGlobalDeErros.class);

    /** Campo invalido e a mensagem de validacao correspondente. */
    public record CampoInvalido(String campo, String mensagem) {
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<CampoInvalido> campos = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new CampoInvalido(e.getField(), e.getDefaultMessage()))
                .toList();
        ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "Erro de validação",
                "Um ou mais campos da requisição são inválidos.", "erro-validacao");
        problema.setProperty("campos", campos);
        return handleExceptionInternal(ex, problema, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        if (causaDeEnumInvalido(ex) instanceof InvalidFormatException formato) {
            String campo = formato.getPath().stream().map(r -> r.getPropertyName()).filter(n -> n != null)
                    .reduce((primeiro, ultimo) -> ultimo).orElse("corpo");
            String aceitos = Arrays.stream(formato.getTargetType().getEnumConstants())
                    .map(Object::toString).collect(Collectors.joining(", "));
            ProblemDetail invalido = problema(HttpStatus.BAD_REQUEST, "Valor inválido",
                    "Valor '" + formato.getValue() + "' inválido para o campo '" + campo
                            + "'. Valores aceitos: " + aceitos + ".",
                    "erro-validacao");
            invalido.setProperty("campos", List.of(new CampoInvalido(campo,
                    "valor '" + formato.getValue() + "' inválido; aceitos: " + aceitos)));
            return handleExceptionInternal(ex, invalido, headers, HttpStatus.BAD_REQUEST, request);
        }
        ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "Corpo da requisição ilegível",
                "O corpo da requisição está ausente ou malformado.", "corpo-ilegivel");
        return handleExceptionInternal(ex, problema, headers, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    ProblemDetail tratar(RecursoNaoEncontradoException ex) {
        return problema(HttpStatus.NOT_FOUND, "Recurso não encontrado", ex.getMessage(), "recurso-nao-encontrado");
    }

    @ExceptionHandler(ConflitoException.class)
    ProblemDetail tratar(ConflitoException ex) {
        ProblemDetail problema = problema(HttpStatus.CONFLICT, ex.titulo(), ex.getMessage(), ex.tipo());
        ex.propriedades().forEach(problema::setProperty);
        return problema;
    }

    @ExceptionHandler(DadoInvalidoException.class)
    ProblemDetail tratar(DadoInvalidoException ex) {
        return problema(HttpStatus.BAD_REQUEST, "Dado inválido", ex.getMessage(), "dado-invalido");
    }

    /** O autoflush de uma consulta tambem pode detectar versao obsoleta, antes do salvar explicito. */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    ProblemDetail tratar(OptimisticLockingFailureException ex) {
        return problema(HttpStatus.CONFLICT, "Conflito concorrente",
                "Os dados foram alterados por outra operação. Consulte novamente e tente outra vez.", "conflito");
    }

    /** Falha de autenticacao lancada dentro de um controller: 401, e nao 500. */
    @ExceptionHandler(NaoAutenticadoException.class)
    ResponseEntity<ProblemDetail> tratar(NaoAutenticadoException ex) {
        return naoAutenticado(ex.getMessage());
    }

    /** Excecao de autenticacao do Spring Security lancada dentro de um controller: 401, e nao 500. */
    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ProblemDetail> tratar(AuthenticationException ex) {
        return naoAutenticado("Autenticação necessária ou token inválido.");
    }

    /** Negacao de acesso lancada dentro de um controller (ex.: {@code @PreAuthorize}): 403, e nao 500. */
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemDetail> tratar(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Problemas.acessoNegado());
    }

    @ExceptionHandler(br.com.fatec.ecommerce.compartilhado.dominio.ServicoIndisponivelException.class)
    ProblemDetail tratarIndisponivel(br.com.fatec.ecommerce.compartilhado.dominio.ServicoIndisponivelException ex) {
        return problema(HttpStatus.SERVICE_UNAVAILABLE, "Serviço indisponível", ex.getMessage(), "servico-indisponivel");
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail tratarInesperado(Exception ex) {
        LOG.error("Erro inesperado ao processar a requisicao", ex);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno",
                "Ocorreu um erro inesperado. Tente novamente mais tarde.", "erro-interno");
    }

    private static ResponseEntity<ProblemDetail> naoAutenticado(String detalhe) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .body(Problemas.naoAutenticado(detalhe));
    }

    private static Throwable causaDeEnumInvalido(Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof InvalidFormatException formato && formato.getTargetType() != null
                    && formato.getTargetType().isEnum()) {
                return t;
            }
        }
        return null;
    }

    private static ProblemDetail problema(HttpStatus status, String titulo, String detalhe, String tipo) {
        return Problemas.criar(status, titulo, detalhe, tipo);
    }
}
