package br.com.fatec.ecommerce.catalogo.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.DadoInvalidoException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Categoria da arvore do catalogo: nome e categoria pai opcional (raiz quando nao ha pai), com profundidade livre.
 * Sem pattern: e uma arvore simples. As regras que dependem das demais categorias (nome unico entre irmas e
 * ausencia de ciclo) sao verificadas por {@link ArvoreDeCategorias}, que as operacoes abaixo recebem.
 */
@Entity
@Table(name = "categoria")
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @ManyToOne
    @JoinColumn(name = "pai_id")
    private Categoria pai;

    protected Categoria() {
    }

    private Categoria(String nome, Categoria pai) {
        this.nome = nome;
        this.pai = pai;
    }

    /** Cria a categoria; o nome deve ser unico entre as irmas (mesmo pai), sem diferenciar maiusculas. */
    public static Categoria criar(String nome, Categoria pai, ArvoreDeCategorias arvore) {
        String limpo = nomeValido(nome);
        arvore.validarNomeUnico(limpo, idDe(pai), null);
        return new Categoria(limpo, pai);
    }

    public void renomear(String novoNome, ArvoreDeCategorias arvore) {
        String limpo = nomeValido(novoNome);
        arvore.validarNomeUnico(limpo, idDe(pai), id);
        this.nome = limpo;
    }

    /**
     * Move para baixo de {@code novoPai} ({@code null} torna raiz). Rejeita com
     * {@link br.com.fatec.ecommerce.compartilhado.dominio.ConflitoException} mover para si mesma ou para uma
     * descendente, ou repetir o nome de uma irma do novo pai.
     */
    public void mover(Categoria novoPai, ArvoreDeCategorias arvore) {
        arvore.validarMovimento(id, idDe(novoPai));
        arvore.validarNomeUnico(nome, idDe(novoPai), id);
        this.pai = novoPai;
    }

    private static String nomeValido(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new DadoInvalidoException("O nome da categoria é obrigatório.");
        }
        if (nome.trim().length() > 100) {
            throw new DadoInvalidoException("O nome da categoria deve ter no máximo 100 caracteres.");
        }
        return nome.trim();
    }

    private static Long idDe(Categoria categoria) {
        return categoria == null ? null : categoria.getId();
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public Categoria getPai() {
        return pai;
    }

    /** Id do pai, ou {@code null} para uma categoria raiz. */
    public Long getPaiId() {
        return idDe(pai);
    }
}
