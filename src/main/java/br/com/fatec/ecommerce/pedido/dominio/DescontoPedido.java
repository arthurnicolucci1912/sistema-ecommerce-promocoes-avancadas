package br.com.fatec.ecommerce.pedido.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/** Desconto do snapshot do pedido, com nome, origem, alvo e valor abatido no momento da compra. */
@Entity
@Table(name = "pedido_desconto")
public class DescontoPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private OrigemDesconto origem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private AlvoDoDesconto alvo;

    @Column(nullable = false, updatable = false, precision = 12, scale = 2)
    private BigDecimal valor;
    @Column(name="referencia_id", updatable=false) private Long referenciaId;
    @Column(updatable=false,length=40) private String codigo;
    public Long getReferenciaId() { return referenciaId; }
    public String getCodigo() { return codigo; }

    protected DescontoPedido() {
    }

    DescontoPedido(DadosNovoPedido.Desconto desconto) {
        this.descricao = desconto.descricao().trim();
        this.origem = desconto.origem();
        this.alvo = desconto.alvo();
        this.valor = desconto.valor().valor();
        this.referenciaId = desconto.referenciaId(); this.codigo = desconto.codigo();
    }

    public Long getId() {
        return id;
    }

    public String getDescricao() {
        return descricao;
    }

    public OrigemDesconto getOrigem() {
        return origem;
    }

    public AlvoDoDesconto getAlvo() {
        return alvo;
    }

    public Dinheiro getValor() {
        return Dinheiro.de(valor);
    }
}
