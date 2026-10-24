package br.com.fatec.ecommerce.seguranca.aplicacao;

import br.com.fatec.ecommerce.compartilhado.aplicacao.PublicarOperacaoAdministrativa;
import br.com.fatec.ecommerce.compartilhado.dominio.ConflitoException;
import br.com.fatec.ecommerce.compartilhado.dominio.DadoInvalidoException;
import br.com.fatec.ecommerce.compartilhado.dominio.RecursoNaoEncontradoException;
import br.com.fatec.ecommerce.seguranca.dominio.Perfil;
import br.com.fatec.ecommerce.seguranca.dominio.RegraUltimoAdmin;
import br.com.fatec.ecommerce.seguranca.dominio.Usuario;
import br.com.fatec.ecommerce.seguranca.dominio.UsuarioRepository;
import java.util.Collection;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>Observer administrativo: publica snapshots permitidos na transação original; falha da trilha reverte a operação.
 *
 * Caso de uso: gestao de usuarios administrativos (criar, listar, alterar perfis, inativar).
 * Restrito a ADMIN. As operacoes que podem remover um ADMIN contam os ADMIN ativos sob lock
 * na mesma transacao ({@link RegraUltimoAdmin}).
 */
@Service
@PreAuthorize("hasRole('ADMIN')")
@Transactional
public class GerenciarUsuariosAdministrativos {

    private final PublicarOperacaoAdministrativa auditoria;
    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;

    public GerenciarUsuariosAdministrativos(UsuarioRepository usuarios, PasswordEncoder passwordEncoder, PublicarOperacaoAdministrativa auditoria) {
        this.auditoria = auditoria;
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
    }

    public Usuario criar(String email, String senha, Collection<Perfil> perfis) {
        validarPerfisAdministrativos(perfis);
        if (usuarios.existePorEmail(email)) {
            throw new ConflitoException("E-mail já cadastrado.");
        }
        Usuario salvo = usuarios.salvar(new Usuario(email, passwordEncoder.encode(senha), perfis));
        auditoria.publicar("USUARIO_CRIADO", "USUARIO", salvo.getId(), null, dados(salvo));
        return salvo;
    }

    @Transactional(readOnly = true)
    public List<Usuario> listar() {
        return usuarios.listarAdministrativos();
    }

    public Usuario alterarPerfis(Long id, Collection<Perfil> novosPerfis) {
        validarPerfisAdministrativos(novosPerfis);
        long admins = usuarios.contarAdminsAtivosComLock();
        Usuario alvo = buscarAdministrativo(id);
        var antes = dados(alvo);
        RegraUltimoAdmin.garantirAdminRemanescente(alvo, novosPerfis, alvo.isAtivo(), admins);
        alvo.substituirPerfis(novosPerfis);
        Usuario salvo = usuarios.salvar(alvo);
        auditoria.publicar("USUARIO_PERFIS_ALTERADOS", "USUARIO", id, antes, dados(salvo));
        return salvo;
    }

    public void inativar(Long id) {
        long admins = usuarios.contarAdminsAtivosComLock();
        Usuario alvo = buscarAdministrativo(id);
        var antes = dados(alvo);
        RegraUltimoAdmin.garantirAdminRemanescente(alvo, alvo.getPerfis(), false, admins);
        alvo.inativar();
        usuarios.salvar(alvo);
        auditoria.publicar("USUARIO_INATIVADO", "USUARIO", id, antes, dados(alvo));
    }

    private java.util.Map<String,Object> dados(Usuario u) {
        return java.util.Map.of("perfis", u.getPerfis().stream().map(Enum::name).sorted().toList(), "ativo", u.isAtivo());
    }
    private Usuario buscarAdministrativo(Long id) {
        return usuarios.buscarPorId(id).filter(Usuario::ehAdministrativo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário administrativo não encontrado."));
    }

    private static void validarPerfisAdministrativos(Collection<Perfil> perfis) {
        if (perfis == null || perfis.isEmpty()) {
            throw new DadoInvalidoException("Informe ao menos um perfil administrativo.");
        }
        if (perfis.stream().anyMatch(p -> p == null || !p.ehAdministrativo())) {
            throw new DadoInvalidoException(
                    "Somente perfis administrativos podem ser atribuídos por esta operação (CLIENTE não é permitido).");
        }
    }
}
