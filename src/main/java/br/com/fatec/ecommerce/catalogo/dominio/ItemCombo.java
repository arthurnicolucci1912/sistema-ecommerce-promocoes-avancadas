package br.com.fatec.ecommerce.catalogo.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Componente de um {@link Combo}: um {@link ItemVendavel} (variacao ou outro combo) e a quantidade em que entra.
 *
 * <p>Pattern: Composite - papel: composto (elo entre o combo e o filho que ele agrega).
 *
 * <p>Ver docs/04-design-patterns.md, secao 3.8 (Composite: produto simples vs. combo/kit).
 */
@Entity
@Table(name = "combo_item")
public class ItemCombo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "item_id", nullable = false, updatable = false)
    private ItemVendavel item;

    @Column(nullable = false, updatable = false)
    private int quantidade;

    protected ItemCombo() {
    }

    ItemCombo(ItemVendavel item, int quantidade) {
        this.item = item;
        this.quantidade = quantidade;
    }

    public Long getId() {
        return id;
    }

    public ItemVendavel getItem() {
        return item;
    }

    public int getQuantidade() {
        return quantidade;
    }
}
