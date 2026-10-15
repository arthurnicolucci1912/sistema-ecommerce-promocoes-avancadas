package br.com.fatec.ecommerce.pedido.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/** Item do snapshot do pedido: copia de identificacao, nome, variacao, quantidade e preco no momento da compra. */
@Entity
@Table(name = "pedido_item")
public class ItemPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private String sku;

    @Column(name = "nome_produto", nullable = false, updatable = false)
    private String nomeProduto;

    @Column(name = "descricao_variacao", updatable = false)
    private String descricaoVariacao;

    @Column(nullable = false, updatable = false)
    private int quantidade;

    @Column(name = "preco_unitario", nullable = false, updatable = false, precision = 12, scale = 2)
    private BigDecimal precoUnitario;

    @Column(name = "total_item", nullable = false, updatable = false, precision = 12, scale = 2)
    private BigDecimal totalItem;

    protected ItemPedido() {
    }

    ItemPedido(DadosNovoPedido.Item item) {
        this.sku = item.sku().trim();
        this.nomeProduto = item.nomeProduto().trim();
        this.descricaoVariacao = item.descricaoVariacao() == null || item.descricaoVariacao().isBlank()
                ? null : item.descricaoVariacao().trim();
        this.quantidade = item.quantidade();
        this.precoUnitario = item.precoUnitario().valor();
        this.totalItem = item.precoUnitario().multiplicar(item.quantidade()).valor();
    }

    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getNomeProduto() {
        return nomeProduto;
    }

    public String getDescricaoVariacao() {
        return descricaoVariacao;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public Dinheiro getPrecoUnitario() {
        return Dinheiro.de(precoUnitario);
    }

    public Dinheiro getTotalItem() {
        return Dinheiro.de(totalItem);
    }
}
