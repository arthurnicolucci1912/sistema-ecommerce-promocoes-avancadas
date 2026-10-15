package br.com.fatec.ecommerce.estoque.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Reserva de uma variacao para um pedido (unica por pedido e variacao). O pedido e a variacao sao so referencias,
 * sem chave estrangeira. A situacao muda apenas por troca condicional no banco, o que torna as operacoes
 * idempotentes (ver {@link SituacaoReserva}).
 */
@Entity
@Table(name = "reserva_estoque")
public class ReservaEstoque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pedido_id", nullable = false)
    private Long pedidoId;

    @Column(name = "variacao_id", nullable = false)
    private Long variacaoId;

    @Column(nullable = false)
    private int quantidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SituacaoReserva situacao;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected ReservaEstoque() {
    }

    private ReservaEstoque(Long pedidoId, Long variacaoId, int quantidade, Instant momento) {
        this.pedidoId = pedidoId;
        this.variacaoId = variacaoId;
        this.quantidade = quantidade;
        this.situacao = SituacaoReserva.ATIVA;
        this.criadoEm = momento;
        this.atualizadoEm = momento;
    }

    /** Nova reserva, ja ativa. */
    public static ReservaEstoque ativa(Long pedidoId, ComponenteAReservar componente, Instant momento) {
        return new ReservaEstoque(pedidoId, componente.variacaoId(), componente.quantidade(), momento);
    }

    public Long getId() {
        return id;
    }

    public Long getPedidoId() {
        return pedidoId;
    }

    public Long getVariacaoId() {
        return variacaoId;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public SituacaoReserva getSituacao() {
        return situacao;
    }
}
