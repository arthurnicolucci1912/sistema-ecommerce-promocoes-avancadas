package br.com.fatec.ecommerce.cliente.aplicacao;

import br.com.fatec.ecommerce.cliente.dominio.Cliente;
import br.com.fatec.ecommerce.cliente.dominio.ClienteRepository;
import br.com.fatec.ecommerce.cliente.dominio.Cpf;
import br.com.fatec.ecommerce.compartilhado.dominio.ConflitoException;
import br.com.fatec.ecommerce.seguranca.dominio.Perfil;
import br.com.fatec.ecommerce.seguranca.dominio.Usuario;
import br.com.fatec.ecommerce.seguranca.dominio.UsuarioRepository;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: autocadastro de cliente. Cria o usuario (perfil CLIENTE, via porta do modulo seguranca)
 * e o cliente na mesma transacao; qualquer falha desfaz os dois.
 */
@Service
@Transactional
public class CadastrarCliente {

    private final UsuarioRepository usuarios;
    private final ClienteRepository clientes;
    private final PasswordEncoder passwordEncoder;

    public CadastrarCliente(UsuarioRepository usuarios, ClienteRepository clientes, PasswordEncoder passwordEncoder) {
        this.usuarios = usuarios;
        this.clientes = clientes;
        this.passwordEncoder = passwordEncoder;
    }

    public DadosDoCliente executar(String nome, String email, String senha, String cpf, String telefone) {
        Cpf cpfNormalizado = Cpf.de(cpf);
        if (usuarios.existePorEmail(email)) {
            throw new ConflitoException("E-mail já cadastrado.");
        }
        if (clientes.existePorCpf(cpfNormalizado)) {
            throw new ConflitoException("CPF já cadastrado.");
        }
        Usuario usuario = usuarios.salvar(new Usuario(email, passwordEncoder.encode(senha), List.of(Perfil.CLIENTE)));
        Cliente cliente = clientes.salvar(new Cliente(usuario.getId(), nome, cpfNormalizado, telefone));
        return new DadosDoCliente(cliente.getId(), cliente.getNome(), usuario.getEmail(),
                cliente.getCpf().digitos(), cliente.getTelefone());
    }
}
