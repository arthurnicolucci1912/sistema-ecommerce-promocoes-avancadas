package br.com.fatec.ecommerce.seguranca.web;

import br.com.fatec.ecommerce.seguranca.aplicacao.GerenciarUsuariosAdministrativos;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/usuarios")
public class UsuariosAdministrativosController {

    private final GerenciarUsuariosAdministrativos gerenciar;

    public UsuariosAdministrativosController(GerenciarUsuariosAdministrativos gerenciar) {
        this.gerenciar = gerenciar;
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> criar(@Valid @RequestBody CriarUsuarioAdministrativoRequest request) {
        UsuarioResponse criado = UsuarioResponse.de(gerenciar.criar(request.email(), request.senha(), request.perfis()));
        return ResponseEntity.created(URI.create("/api/admin/usuarios/" + criado.id())).body(criado);
    }

    @GetMapping
    public List<UsuarioResponse> listar() {
        return gerenciar.listar().stream().map(UsuarioResponse::de).toList();
    }

    @PutMapping("/{id}/perfis")
    public UsuarioResponse alterarPerfis(@PathVariable Long id, @Valid @RequestBody AlterarPerfisRequest request) {
        return UsuarioResponse.de(gerenciar.alterarPerfis(id, request.perfis()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> inativar(@PathVariable Long id) {
        gerenciar.inativar(id);
        return ResponseEntity.noContent().build();
    }
}
