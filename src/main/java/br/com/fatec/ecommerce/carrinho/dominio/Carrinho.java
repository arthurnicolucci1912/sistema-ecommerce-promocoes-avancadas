package br.com.fatec.ecommerce.carrinho.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.DadoInvalidoException;
import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import br.com.fatec.ecommerce.compartilhado.dominio.RecursoNaoEncontradoException;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Agregado por cliente. A revisao garante escrita no pai mesmo quando apenas uma linha muda. */
@Entity
@Table(name = "carrinho")
public class Carrinho {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "cliente_id", nullable = false, unique = true)
    private Long clienteId;
    @Version
    private Long versao;
    @Column(nullable = false)
    private long revisao;
    @Column(name = "cupom_codigo", length = 40)
    private String cupomCodigo;
    @ElementCollection
    @CollectionTable(name = "item_carrinho", joinColumns = @JoinColumn(name = "carrinho_id"))
    @OrderBy("sku ASC")
    private List<ItemCarrinho> itens = new ArrayList<>();

    protected Carrinho() {
    }

    public Carrinho(Long clienteId) {
        if (clienteId == null) {
            throw new DadoInvalidoException("O cliente é obrigatório.");
        }
        this.clienteId = clienteId;
    }

    public void incluir(String sku, Long itemId, String nome, Dinheiro preco, int quantidade) {
        ItemCarrinho novo = new ItemCarrinho(sku, itemId, nome, preco, quantidade);
        ItemCarrinho atual = itens.stream().filter(i -> i.getSku().equals(novo.getSku())).findFirst().orElse(null);
        if (atual != null) {
            try {
                novo.definirQuantidade(Math.addExact(atual.getQuantidade(), quantidade));
            } catch (ArithmeticException erro) {
                throw new DadoInvalidoException("A quantidade excede o limite permitido.");
            }
            itens.remove(atual);
        }
        itens.add(novo);
        revisao++;
    }

    public void alterarQuantidade(String sku, int quantidade) {
        item(sku).definirQuantidade(quantidade);
        revisao++;
    }

    public void remover(String sku) {
        String normalizado = ItemCarrinho.validarSku(sku);
        if (itens.removeIf(i -> i.getSku().equals(normalizado))) {
            revisao++;
        }
    }

    public void atualizarReferencia(String sku, Long itemId, String nome, Dinheiro preco) {
        item(sku).atualizarReferencia(itemId, nome, preco);
        revisao++;
    }

    /** Consome a selecao no checkout, disputando a mesma versao das demais escritas no agregado. */
    public void esvaziar() {
        if (!itens.isEmpty() || cupomCodigo != null) {
            itens.clear();
            cupomCodigo = null;
            revisao++;
        }
    }

    public ItemCarrinho item(String sku) {
        String normalizado = ItemCarrinho.validarSku(sku);
        return itens.stream().filter(i -> i.getSku().equals(normalizado)).findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item não encontrado no carrinho."));
    }

    public Long getId() { return id; }
    public String getCupomCodigo() { return cupomCodigo; }
    public void selecionarCupom(String codigo) {
        if (!java.util.Objects.equals(cupomCodigo, codigo)) { cupomCodigo = codigo; revisao++; }
    }
    public Long getClienteId() { return clienteId; }
    public Long getVersao() { return versao; }
    public List<ItemCarrinho> getItens() { return Collections.unmodifiableList(itens); }
}
