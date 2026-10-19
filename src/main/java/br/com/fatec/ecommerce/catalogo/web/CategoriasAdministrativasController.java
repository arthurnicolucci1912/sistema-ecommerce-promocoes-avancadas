package br.com.fatec.ecommerce.catalogo.web;

import br.com.fatec.ecommerce.catalogo.aplicacao.GerenciarCategorias;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Administração de categorias; as regras (nome único por pai, ciclos, remoção) ficam no domínio e no caso de uso. */
@RestController
@RequestMapping("/api/admin/categorias")
@PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR_COMERCIAL')")
@Tag(name = "Categorias (administração)",
        description = "Exige ADMIN ou OPERADOR_COMERCIAL. Nome repetido no mesmo pai, ciclo na hierarquia e remoção "
                + "de categoria com filhas ou produtos respondem 409.")
public class CategoriasAdministrativasController {

    private final GerenciarCategorias gerenciar;

    public CategoriasAdministrativasController(GerenciarCategorias gerenciar) {
        this.gerenciar = gerenciar;
    }

    @PostMapping
    @Operation(summary = "Cria uma categoria", description = "Sem paiId, a categoria é raiz.")
    public ResponseEntity<CategoriaResponse> criar(@Valid @RequestBody CategoriaRequest request) {
        CategoriaResponse criada = CategoriaResponse.de(gerenciar.criar(request.nome(), request.paiId()));
        return ResponseEntity.created(URI.create("/api/admin/categorias/" + criada.id())).body(criada);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Altera nome e pai de uma categoria",
            description = "Sem paiId, a categoria passa a ser raiz.")
    public CategoriaResponse alterar(@PathVariable Long id, @Valid @RequestBody CategoriaRequest request) {
        return CategoriaResponse.de(gerenciar.alterar(id, request.nome(), request.paiId()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove uma categoria", description = "Só categorias sem subcategorias nem produtos.")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        gerenciar.remover(id);
        return ResponseEntity.noContent().build();
    }
}
