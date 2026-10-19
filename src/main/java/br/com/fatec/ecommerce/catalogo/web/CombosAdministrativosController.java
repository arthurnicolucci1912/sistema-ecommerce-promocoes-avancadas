package br.com.fatec.ecommerce.catalogo.web;

import br.com.fatec.ecommerce.catalogo.aplicacao.ComponenteDeCombo;
import br.com.fatec.ecommerce.catalogo.aplicacao.GerenciarCombos;
import br.com.fatec.ecommerce.catalogo.dominio.Combo;
import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Administração de combos; ciclos e componentes inexistentes são rejeitados pelo domínio e pelo caso de uso. */
@RestController
@RequestMapping("/api/admin/combos")
@PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR_COMERCIAL')")
@Tag(name = "Combos (administração)",
        description = "Exige ADMIN ou OPERADOR_COMERCIAL. SKU repetido e combo que contém a si mesmo respondem 409; "
                + "componente inexistente responde 400.")
public class CombosAdministrativosController {

    private final GerenciarCombos gerenciar;

    public CombosAdministrativosController(GerenciarCombos gerenciar) {
        this.gerenciar = gerenciar;
    }

    @PostMapping
    @Operation(summary = "Cadastra um combo")
    public ResponseEntity<ComboAdministrativoResponse> cadastrar(@Valid @RequestBody CadastrarComboRequest request) {
        ComboAdministrativoResponse criado = ComboAdministrativoResponse.de(gerenciar.cadastrar(request.sku(),
                request.nome(), Dinheiro.de(request.preco()), componentes(request.componentes())));
        return ResponseEntity.created(URI.create("/api/admin/combos/" + criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Altera o preço e/ou os componentes de um combo",
            description = "Informe ao menos um dos dois (400 caso contrário). Os componentes substituem os atuais.")
    public ComboAdministrativoResponse alterar(@PathVariable Long id,
            @Valid @RequestBody AlterarComboRequest request) {
        Combo combo = gerenciar.alterar(id, request.preco() == null ? null : Dinheiro.de(request.preco()),
                request.componentes() == null ? null : componentes(request.componentes()));
        return ComboAdministrativoResponse.de(combo);
    }

    @PostMapping("/{id}/inativacao")
    @Operation(summary = "Inativa um combo")
    public ResponseEntity<Void> inativar(@PathVariable Long id) {
        gerenciar.inativar(id);
        return ResponseEntity.noContent().build();
    }

    private static List<ComponenteDeCombo> componentes(List<ComponenteDeComboRequest> requests) {
        return requests.stream().map(ComponenteDeComboRequest::paraDados).toList();
    }
}
