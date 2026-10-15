package br.com.fatec.ecommerce.seguranca.web;

import br.com.fatec.ecommerce.seguranca.aplicacao.Autenticar;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AutenticacaoController {

    private final Autenticar autenticar;

    public AutenticacaoController(Autenticar autenticar) {
        this.autenticar = autenticar;
    }

    @PostMapping("/login")
    @SecurityRequirements
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return TokenResponse.de(autenticar.executar(request.email(), request.senha()));
    }
}
