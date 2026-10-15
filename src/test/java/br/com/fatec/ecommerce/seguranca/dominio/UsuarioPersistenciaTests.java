package br.com.fatec.ecommerce.seguranca.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fatec.ecommerce.TesteDeIntegracao;
import br.com.fatec.ecommerce.compartilhado.dominio.ConflitoException;
import br.com.fatec.ecommerce.seguranca.UsuariosParaTeste;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

@TesteDeIntegracao
class UsuarioPersistenciaTests {

    @Autowired
    UsuarioRepository usuarios;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    UsuariosParaTeste helper;

    @BeforeEach
    void limpar() {
        helper.limpar();
    }

    @Test
    void emailEhSalvoEmMinusculasESemEspacos() {
        usuarios.salvar(new Usuario("  Maria.Silva@Exemplo.COM ", "hash", List.of(Perfil.CLIENTE)));

        String gravado = jdbc.queryForObject("SELECT email FROM usuario", String.class);
        assertThat(gravado).isEqualTo("maria.silva@exemplo.com");
        assertThat(usuarios.buscarPorEmail("MARIA.SILVA@exemplo.com")).isPresent();
        assertThat(usuarios.existePorEmail(" Maria.Silva@Exemplo.com")).isTrue();
    }

    @Test
    void perfisEDatasSaoPersistidos() {
        Usuario salvo = usuarios.salvar(new Usuario("a@a.com", "hash", List.of(Perfil.ADMIN, Perfil.ESTOQUE)));

        Usuario lido = usuarios.buscarPorId(salvo.getId()).orElseThrow();
        assertThat(lido.getPerfis()).containsExactlyInAnyOrder(Perfil.ADMIN, Perfil.ESTOQUE);
        assertThat(lido.isAtivo()).isTrue();
        assertThat(lido.getCriadoEm()).isNotNull();
        assertThat(lido.getAtualizadoEm()).isNotNull();
        assertThat(jdbc.queryForList("SELECT perfil FROM usuario_perfil", String.class))
                .containsExactlyInAnyOrder("ADMIN", "ESTOQUE");
    }

    @Test
    void emailDuplicadoEhRejeitadoMesmoComCaixaDiferente() {
        usuarios.salvar(new Usuario("dup@exemplo.com", "hash", List.of(Perfil.CLIENTE)));

        assertThatThrownBy(() -> usuarios.salvar(new Usuario("DUP@Exemplo.com", "hash", List.of(Perfil.CLIENTE))))
                .isInstanceOf(ConflitoException.class)
                .hasMessage("E-mail já cadastrado.");
    }

    @Test
    void conflitoDeEmailMantemApenasOUsuarioOriginal() {
        usuarios.salvar(new Usuario("original@exemplo.com", "hash-original", List.of(Perfil.ADMIN)));

        assertThatThrownBy(() -> usuarios.salvar(new Usuario("original@exemplo.com", "outro", List.of(Perfil.CLIENTE))))
                .isInstanceOf(ConflitoException.class);

        assertThat(jdbc.queryForList("SELECT senha_hash FROM usuario", String.class))
                .containsExactly("hash-original");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM usuario_perfil", Integer.class)).isEqualTo(1);
    }

    @Test
    void senhaPersistidaEhHashVerificavel() {
        String senha = "Demo1234";
        usuarios.salvar(new Usuario("hash@exemplo.com", passwordEncoder.encode(senha), List.of(Perfil.CLIENTE)));

        String gravado = jdbc.queryForObject("SELECT senha_hash FROM usuario", String.class);
        assertThat(gravado).isNotEqualTo(senha).doesNotContain(senha);
        assertThat(passwordEncoder.matches(senha, gravado)).isTrue();
        assertThat(passwordEncoder.matches("outra-senha1", gravado)).isFalse();
    }
}
