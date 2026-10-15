package br.com.fatec.ecommerce.seguranca.dominio;

import java.util.List;
import java.util.Optional;

/** Porta de persistencia de usuarios (implementada em {@code infra.persistencia}). */
public interface UsuarioRepository {

    Usuario salvar(Usuario usuario);

    Optional<Usuario> buscarPorId(Long id);

    /** Busca pelo e-mail, normalizando-o (minusculas, sem espacos nas pontas). */
    Optional<Usuario> buscarPorEmail(String email);

    boolean existePorEmail(String email);

    /** Usuarios com ao menos um perfil administrativo, ordenados por id. */
    List<Usuario> listarAdministrativos();

    /**
     * Conta os {@link Perfil#ADMIN} ativos travando as linhas ({@code SELECT ... FOR UPDATE}) ate o fim
     * da transacao corrente, para serializar operacoes concorrentes sobre o ultimo ADMIN.
     */
    long contarAdminsAtivosComLock();
}
