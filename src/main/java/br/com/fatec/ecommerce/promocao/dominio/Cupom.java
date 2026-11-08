package br.com.fatec.ecommerce.promocao.dominio;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.Map;
/** Cupom configurado; disponibilidade e consumo sao protegidos pelo adaptador transacional. */
@Entity @Table(name="cupom")
public class Cupom {
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
    @Column(nullable=false,updatable=false,unique=true,length=40) private String codigo;
    @Column(nullable=false) private boolean acumulavel;
    @Column(name="limite_total",nullable=false) private int limiteTotal;
    @Column(name="limite_cliente") private Integer limiteCliente;
    protected Cupom() { }
    public Cupom(String codigo, String nome, String tipo, Instant inicio, Instant fim, boolean acumulavel,
            int limiteTotal, Integer limiteCliente, Map<String,Object> parametros, CondicaoConfig condicao, Instant criadoEm) {
        this.codigo=normalizar(codigo); alterar(nome,tipo,inicio,fim,acumulavel,limiteTotal,limiteCliente,parametros,condicao); this.criadoEm=criadoEm;
    }
    public static String normalizar(String valor) {
        if(valor==null) { throw Parametros.invalido("codigo"); }
        String aparado=valor.trim();
        // valida antes de maiusculizar: 'ß' -> 'SS' ou 'ﬁ' -> 'FI' nao podem virar ASCII valido
        if(!aparado.matches("[A-Za-z0-9_-]{3,40}")) { throw Parametros.invalido("codigo"); }
        return aparado.toUpperCase(java.util.Locale.ROOT);
    }
    public void alterar(String nome, String tipo, Instant inicio, Instant fim, boolean acumulavel, int limiteTotal,
            Integer limiteCliente, Map<String,Object> parametros, CondicaoConfig condicao) {
        if(limiteTotal<=0 || (limiteCliente!=null && limiteCliente<=0)) { throw Parametros.invalido("limites"); }
        dados(nome,tipo,inicio,fim,parametros,condicao); this.acumulavel=acumulavel;
        this.limiteTotal=limiteTotal; this.limiteCliente=limiteCliente;
    }
    public String codigo() { return codigo; }
    public boolean acumulavel() { return acumulavel; }
    public int limiteTotal() { return limiteTotal; }
    public Integer limiteCliente() { return limiteCliente; }
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
