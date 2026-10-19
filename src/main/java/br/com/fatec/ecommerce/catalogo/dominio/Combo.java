package br.com.fatec.ecommerce.catalogo.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.ConflitoException;
import br.com.fatec.ecommerce.compartilhado.dominio.DadoInvalidoException;
import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.hibernate.annotations.BatchSize;

/**
 * Conjunto vendido com SKU e preco proprios, composto por itens vendaveis (variacoes ou outros combos) com
 * quantidade. Um combo nunca contem a si mesmo, direta ou indiretamente.
 *
 * <p>Pattern: Composite - papel: composto. Delega aos filhos pelo contrato de {@link ItemVendavel}:
 * {@link #componentesDeEstoque(int)} soma recursivamente {@code item.componentesDeEstoque(q x quantidade)} e
 * agrega variacoes repetidas; {@link #estaDisponivelParaVenda()} exige o combo ativo e todos os componentes
 * disponiveis. Nenhum codigo aqui distingue o tipo do filho.
 *
 * <p>Ver docs/04-design-patterns.md, secao 3.8 (Composite: produto simples vs. combo/kit).
 */
@Entity
@Table(name = "combo")
@DiscriminatorValue("COMBO")
public class Combo extends ItemVendavel {

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "combo_id", nullable = false, updatable = false)
    @BatchSize(size = 50)
    @OrderBy("id ASC")
    private List<ItemCombo> itens = new ArrayList<>();

    protected Combo() {
    }

    /** O combo nasce sem componentes; a aplicacao exige ao menos um ao cadastrar (um combo vazio nao e vendavel). */
    public Combo(String sku, String nome, Dinheiro preco) {
        super(sku, nome, preco);
    }

    /**
     * Inclui um componente. Rejeita quantidade nao positiva ({@link DadoInvalidoException}), componente repetido
     * e combo circular, isto e, componente que e este combo ou que o contem ({@link ConflitoException}).
     */
    public void adicionarItem(ItemVendavel item, int quantidade) {
        if (item == null) {
            throw new DadoInvalidoException("O componente do combo é obrigatório.");
        }
        exigirQuantidadePositiva(quantidade);
        if (item.contemOuE(this)) {
            throw new ConflitoException("Um combo não pode conter a si mesmo, direta ou indiretamente.");
        }
        if (itens.stream().anyMatch(existente -> existente.getItem().mesmoItemQue(item))) {
            throw new ConflitoException("O item já faz parte do combo.");
        }
        itens.add(new ItemCombo(item, quantidade));
    }

    /** Combo vendavel: ativo, com componentes, e todos eles disponiveis (recursivamente). */
    /** Remove todos os componentes, para substitui-los por uma nova lista (o chamador grava antes de adicionar). */
    public void removerItens() {
        itens.clear();
    }

    @Override
    public boolean estaDisponivelParaVenda() {
        return isAtivo() && !itens.isEmpty()
                && itens.stream().allMatch(componente -> componente.getItem().estaDisponivelParaVenda());
    }

    @Override
    public List<ComponenteEstoque> componentesDeEstoque(int quantidade) {
        exigirQuantidadePositiva(quantidade);
        Map<Long, Integer> agregado = new LinkedHashMap<>();
        for (ItemCombo componente : itens) {
            int necessaria = Math.multiplyExact(quantidade, componente.getQuantidade());
            for (ComponenteEstoque parcela : componente.getItem().componentesDeEstoque(necessaria)) {
                agregado.merge(parcela.variacaoId(), parcela.quantidade(), Integer::sum);
            }
        }
        return agregado.entrySet().stream()
                .map(entrada -> new ComponenteEstoque(entrada.getKey(), entrada.getValue()))
                .toList();
    }

    @Override
    public boolean contemOuE(ItemVendavel alvo) {
        return super.contemOuE(alvo) || itens.stream().anyMatch(componente -> componente.getItem().contemOuE(alvo));
    }

    public List<ItemCombo> getItens() {
        return Collections.unmodifiableList(itens);
    }
}
