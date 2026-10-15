package br.com.fatec.ecommerce.pedido.dominio;

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
 * Registro de uma transicao do pedido: de onde veio, para onde foi, quando, quem fez e por que. A entrada de
 * criacao do pedido tem {@code de} nulo. Imutavel depois de criada.
 */
@Entity
@Table(name = "pedido_historico")
public class EntradaHistorico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(updatable = false)
    private SituacaoPedido de;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private SituacaoPedido para;

    @Column(name = "ocorrido_em", nullable = false, updatable = false)
    private Instant ocorridoEm;

    @Enumerated(EnumType.STRING)
    @Column(name = "responsavel_tipo", nullable = false, updatable = false)
    private TipoResponsavel responsavelTipo;

    @Column(name = "responsavel_usuario_id", updatable = false)
    private Long responsavelUsuarioId;

    @Column(updatable = false)
    private String motivo;

    protected EntradaHistorico() {
    }

    EntradaHistorico(SituacaoPedido de, SituacaoPedido para, Instant ocorridoEm, Responsavel responsavel,
            String motivo) {
        this.de = de;
        this.para = para;
        this.ocorridoEm = ocorridoEm;
        this.responsavelTipo = responsavel.tipo();
        this.responsavelUsuarioId = responsavel.usuarioId();
        this.motivo = motivo == null || motivo.isBlank() ? null : motivo.trim();
    }

    public Long getId() {
        return id;
    }

    /** Situacao de origem; nula na entrada de criacao do pedido. */
    public SituacaoPedido getDe() {
        return de;
    }

    public SituacaoPedido getPara() {
        return para;
    }

    public Instant getOcorridoEm() {
        return ocorridoEm;
    }

    public Responsavel getResponsavel() {
        return new Responsavel(responsavelTipo, responsavelUsuarioId);
    }

    public String getMotivo() {
        return motivo;
    }
}
