package br.com.fatec.ecommerce.catalogo.web;

import br.com.fatec.ecommerce.catalogo.aplicacao.ConsultarCatalogo;
import br.com.fatec.ecommerce.catalogo.aplicacao.FiltroDeProdutos;
import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import br.com.fatec.ecommerce.compartilhado.web.PaginaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Vitrine pública do catálogo (somente leitura, sem autenticação). Só aparecem itens ativos e com ao menos uma
 * variação ativa; a disponibilidade vem do estoque pelo caso de uso, não é calculada aqui.
 */
@RestController
@RequestMapping("/api/catalogo")
@Tag(name = "Catálogo (público)",
        description = "Consulta sem autenticação. Produtos e combos inativos ou sem variação ativa respondem 404.")
public class CatalogoPublicoController {

    private final ConsultarCatalogo consultar;

    public CatalogoPublicoController(ConsultarCatalogo consultar) {
        this.consultar = consultar;
    }

    @GetMapping("/categorias")
    @Operation(summary = "Árvore de categorias", description = "Categorias raiz com as subcategorias em 'filhas'.")
    public List<NoDeCategoriaResponse> categorias() {
        return consultar.arvoreDeCategorias().stream().map(NoDeCategoriaResponse::de).toList();
    }

    @GetMapping("/produtos")
    @Operation(summary = "Lista produtos",
            description = "Filtros opcionais: categoria (inclui as subcategorias), trecho do nome e faixa de preço. "
                    + "Paginado (page a partir de 0; size padrão 20, máximo 100).")
    public PaginaResponse<ProdutoResumoResponse> produtos(@RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) String nome, @RequestParam(required = false) BigDecimal precoMin,
            @RequestParam(required = false) BigDecimal precoMax, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + ConsultarCatalogo.TAMANHO_PADRAO) int size) {
        FiltroDeProdutos filtro = new FiltroDeProdutos(categoriaId, nome,
                precoMin == null ? null : Dinheiro.de(precoMin), precoMax == null ? null : Dinheiro.de(precoMax));
        return PaginaResponse.de(consultar.produtos(filtro, page, size), ProdutoResumoResponse::de);
    }

    @GetMapping("/produtos/{id}")
    @Operation(summary = "Detalhe de um produto", description = "Variações ativas com a disponibilidade de cada uma.")
    public ProdutoDetalheResponse produto(@PathVariable Long id) {
        return ProdutoDetalheResponse.de(consultar.detalheProduto(id));
    }

    @GetMapping("/combos")
    @Operation(summary = "Lista combos disponíveis para venda")
    public List<ComboResponse> combos() {
        return consultar.combos().stream().map(ComboResponse::de).toList();
    }

    @GetMapping("/combos/{id}")
    @Operation(summary = "Detalhe de um combo", description = "Componentes e disponibilidade do combo.")
    public ComboResponse combo(@PathVariable Long id) {
        return ComboResponse.de(consultar.detalheCombo(id));
    }
}
