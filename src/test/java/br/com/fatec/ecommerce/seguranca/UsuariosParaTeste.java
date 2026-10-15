package br.com.fatec.ecommerce.seguranca;

import br.com.fatec.ecommerce.seguranca.aplicacao.EmissorDeToken;
import br.com.fatec.ecommerce.seguranca.dominio.Perfil;
import br.com.fatec.ecommerce.seguranca.dominio.Usuario;
import br.com.fatec.ecommerce.seguranca.dominio.UsuarioRepository;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Apoio aos testes: limpa as tabelas, cria usuarios direto pelo repositorio e emite tokens reais. */
@org.springframework.stereotype.Component
public class UsuariosParaTeste {

    public static final String SENHA = "Demo1234";

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final EmissorDeToken emissor;
    private final JdbcTemplate jdbc;

    public UsuariosParaTeste(UsuarioRepository usuarios, PasswordEncoder passwordEncoder, EmissorDeToken emissor,
            JdbcTemplate jdbc) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.emissor = emissor;
        this.jdbc = jdbc;
    }

    public void limpar() {
        jdbc.update("DELETE FROM reserva_estoque");
        jdbc.update("DELETE FROM saldo_estoque");
        jdbc.update("DELETE FROM combo_item");
        jdbc.update("DELETE FROM combo");
        jdbc.update("DELETE FROM variacao_atributo");
        jdbc.update("DELETE FROM variacao_produto");
        jdbc.update("DELETE FROM item_vendavel");
        jdbc.update("DELETE FROM produto");
        jdbc.update("DELETE FROM categoria");
        jdbc.update("DELETE FROM pedido_historico");
        jdbc.update("DELETE FROM pedido_desconto");
        jdbc.update("DELETE FROM pedido_item");
        jdbc.update("DELETE FROM pedido");
        jdbc.update("DELETE FROM endereco");
        jdbc.update("DELETE FROM cliente");
        jdbc.update("DELETE FROM usuario_perfil");
        jdbc.update("DELETE FROM usuario");
    }

    public Usuario criar(String email, Perfil... perfis) {
        return usuarios.salvar(new Usuario(email, passwordEncoder.encode(SENHA), List.of(perfis)));
    }

    public Usuario criarInativo(String email, Perfil... perfis) {
        Usuario usuario = criar(email, perfis);
        usuario.inativar();
        return usuarios.salvar(usuario);
    }

    /** Cabecalho {@code Authorization} com um token real do usuario. */
    public String bearer(Usuario usuario) {
        return "Bearer " + emissor.emitir(usuario).valor();
    }
}
