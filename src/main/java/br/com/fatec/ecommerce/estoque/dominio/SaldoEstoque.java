package br.com.fatec.ecommerce.estoque.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Saldo de uma variacao: quantidade disponivel e quantidade reservada, nunca negativas (o banco tambem garante).
 * E uma visao de leitura: toda alteracao e feita por atualizacoes condicionais atomicas no banco (RN-21), nunca
 * por leitura-modificacao-gravacao da entidade. A variacao e so uma referencia (sem dependencia do catalogo).
 */
@Entity
@Table(name = "saldo_estoque")
public class SaldoEstoque {

    @Id
    @Column(name = "variacao_id")
    private Long variacaoId;

    @Column(nullable = false)
    private int disponivel;

    @Column(nullable = false)
    private int reservado;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected SaldoEstoque() {
    }

    public Long getVariacaoId() {
        return variacaoId;
    }

    public int getDisponivel() {
        return disponivel;
    }

    public int getReservado() {
        return reservado;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}
