package br.com.fatec.ecommerce.promocao.dominio;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.Map;
/** Configuracao persistente; Factory Method materializa a estrategia por tipo, sem heranca JPA de beneficios. */
@Entity @Table(name="promocao_config")
public class PromocaoConfig {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Version private Long versao;
    @Column(nullable=false) private String nome;
    @Column(nullable=false) private String tipo;
    @Column(nullable=false) private boolean ativo = true;
    @Column(nullable=false) private Instant inicio;
    @Column(nullable=false) private Instant fim;
    @Column(name="criado_em",nullable=false,updatable=false) private Instant criadoEm;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition="jsonb",nullable=false) private Map<String,Object> parametros;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition="jsonb") private CondicaoConfig condicao;
    @Column(nullable=false) private int prioridade;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private ModoAcumulacao modo;
    protected PromocaoConfig() { }
    public PromocaoConfig(String nome, String tipo, Instant inicio, Instant fim, int prioridade, ModoAcumulacao modo,
            Map<String,Object> parametros, CondicaoConfig condicao, Instant criadoEm) {
        alterar(nome,tipo,inicio,fim,prioridade,modo,parametros,condicao); this.criadoEm=criadoEm;
    }
    public void alterar(String nome, String tipo, Instant inicio, Instant fim, int prioridade, ModoAcumulacao modo,
            Map<String,Object> parametros, CondicaoConfig condicao) {
        if (modo==null) { throw Parametros.invalido("modo"); }
        dados(nome,tipo,inicio,fim,parametros,condicao); this.prioridade=prioridade; this.modo=modo;
    }
    public int prioridade() { return prioridade; }
    public ModoAcumulacao modo() { return modo; }
    public Long id() { return id; }
    public Long versao() { return versao; }
    public String nome() { return nome; }
    public String tipo() { return tipo; }
    public boolean ativo() { return ativo; }
    public Instant inicio() { return inicio; }
    public Instant fim() { return fim; }
    public Instant criadoEm() { return criadoEm; }
    public Map<String,Object> parametros() { return java.util.Collections.unmodifiableMap(parametros); }
    public CondicaoConfig condicao() { return condicao; }
    public void definirAtivo(boolean valor) { ativo = valor; }
    public boolean vigenteEm(Instant momento) { return ativo && !momento.isBefore(inicio) && momento.isBefore(fim); }
    private void dados(String nome, String tipo, Instant inicio, Instant fim, Map<String,Object> parametros, CondicaoConfig condicao) {
        if (nome == null || nome.isBlank() || nome.trim().length()>255 || tipo == null || tipo.isBlank()
                || inicio == null || fim == null || !inicio.isBefore(fim)) { throw Parametros.invalido("campanha/cupom"); }
        this.nome=nome.trim(); this.tipo=tipo; this.inicio=inicio; this.fim=fim;
        this.parametros=Parametros.copiaSemNulos(parametros); this.condicao=condicao;
    }
}
