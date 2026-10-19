package br.com.fatec.ecommerce.catalogo.dominio;

import br.com.fatec.ecommerce.catalogo.dominio.evento.VariacaoCadastrada;
import br.com.fatec.ecommerce.compartilhado.dominio.DadoInvalidoException;
import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import br.com.fatec.ecommerce.compartilhado.dominio.EventoDominio;
import br.com.fatec.ecommerce.compartilhado.dominio.RecursoNaoEncontradoException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.hibernate.annotations.BatchSize;

/**
 * Produto do catalogo: nome, descricao, marca opcional, exatamente uma categoria e as suas variacoes. Nao e um item
 * vendavel (quem se vende e a {@link VariacaoProduto}); inativar o produto inativa todas as variacoes.
 *
 * <p>Pattern: Observer - papel: acumula os eventos de dominio ({@link VariacaoCadastrada}) em
 * {@link #consumirEventos()}; quem publica e o servico de aplicacao, depois de salvar.
 *
 * <p>Ver docs/04-design-patterns.md, secao 3.6 (Observer) e 3.8 (Composite, para as variacoes).
 */
@Entity
@Table(name = "produto")
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(columnDefinition = "text")
    private String descricao;

    @Column(length = 100)
    private String marca;

    @ManyToOne(optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @OneToMany(mappedBy = "produto", cascade = CascadeType.ALL)
    @BatchSize(size = 50)
    @OrderBy("id ASC")
    private List<VariacaoProduto> variacoes = new ArrayList<>();

    /** Eventos acumulados, montados so na publicacao (quando as variacoes ja tem id). */
    @Transient
    private List<Supplier<EventoDominio>> eventosPendentes = new ArrayList<>();

    protected Produto() {
    }

    private Produto(String nome, String descricao, String marca, Categoria categoria, Instant criadoEm) {
        this.nome = nome;
        this.descricao = descricao;
        this.marca = marca;
        this.categoria = categoria;
        this.criadoEm = criadoEm;
    }

    public static Produto criar(String nome, String descricao, String marca, Categoria categoria, Instant criadoEm) {
        if (nome == null || nome.isBlank()) {
            throw new DadoInvalidoException("O nome do produto é obrigatório.");
        }
        if (categoria == null) {
            throw new DadoInvalidoException("A categoria do produto é obrigatória.");
        }
        return new Produto(nome.trim(), vazioParaNulo(descricao), vazioParaNulo(marca), categoria, criadoEm);
    }

    /**
     * Cadastra uma variacao. Os atributos sao pares nome/valor livres (um tipo novo de caracteristica nao exige
     * configuracao previa); o nome da variacao e derivado do produto e dos valores dos atributos. Acumula o
     * evento {@link VariacaoCadastrada}.
     */
    public VariacaoProduto adicionarVariacao(String sku, Dinheiro preco, Map<String, String> atributos,
            Instant momento) {
        Map<String, String> limpos = atributosValidos(atributos);
        String nomeDaVariacao = limpos.isEmpty() ? nome : nome + " " + String.join(" / ", limpos.values());
        VariacaoProduto variacao = new VariacaoProduto(this, sku, nomeDaVariacao, preco, limpos);
        variacoes.add(variacao);
        String skuDaVariacao = variacao.getSku();
        // O id da variacao so existe depois de salvar: localiza-se pelo SKU (unico) na hora de montar o evento.
        eventosPendentes.add(() -> {
            VariacaoProduto salva = variacoes.stream()
                    .filter(v -> v.getSku().equals(skuDaVariacao))
                    .findFirst()
                    .orElseThrow();
            return new VariacaoCadastrada(salva.getId(), skuDaVariacao, momento);
        });
        return variacao;
    }

    /** Inativa o produto e todas as suas variacoes (nada e excluido). */
    public void alterarDados(String novoNome, String novaDescricao, String novaMarca, Categoria novaCategoria) {
        if (novoNome == null || novoNome.isBlank()) {
            throw new DadoInvalidoException("O nome do produto é obrigatório.");
        }
        if (novaCategoria == null) {
            throw new DadoInvalidoException("A categoria do produto é obrigatória.");
        }
        this.nome = novoNome.trim();
        this.descricao = vazioParaNulo(novaDescricao);
        this.marca = vazioParaNulo(novaMarca);
        this.categoria = novaCategoria;
    }

    /** A variacao deste produto com o id dado; 404 se o id nao for de uma variacao deste produto. */
    public VariacaoProduto variacaoPorId(Long variacaoId) {
        return variacoes.stream()
                .filter(v -> v.getId() != null && v.getId().equals(variacaoId))
                .findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Variação não encontrada."));
    }

    public void inativar() {
        this.ativo = false;
        variacoes.forEach(VariacaoProduto::inativar);
    }

    /** Entrega os eventos acumulados (e os esquece); o servico de aplicacao os publica depois de salvar. */
    public List<EventoDominio> consumirEventos() {
        List<EventoDominio> eventos = eventosPendentes.stream().map(Supplier::get).toList();
        eventosPendentes.clear();
        return eventos;
    }

    private static Map<String, String> atributosValidos(Map<String, String> atributos) {
        Map<String, String> limpos = new LinkedHashMap<>();
        if (atributos == null) {
            return limpos;
        }
        atributos.forEach((nome, valor) -> {
            if (nome == null || nome.isBlank() || valor == null || valor.isBlank()) {
                throw new DadoInvalidoException("Os atributos da variação precisam de nome e valor.");
            }
            if (limpos.put(nome.trim(), valor.trim()) != null) {
                throw new DadoInvalidoException("Atributo repetido na variação: " + nome.trim() + ".");
            }
        });
        return limpos;
    }

    private static String vazioParaNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getMarca() {
        return marca;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public List<VariacaoProduto> getVariacoes() {
        return Collections.unmodifiableList(variacoes);
    }
}
