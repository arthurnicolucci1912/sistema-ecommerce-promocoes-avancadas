package br.com.fatec.ecommerce.compartilhado.web;

import br.com.fatec.ecommerce.compartilhado.dominio.ConflitoException;
import br.com.fatec.ecommerce.compartilhado.dominio.RecursoNaoEncontradoException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Controller existente apenas nos testes, para disparar cada tipo de erro. */
@RestController
class ErrosDeTesteController {

    record Entrada(@NotBlank(message = "o nome é obrigatório") String nome,
            @Email(message = "e-mail inválido") @NotBlank(message = "o e-mail é obrigatório") String email) {
    }

    @PostMapping("/teste/validacao")
    void validar(@Valid @RequestBody Entrada entrada) {
    }

    @GetMapping("/teste/nao-encontrado")
    void naoEncontrado() {
        throw new RecursoNaoEncontradoException("Pedido 99 não encontrado.");
    }

    @GetMapping("/teste/conflito")
    void conflito() {
        throw new ConflitoException("E-mail já cadastrado.");
    }

    @GetMapping("/teste/falha")
    void falha() {
        throw new IllegalStateException("segredo interno br.com.fatec.ecommerce.Interna");
    }

    @GetMapping("/teste/protegido")
    @PreAuthorize("hasRole('ADMIN')")
    public String protegido() {
        return "ok";
    }

    @GetMapping("/teste/autenticacao")
    void autenticacao() {
        throw new BadCredentialsException("detalhe interno");
    }
}
