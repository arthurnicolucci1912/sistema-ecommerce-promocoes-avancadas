package br.com.fatec.ecommerce.cliente.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.DadoInvalidoException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Endereco de entrega de um cliente. A remocao e logica ({@link #inativar()}). Quem decide qual endereco
 * e o principal e {@link RegraEnderecoPrincipal}.
 */
@Entity
@Table(name = "endereco")
public class Endereco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cliente_id", nullable = false, updatable = false)
    private Long clienteId;

    private String apelido;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 8)
    private String cep;

    @Column(nullable = false)
    private String logradouro;

    @Column(nullable = false)
    private String numero;

    private String complemento;

    @Column(nullable = false)
    private String bairro;

    @Column(nullable = false)
    private String cidade;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 2)
    private String uf;

    @Column(nullable = false)
    private boolean principal;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected Endereco() {
    }

    public Endereco(Long clienteId, String apelido, String cep, String logradouro, String numero,
            String complemento, String bairro, String cidade, String uf) {
        this.clienteId = clienteId;
        alterarDados(apelido, cep, logradouro, numero, complemento, bairro, cidade, uf);
    }

    @PrePersist
    void aoCriar() {
        criadoEm = Instant.now();
    }

    /** Substitui os dados de localizacao (nao altera principal nem situacao). */
    public void alterarDados(String apelido, String cep, String logradouro, String numero, String complemento,
            String bairro, String cidade, String uf) {
        this.cep = normalizarCep(cep);
        this.uf = Uf.da(uf).orElseThrow(() -> new DadoInvalidoException("UF inválida.")).name();
        this.apelido = apelido == null ? null : apelido.trim();
        this.logradouro = logradouro.trim();
        this.numero = numero.trim();
        this.complemento = complemento == null || complemento.isBlank() ? null : complemento.trim();
        this.bairro = bairro.trim();
        this.cidade = cidade.trim();
    }

    /** Aceita CEP com ou sem hifen e devolve os 8 digitos; lanca {@link DadoInvalidoException} se invalido. */
    public static String normalizarCep(String cep) {
        String digitos = cep == null ? "" : cep.trim().replace("-", "");
        if (!digitos.matches("\\d{8}")) {
            throw new DadoInvalidoException("CEP inválido: informe 8 dígitos.");
        }
        return digitos;
    }

    public void marcarPrincipal() {
        this.principal = true;
    }

    public void desmarcarPrincipal() {
        this.principal = false;
    }

    /** Remocao logica; um endereco inativo nunca e principal. */
    public void inativar() {
        this.ativo = false;
        this.principal = false;
    }

    public Long getId() {
        return id;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public String getApelido() {
        return apelido;
    }

    public String getCep() {
        return cep;
    }

    public String getLogradouro() {
        return logradouro;
    }

    public String getNumero() {
        return numero;
    }

    public String getComplemento() {
        return complemento;
    }

    public String getBairro() {
        return bairro;
    }

    public String getCidade() {
        return cidade;
    }

    public String getUf() {
        return uf;
    }

    public boolean isPrincipal() {
        return principal;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
