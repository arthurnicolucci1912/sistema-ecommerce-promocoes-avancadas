package br.com.fatec.ecommerce.auditoria.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.OperacaoAdministrativa;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Trilha sem métodos de alteração; dados copiados do evento validado. */
@Entity @Table(name = "auditoria_operacao")
public class RegistroAuditoria {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable=false, updatable=false) private Long usuarioId;
    @Column(nullable=false, updatable=false) private Instant ocorridoEm;
    @Column(nullable=false, updatable=false, length=100) private String operacao;
    @Column(nullable=false, updatable=false, length=100) private String recursoTipo;
    @Column(nullable=false, updatable=false) private Long recursoId;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable=false, updatable=false, columnDefinition="jsonb") private Map<String,Object> detalhes;
    protected RegistroAuditoria() {}
    public RegistroAuditoria(OperacaoAdministrativa evento) {
        usuarioId=evento.usuarioId(); ocorridoEm=evento.ocorridoEm(); operacao=evento.operacao();
        recursoTipo=evento.recursoTipo(); recursoId=evento.recursoId(); detalhes=evento.detalhes();
    }
    public Long getId() { return id; }
    public Long getUsuarioId() { return usuarioId; }
    public Instant getOcorridoEm() { return ocorridoEm; }
    public String getOperacao() { return operacao; }
    public String getRecursoTipo() { return recursoTipo; }
    public Long getRecursoId() { return recursoId; }
    public Map<String,Object> getDetalhes() {
        return new OperacaoAdministrativa(usuarioId, ocorridoEm, operacao, recursoTipo, recursoId, detalhes).detalhes();
    }
}
