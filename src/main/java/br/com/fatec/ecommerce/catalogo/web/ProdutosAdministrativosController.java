package br.com.fatec.ecommerce.catalogo.web;

import br.com.fatec.ecommerce.catalogo.aplicacao.GerenciarProdutos;
import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Administração de produtos e variações; o controller só traduz HTTP para o caso de uso. */
@RestController
@RequestMapping("/api/admin/produtos")
@PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR_COMERCIAL')")
@Tag(name = "Produtos (administração)",
        description = "Exige ADMIN ou OPERADOR_COMERCIAL. SKU repetido no catálogo responde 409; "
                + "cada variação cadastrada ganha o saldo de estoque inicial (zero).")
public class ProdutosAdministrativosController {

    private final GerenciarProdutos gerenciar;

    public ProdutosAdministrativosController(GerenciarProdutos gerenciar) {
        this.gerenciar = gerenciar;
    }

    @PostMapping
    @Operation(summary = "Cadastra um produto com suas variações")
    public ResponseEntity<ProdutoAdministrativoResponse> cadastrar(
            @Valid @RequestBody CadastrarProdutoRequest request) {
        ProdutoAdministrativoResponse criado = ProdutoAdministrativoResponse.de(gerenciar.cadastrar(request.nome(),
                request.descricao(), request.marca(), request.categoriaId(),
                request.variacoes().stream().map(VariacaoRequest::paraDados).toList()));
        return ResponseEntity.created(URI.create("/api/admin/produtos/" + criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Altera os dados cadastrais de um produto")
    public ProdutoAdministrativoResponse alterar(@PathVariable Long id,
            @Valid @RequestBody AlterarProdutoRequest request) {
        return ProdutoAdministrativoResponse.de(gerenciar.alterarDados(id, request.nome(), request.descricao(),
                request.marca(), request.categoriaId()));
    }

    @PostMapping("/{id}/variacoes")
    @Operation(summary = "Adiciona uma variação a um produto")
    public ResponseEntity<ProdutoAdministrativoResponse> adicionarVariacao(@PathVariable Long id,
            @Valid @RequestBody VariacaoRequest request) {
        ProdutoAdministrativoResponse produto = ProdutoAdministrativoResponse
                .de(gerenciar.adicionarVariacao(id, request.paraDados()));
        return ResponseEntity.created(URI.create("/api/admin/produtos/" + id)).body(produto);
    }

    @PutMapping("/variacoes/{id}")
    @Operation(summary = "Altera o preço de uma variação")
    public ProdutoAdministrativoResponse alterarPreco(@PathVariable Long id,
            @Valid @RequestBody AlterarPrecoRequest request) {
        return ProdutoAdministrativoResponse
                .de(gerenciar.alterarPrecoDaVariacao(id, Dinheiro.de(request.preco())));
    }

    @PostMapping("/{id}/inativacao")
    @Operation(summary = "Inativa um produto", description = "Some da vitrine; o histórico de pedidos é preservado.")
    public ResponseEntity<Void> inativar(@PathVariable Long id) {
        gerenciar.inativarProduto(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/variacoes/{id}/inativacao")
    @Operation(summary = "Inativa uma variação")
    public ResponseEntity<Void> inativarVariacao(@PathVariable Long id) {
        gerenciar.inativarVariacao(id);
        return ResponseEntity.noContent().build();
    }
}
