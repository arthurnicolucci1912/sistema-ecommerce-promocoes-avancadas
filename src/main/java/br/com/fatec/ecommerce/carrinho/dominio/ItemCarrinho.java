package br.com.fatec.ecommerce.carrinho.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.DadoInvalidoException;
import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

/** Linha comercial por SKU; referencias permitem exibir e remover itens que saiam do catalogo. */
@Embeddable
public class ItemCarrinho {
    @Column(nullable = false, length = 100)
    private String sku;
    @Column(name = "item_vendavel_id", nullable = false)
    private Long itemVendavelId;
    @Column(nullable = false)
    private String nome;
    @Column(name = "preco_referencia", nullable = false, precision = 19, scale = 2)
    private BigDecimal precoReferencia;
    @Column(nullable = false)
    private int quantidade;

    protected ItemCarrinho() {
    }

    public ItemCarrinho(String sku, Long itemVendavelId, String nome, Dinheiro preco, int quantidade) {
        this.sku = validarSku(sku);
        this.itemVendavelId = itemVendavelId;
        this.nome = nome;
        this.precoReferencia = preco.valor();
        definirQuantidade(quantidade);
    }

    static String validarSku(String sku) {
        if (sku == null || sku.isBlank() || sku.trim().length() > 100) {
            throw new DadoInvalidoException("O SKU deve ter entre 1 e 100 caracteres.");
        }
        return sku.trim();
    }

    void definirQuantidade(int quantidade) {
        if (quantidade <= 0) {
            throw new DadoInvalidoException("A quantidade deve ser um inteiro positivo.");
        }
        this.quantidade = quantidade;
    }

    void atualizarReferencia(Long itemId, String nome, Dinheiro preco) {
        this.itemVendavelId = itemId;
        this.nome = nome;
        this.precoReferencia = preco.valor();
    }

    public String getSku() { return sku; }
    public Long getItemVendavelId() { return itemVendavelId; }
    public String getNome() { return nome; }
    public Dinheiro getPrecoReferencia() { return Dinheiro.de(precoReferencia); }
    public int getQuantidade() { return quantidade; }
}
