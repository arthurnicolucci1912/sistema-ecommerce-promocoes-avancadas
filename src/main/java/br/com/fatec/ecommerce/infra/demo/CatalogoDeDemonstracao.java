package br.com.fatec.ecommerce.infra.demo;

import br.com.fatec.ecommerce.catalogo.aplicacao.ComponenteDeCombo;
import br.com.fatec.ecommerce.catalogo.aplicacao.GerenciarCategorias;
import br.com.fatec.ecommerce.catalogo.aplicacao.GerenciarCombos;
import br.com.fatec.ecommerce.catalogo.aplicacao.GerenciarProdutos;
import br.com.fatec.ecommerce.catalogo.aplicacao.NovaVariacao;
import br.com.fatec.ecommerce.catalogo.dominio.Categoria;
import br.com.fatec.ecommerce.catalogo.dominio.CategoriaRepository;
import br.com.fatec.ecommerce.catalogo.dominio.ItemVendavelRepository;
import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import br.com.fatec.ecommerce.estoque.aplicacao.AjustarEstoque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Semeia, apenas no perfil {@code dev}, o catalogo e o estoque de demonstracao (dados ficticios): categorias
 * Roupas &gt; Camisetas, Casa &gt; Cozinha e Eletronicos &gt; Perifericos; Camiseta Basica (P, M e G), Caneca,
 * Cafe e Teclado; o combo "Kit Cafe" (1 caneca + 2 cafes); e o estoque inicial, com o Teclado em 1 unidade para
 * demonstrar a disputa da ultima unidade. Usa os casos de uso reais (o combo exercita o Composite).
 *
 * <p>Idempotente: categoria pelo nome (entre irmas), produto e combo pelo SKU; o estoque inicial so e lancado
 * junto com a criacao do produto, na mesma transacao, entao reiniciar nao soma estoque de novo.
 *
 * <p>Os casos de uso de catalogo exigem {@code ADMIN} ou {@code OPERADOR_COMERCIAL} ({@code @PreAuthorize}); como
 * a carga roda na subida, sem usuario autenticado, ela executa com um contexto de seguranca de sistema temporario
 * (authority {@code ROLE_ADMIN}), restaurado no {@code finally}. A seguranca nao e desabilitada.
 */
@Component
@Profile("dev")
class CatalogoDeDemonstracao {

    static final String MOTIVO = "carga de demonstração";

    /** SKUs fixos, tambem usados pelo pedido de exemplo e pelos testes. */
    static final String SKU_CAMISETA_P = "CAM-BAS-P";
    static final String SKU_CAMISETA_M = "CAM-BAS-M";
    static final String SKU_CAMISETA_G = "CAM-BAS-G";
    static final String SKU_CANECA = "CAN-CER-300";
    static final String SKU_CAFE = "CAF-ESP-250";
    static final String SKU_TECLADO = "TEC-MEC-01";
    static final String SKU_KIT_CAFE = "KIT-CAFE";

    private static final Logger LOG = LoggerFactory.getLogger(CatalogoDeDemonstracao.class);

    private final GerenciarCategorias gerenciarCategorias;
    private final GerenciarProdutos gerenciarProdutos;
    private final GerenciarCombos gerenciarCombos;
    private final AjustarEstoque ajustarEstoque;
    private final CategoriaRepository categorias;
    private final ItemVendavelRepository itens;
    private final TransactionTemplate transacao;

    CatalogoDeDemonstracao(GerenciarCategorias gerenciarCategorias, GerenciarProdutos gerenciarProdutos,
            GerenciarCombos gerenciarCombos, AjustarEstoque ajustarEstoque, CategoriaRepository categorias,
            ItemVendavelRepository itens, PlatformTransactionManager gerenteDeTransacao) {
        this.gerenciarCategorias = gerenciarCategorias;
        this.gerenciarProdutos = gerenciarProdutos;
        this.gerenciarCombos = gerenciarCombos;
        this.ajustarEstoque = ajustarEstoque;
        this.categorias = categorias;
        this.itens = itens;
        this.transacao = new TransactionTemplate(gerenteDeTransacao);
    }

    /** Garante categorias, produtos, combo e estoque inicial, tudo em uma transacao e com perfil de sistema. */
    void semear() {
        SecurityContext anterior = SecurityContextHolder.getContext();
        SecurityContext sistema = SecurityContextHolder.createEmptyContext();
        sistema.setAuthentication(UsernamePasswordAuthenticationToken.authenticated("carga-de-demonstracao", null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        SecurityContextHolder.setContext(sistema);
        try {
            transacao.executeWithoutResult(status -> semearCatalogoEEstoque());
        } finally {
            SecurityContextHolder.setContext(anterior);
        }
    }

    private void semearCatalogoEEstoque() {
        Long camisetas = categoria("Camisetas", categoria("Roupas", null));
        Long cozinha = categoria("Cozinha", categoria("Casa", null));
        Long perifericos = categoria("Periféricos", categoria("Eletrônicos", null));

        if (!itens.existePorSku(SKU_CAMISETA_P)) {
            gerenciarProdutos.cadastrar("Camiseta Básica", "Camiseta de algodão, corte reto.", null, camisetas, List.of(
                    camiseta(SKU_CAMISETA_P, "P"), camiseta(SKU_CAMISETA_M, "M"), camiseta(SKU_CAMISETA_G, "G")));
            List.of(SKU_CAMISETA_P, SKU_CAMISETA_M, SKU_CAMISETA_G).forEach(sku -> estoqueInicial(sku, 20));
        }
        produtoSimples(SKU_CANECA, "Caneca de Cerâmica 300 ml", "Caneca de cerâmica de 300 ml.", cozinha, "34.90", 15);
        produtoSimples(SKU_CAFE, "Café Especial 250 g", "Café torrado em grãos, pacote de 250 g.", cozinha, "29.90", 30);
        produtoSimples(SKU_TECLADO, "Teclado Mecânico", "Teclado mecânico com switches lineares.", perifericos,
                "249.90", 1);

        if (!itens.existePorSku(SKU_KIT_CAFE)) {
            gerenciarCombos.cadastrar(SKU_KIT_CAFE, "Kit Café", Dinheiro.de("79.90"),
                    List.of(new ComponenteDeCombo(SKU_CANECA, 1), new ComponenteDeCombo(SKU_CAFE, 2)));
            LOG.info("Combo de demonstração criado: {}", SKU_KIT_CAFE);
        }
    }

    private static NovaVariacao camiseta(String sku, String tamanho) {
        Map<String, String> atributos = new LinkedHashMap<>();
        atributos.put("Tamanho", tamanho);
        atributos.put("Cor", "Azul");
        return new NovaVariacao(sku, Dinheiro.de("59.90"), atributos);
    }

    private void produtoSimples(String sku, String nome, String descricao, Long categoriaId, String preco,
            int estoque) {
        if (itens.existePorSku(sku)) {
            return;
        }
        gerenciarProdutos.cadastrar(nome, descricao, null, categoriaId,
                List.of(new NovaVariacao(sku, Dinheiro.de(preco), Map.of())));
        estoqueInicial(sku, estoque);
    }

    private void estoqueInicial(String sku, int quantidade) {
        Long variacaoId = itens.buscarPorSku(sku).orElseThrow().getId();
        ajustarEstoque.ajustar(variacaoId, quantidade, MOTIVO);
        LOG.info("Estoque de demonstração: {} = {}", sku, quantidade);
    }

    /** Id da categoria com o nome sob o pai dado (raiz se nulo), criando-a se ainda nao existir. */
    private Long categoria(String nome, Long paiId) {
        return categorias.listarTodas().stream()
                .filter(c -> c.getNome().equalsIgnoreCase(nome) && Objects.equals(c.getPaiId(), paiId))
                .map(Categoria::getId).findFirst()
                .orElseGet(() -> gerenciarCategorias.criar(nome, paiId).getId());
    }
}
