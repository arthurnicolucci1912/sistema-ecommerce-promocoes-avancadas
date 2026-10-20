package br.com.fatec.ecommerce.catalogo.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.DadoInvalidoException;
import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * Item que o catalogo vende: uma variacao de produto ou um combo, tratados de forma uniforme por quem consome
 * (carrinho, checkout, promocoes).
 *
 * <p>Pattern: Composite - papel: componente. Define o contrato comum ({@link #preco()},
 * {@link #estaDisponivelParaVenda()}, {@link #componentesDeEstoque(int)}, {@link #contemOuE(ItemVendavel)}); a
 * {@link VariacaoProduto} e a folha e o {@link Combo} e o composto. Um novo tipo de item vendavel e uma nova
 * subclasse; os clientes do contrato (inclusive o proprio combo) nao usam {@code instanceof} nem {@code switch}
 * por tipo e nao mudam.
 *
 * <p>Mapeamento: heranca JPA {@code JOINED} sobre {@code item_vendavel}, com o discriminador {@code tipo}
 * ({@code VARIACAO} ou {@code COMBO}). Um tipo novo persistido precisa de valor de discriminador e tabela propria
 * (migracao); o dominio em si nao muda.
 *
 * <p>As categorias ancestrais do item nao fazem parte do componente: dependem da arvore inteira de categorias e
 * sao calculadas pelo servico de aplicacao com {@link ArvoreDeCategorias}.
 *
 * <p>Ver docs/04-design-patterns.md, secao 3.8 (Composite: produto simples vs. combo/kit).
 */
@Entity
@Table(name = "item_vendavel")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "tipo", discriminatorType = DiscriminatorType.STRING, length = 20)
public abstract class ItemVendavel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false, length = 60)
    private String sku;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal preco;

    @Column(nullable = false)
    private boolean ativo = true;

    protected ItemVendavel() {
    }

    protected ItemVendavel(String sku, String nome, Dinheiro preco) {
        if (sku == null || sku.isBlank()) {
            throw new DadoInvalidoException("O SKU é obrigatório.");
        }
        if (nome == null || nome.isBlank()) {
            throw new DadoInvalidoException("O nome é obrigatório.");
        }
        this.sku = sku.trim();
        this.nome = nome.trim();
        this.preco = validarPreco(preco).valor();
    }

    private static Dinheiro validarPreco(Dinheiro preco) {
        if (preco == null || !preco.maiorQue(Dinheiro.zero())) {
            throw new DadoInvalidoException("O preço deve ser maior que zero.");
        }
        return preco;
    }

    /** Preco do item (proprio, nao a soma dos componentes). */
    public Dinheiro preco() {
        return Dinheiro.de(preco);
    }

    /** Se o item pode ser vendido agora: ativo e com todos os seus componentes disponiveis (estoque a parte). */
    public abstract boolean estaDisponivelParaVenda();

    /**
     * Variacoes e quantidades de estoque que a venda de {@code quantidade} unidades deste item consome, sem
     * variacoes repetidas (as repeticoes sao somadas).
     */
    public abstract List<ComponenteEstoque> componentesDeEstoque(int quantidade);

    /**
     * Ids das categorias a que o item pertence diretamente (sem os ancestrais, que a arvore de categorias calcula).
     * Por padrao e vazio: o item nao pertence a categoria nenhuma e quem o consome nao precisa tratar isso. A
     * variacao devolve a categoria do produto; o combo nao tem categoria propria (nao herda a dos componentes), pois
     * tem preco proprio e e enderecado pelo SKU.
     */
    public Set<Long> categoriaIdsDiretas() {
        return Set.of();
    }

    /**
     * Se este item e {@code alvo} ou o contem, direta ou indiretamente. A folha so "contem" a si mesma; o composto
     * acrescenta os seus componentes. Serve a validacao de combo circular sem inspecionar o tipo dos filhos.
     */
    public boolean contemOuE(ItemVendavel alvo) {
        return mesmoItemQue(alvo);
    }

    /** Identidade do item: a mesma instancia ou o mesmo id persistido. */
    public final boolean mesmoItemQue(ItemVendavel outro) {
        return this == outro || (getId() != null && getId().equals(outro.getId()));
    }

    /** O preco e sempre maior que zero; fora disso lanca {@link DadoInvalidoException}. */
    public void alterarPreco(Dinheiro novoPreco) {
        this.preco = validarPreco(novoPreco).valor();
    }

    public void inativar() {
        this.ativo = false;
    }

    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getNome() {
        return nome;
    }

    public boolean isAtivo() {
        return ativo;
    }

    protected static void exigirQuantidadePositiva(int quantidade) {
        if (quantidade <= 0) {
            throw new DadoInvalidoException("A quantidade deve ser maior que zero.");
        }
    }
}
