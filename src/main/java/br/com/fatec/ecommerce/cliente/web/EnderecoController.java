package br.com.fatec.ecommerce.cliente.web;

import br.com.fatec.ecommerce.cliente.aplicacao.GerenciarEnderecos;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clientes/me/enderecos")
@Tag(name = "Endereços do cliente",
        description = "Exige o perfil CLIENTE. O endereço é sempre buscado pelo cliente do token: "
                + "endereço inexistente, inativo ou de outro cliente responde 404.")
public class EnderecoController {

    private final GerenciarEnderecos gerenciar;

    public EnderecoController(GerenciarEnderecos gerenciar) {
        this.gerenciar = gerenciar;
    }

    @GetMapping
    @Operation(summary = "Lista os endereços ativos")
    public List<EnderecoResponse> listar(@AuthenticationPrincipal Jwt jwt) {
        return gerenciar.listar(UsuarioDoToken.id(jwt)).stream().map(EnderecoResponse::de).toList();
    }

    @PostMapping
    @Operation(summary = "Cadastra um endereço",
            description = "O primeiro endereço ativo do cliente torna-se o principal.")
    public ResponseEntity<EnderecoResponse> cadastrar(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody EnderecoRequest request) {
        EnderecoResponse criado = EnderecoResponse
                .de(gerenciar.cadastrar(UsuarioDoToken.id(jwt), request.paraDados()));
        return ResponseEntity.created(URI.create("/api/clientes/me/enderecos/" + criado.id())).body(criado);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta um endereço")
    public EnderecoResponse consultar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return EnderecoResponse.de(gerenciar.consultar(UsuarioDoToken.id(jwt), id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Altera um endereço", description = "Não altera qual endereço é o principal.")
    public EnderecoResponse alterar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
            @Valid @RequestBody EnderecoRequest request) {
        return EnderecoResponse.de(gerenciar.alterar(UsuarioDoToken.id(jwt), id, request.paraDados()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove (inativa) um endereço",
            description = "Se era o principal, o endereço ativo mais antigo restante passa a ser o principal.")
    public ResponseEntity<Void> remover(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        gerenciar.remover(UsuarioDoToken.id(jwt), id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/principal")
    @Operation(summary = "Marca o endereço como principal", description = "Desmarca o principal anterior.")
    public EnderecoResponse marcarPrincipal(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return EnderecoResponse.de(gerenciar.marcarPrincipal(UsuarioDoToken.id(jwt), id));
    }
}
