package br.com.fatec.ecommerce.catalogo.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hibernate.annotations.BatchSize;

/**
 * Variacao de um {@link Produto} (por exemplo, camiseta M azul): tem SKU, preco, atributos nome/valor e situacao.
 *
 * <p>Pattern: Composite - papel: folha. E a unidade que o estoque controla: {@link #componentesDeEstoque(int)}
 * devolve ela mesma com a quantidade pedida.
 *
 * <p>Ver docs/04-design-patterns.md, secao 3.8 (Composite: produto simples vs. combo/kit).
 */
@Entity
@Table(name = "variacao_produto")
@DiscriminatorValue("VARIACAO")
public class VariacaoProduto extends ItemVendavel {

    @ManyToOne(optional = false)
    @JoinColumn(name = "produto_id", nullable = false, updatable = false)
    private Produto produto;

    @ElementCollection(fetch = FetchType.EAGER)
    @BatchSize(size = 50)
    @CollectionTable(name = "variacao_atributo", joinColumns = @JoinColumn(name = "variacao_id"))
    @MapKeyColumn(name = "nome", length = 100)
    @Column(name = "valor", nullable = false)
    private Map<String, String> atributos = new LinkedHashMap<>();

    protected VariacaoProduto() {
    }

    /** Criada por {@link Produto#adicionarVariacao}, que valida os atributos e deriva o nome. */
    VariacaoProduto(Produto produto, String sku, String nome, Dinheiro preco, Map<String, String> atributos) {
        super(sku, nome, preco);
        this.produto = produto;
        this.atributos.putAll(atributos);
    }

    /** Disponivel quando a propria variacao e o produto estao ativos. */
    @Override
    public boolean estaDisponivelParaVenda() {
        return isAtivo() && produto.isAtivo();
    }

    @Override
    public List<ComponenteEstoque> componentesDeEstoque(int quantidade) {
        exigirQuantidadePositiva(quantidade);
        return List.of(new ComponenteEstoque(getId(), quantidade));
    }

    @Override
    public Set<Long> categoriaIdsDiretas() {
        return Set.of(produto.getCategoria().getId());
    }

    public Produto getProduto() {
        return produto;
    }

    public Map<String, String> getAtributos() {
        return Collections.unmodifiableMap(atributos);
    }

    @Override
    public String descricaoVariacao() {
        if (atributos.isEmpty()) { return null; }
        return atributos.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(java.util.stream.Collectors.joining(", "));
    }
}
