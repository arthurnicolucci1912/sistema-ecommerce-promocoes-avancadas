package br.com.fatec.ecommerce.cliente.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Dados cadastrais do cliente. Referencia a conta de acesso apenas pelo id do usuario (modulo seguranca).
 * O CPF e imutavel: somente nome e telefone podem ser alterados.
 */
@Entity
@Table(name = "cliente")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private Long usuarioId;

    @Column(nullable = false)
    private String nome;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, updatable = false, length = 11)
    private String cpf;

    private String telefone;

    protected Cliente() {
    }

    public Cliente(Long usuarioId, String nome, Cpf cpf, String telefone) {
        this.usuarioId = usuarioId;
        this.nome = nome.trim();
        this.cpf = cpf.digitos();
        this.telefone = normalizarTelefone(telefone);
    }

    /** Altera os unicos dados editaveis do cliente. */
    public void alterarDados(String nome, String telefone) {
        this.nome = nome.trim();
        this.telefone = normalizarTelefone(telefone);
    }

    private static String normalizarTelefone(String telefone) {
        return telefone == null || telefone.isBlank() ? null : telefone.trim();
    }

    public Long getId() {
        return id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public String getNome() {
        return nome;
    }

    public Cpf getCpf() {
        return new Cpf(cpf);
    }

    public String getTelefone() {
        return telefone;
    }
}
