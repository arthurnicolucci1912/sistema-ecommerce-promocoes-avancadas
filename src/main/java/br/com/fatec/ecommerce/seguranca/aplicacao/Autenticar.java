package br.com.fatec.ecommerce.seguranca.aplicacao;

import br.com.fatec.ecommerce.compartilhado.dominio.NaoAutenticadoException;
import br.com.fatec.ecommerce.seguranca.dominio.Usuario;
import br.com.fatec.ecommerce.seguranca.dominio.UsuarioRepository;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: autenticar com e-mail e senha e emitir o token de acesso.
 * Qualquer falha resulta na mesma mensagem generica, e a comparacao de senha sempre acontece
 * (contra um hash ficticio quando o usuario nao existe) para nao revelar a existencia do e-mail pelo tempo.
 */
@Service
public class Autenticar {

    static final String MENSAGEM_FALHA = "Credenciais inválidas.";

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final EmissorDeToken emissor;
    private final String hashFicticio;

    public Autenticar(UsuarioRepository usuarios, PasswordEncoder passwordEncoder, EmissorDeToken emissor) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.emissor = emissor;
        this.hashFicticio = passwordEncoder.encode("senha-ficticia-para-igualar-o-tempo");
    }

    @Transactional(readOnly = true)
    public TokenEmitido executar(String email, String senha) {
        Optional<Usuario> usuario = usuarios.buscarPorEmail(email);
        String hash = usuario.map(Usuario::getSenhaHash).orElse(hashFicticio);
        boolean senhaConfere = passwordEncoder.matches(senha, hash);
        if (usuario.isEmpty() || !senhaConfere || !usuario.get().isAtivo()) {
            throw new NaoAutenticadoException(MENSAGEM_FALHA);
        }
        return emissor.emitir(usuario.get());
    }
}
