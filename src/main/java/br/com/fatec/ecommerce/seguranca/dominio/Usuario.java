package br.com.fatec.ecommerce.seguranca.dominio;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/** Identidade e credenciais de quem acessa o sistema. O e-mail e guardado em minusculas e sem espacos nas pontas. */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    @Column(nullable = false)
    private boolean ativo = true;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "usuario_perfil", joinColumns = @JoinColumn(name = "usuario_id"))
    @Column(name = "perfil", nullable = false)
    @Enumerated(EnumType.STRING)
    private Set<Perfil> perfis = EnumSet.noneOf(Perfil.class);

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected Usuario() {
    }

    public Usuario(String email, String senhaHash, Collection<Perfil> perfis) {
        this.email = normalizarEmail(email);
        this.senhaHash = senhaHash;
        this.perfis = copiar(perfis);
    }

    /** Remove espacos das pontas e converte para minusculas. */
    public static String normalizarEmail(String email) {
        if (email == null) {
            throw new IllegalArgumentException("e-mail é obrigatório");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static Set<Perfil> copiar(Collection<Perfil> perfis) {
        Set<Perfil> copia = EnumSet.noneOf(Perfil.class);
        copia.addAll(perfis);
        return copia;
    }

    @PrePersist
    void aoCriar() {
        Instant agora = Instant.now();
        criadoEm = agora;
        atualizadoEm = agora;
    }

    @PreUpdate
    void aoAtualizar() {
        atualizadoEm = Instant.now();
    }

    /** Verdadeiro se o usuario esta ativo e possui o perfil {@link Perfil#ADMIN}. */
    public boolean ehAdminAtivo() {
        return ativo && perfis.contains(Perfil.ADMIN);
    }

    /** Verdadeiro se possui ao menos um perfil administrativo. */
    public boolean ehAdministrativo() {
        return perfis.stream().anyMatch(Perfil::ehAdministrativo);
    }

    public void substituirPerfis(Collection<Perfil> novosPerfis) {
        this.perfis = copiar(novosPerfis);
    }

    public void inativar() {
        this.ativo = false;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public Set<Perfil> getPerfis() {
        return Set.copyOf(perfis);
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}
