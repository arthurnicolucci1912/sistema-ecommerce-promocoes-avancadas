package br.com.fatec.ecommerce.seguranca.infra.persistencia;

import br.com.fatec.ecommerce.compartilhado.dominio.ConflitoException;
import br.com.fatec.ecommerce.seguranca.dominio.Perfil;
import br.com.fatec.ecommerce.seguranca.dominio.Usuario;
import br.com.fatec.ecommerce.seguranca.dominio.UsuarioRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

/** Adaptador JPA da porta {@link UsuarioRepository}. */
@Repository
class UsuarioRepositoryJpa implements UsuarioRepository {

    private final UsuarioJpaRepository jpa;

    UsuarioRepositoryJpa(UsuarioJpaRepository jpa) {
        this.jpa = jpa;
    }

    /**
     * Um cadastro concorrente com o mesmo e-mail passa pela verificacao previa e cai na restricao
     * {@code uk_usuario_email} do banco; isso vira conflito (409). Outras violacoes sao relancadas.
     */
    @Override
    public Usuario salvar(Usuario usuario) {
        try {
            return jpa.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException e) {
            if (violaEmailUnico(e)) {
                throw new ConflitoException("E-mail já cadastrado.");
            }
            throw e;
        }
    }

    private static boolean violaEmailUnico(Throwable erro) {
        for (Throwable causa = erro; causa != null; causa = causa.getCause()) {
            String mensagem = causa.getMessage();
            if (mensagem != null && mensagem.contains("uk_usuario_email")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Optional<Usuario> buscarPorId(Long id) {
        return jpa.findById(id);
    }

    @Override
    public Optional<Usuario> buscarPorEmail(String email) {
        return jpa.findByEmail(Usuario.normalizarEmail(email));
    }

    @Override
    public boolean existePorEmail(String email) {
        return jpa.existsByEmail(Usuario.normalizarEmail(email));
    }

    @Override
    public List<Usuario> listarAdministrativos() {
        return jpa.findAdministrativos(Perfil.CLIENTE);
    }

    /**
     * Primeiro trava as linhas dos ADMIN ativos e so entao conta em uma consulta nova: sob READ COMMITTED,
     * quem esperou o lock enxerga o estado ja confirmado pela transacao concorrente.
     * ({@code FOR UPDATE} nao e permitido junto com {@code count} no PostgreSQL.)
     */
    @Override
    public long contarAdminsAtivosComLock() {
        jpa.findAdminsAtivosParaAtualizacao(Perfil.ADMIN);
        return jpa.contarAdminsAtivos(Perfil.ADMIN);
    }
}
