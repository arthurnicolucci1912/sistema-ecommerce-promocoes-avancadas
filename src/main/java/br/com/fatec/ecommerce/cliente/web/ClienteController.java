package br.com.fatec.ecommerce.cliente.web;

import br.com.fatec.ecommerce.cliente.aplicacao.CadastrarCliente;
import br.com.fatec.ecommerce.cliente.aplicacao.GerenciarCliente;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final CadastrarCliente cadastrar;
    private final GerenciarCliente gerenciar;

    public ClienteController(CadastrarCliente cadastrar, GerenciarCliente gerenciar) {
        this.cadastrar = cadastrar;
        this.gerenciar = gerenciar;
    }

    @PostMapping
    @SecurityRequirements
    @Operation(summary = "Cria uma conta de cliente",
            description = "Público. Cria o usuário (perfil CLIENTE) e o cliente na mesma transação. "
                    + "Responde 409 se o e-mail ou o CPF já estiverem cadastrados. "
                    + "O Location aponta para GET /api/clientes/me (requer login).")
    public ResponseEntity<ClienteResponse> cadastrar(@Valid @RequestBody CadastrarClienteRequest request) {
        ClienteResponse criado = ClienteResponse.de(cadastrar.executar(request.nome(), request.email(),
                request.senha(), request.cpf(), request.telefone()));
        return ResponseEntity.created(URI.create("/api/clientes/me")).body(criado);
    }

    @GetMapping("/me")
    @Operation(summary = "Consulta os próprios dados",
            description = "Exige o perfil CLIENTE (403 para outros perfis). A identidade vem do token.")
    public ClienteResponse consultar(@AuthenticationPrincipal Jwt jwt) {
        return ClienteResponse.de(gerenciar.consultar(UsuarioDoToken.id(jwt)));
    }

    @PutMapping("/me")
    @Operation(summary = "Altera nome e telefone",
            description = "Exige o perfil CLIENTE (403 para outros perfis). Somente nome e telefone são alterados; "
                    + "e-mail e CPF eventualmente enviados no corpo são ignorados.")
    public ClienteResponse alterar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AlterarClienteRequest request) {
        return ClienteResponse.de(gerenciar.alterar(UsuarioDoToken.id(jwt), request.nome(), request.telefone()));
    }
}
