package br.com.fatec.ecommerce.cliente;

import br.com.fatec.ecommerce.cliente.dominio.Cliente;
import br.com.fatec.ecommerce.cliente.dominio.ClienteRepository;
import br.com.fatec.ecommerce.cliente.dominio.Cpf;
import br.com.fatec.ecommerce.seguranca.UsuariosParaTeste;
import br.com.fatec.ecommerce.seguranca.dominio.Perfil;
import br.com.fatec.ecommerce.seguranca.dominio.Usuario;
import org.springframework.stereotype.Component;

/** Apoio aos testes do modulo cliente: cria usuario CLIENTE + cliente direto pelos repositorios e emite o token. */
@Component
public class ClientesParaTeste {

    /** Cliente pronto para uso em testes: o usuario, o cliente e o cabecalho {@code Authorization} dele. */
    public record ClienteDeTeste(Usuario usuario, Cliente cliente, String bearer) {
    }

    private final UsuariosParaTeste usuarios;
    private final ClienteRepository clientes;

    public ClientesParaTeste(UsuariosParaTeste usuarios, ClienteRepository clientes) {
        this.usuarios = usuarios;
        this.clientes = clientes;
    }

    /** {@code semente} define o CPF (via {@link CpfsParaTeste#valido(int)}); use valores diferentes por cliente. */
    public ClienteDeTeste criar(String email, int semente) {
        Usuario usuario = usuarios.criar(email, Perfil.CLIENTE);
        Cliente cliente = clientes.salvar(new Cliente(usuario.getId(), "Cliente " + semente,
                Cpf.de(CpfsParaTeste.valido(semente)), null));
        return new ClienteDeTeste(usuario, cliente, usuarios.bearer(usuario));
    }
}
