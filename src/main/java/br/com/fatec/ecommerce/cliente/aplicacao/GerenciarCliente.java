package br.com.fatec.ecommerce.cliente.aplicacao;

import br.com.fatec.ecommerce.cliente.dominio.Cliente;
import br.com.fatec.ecommerce.cliente.dominio.ClienteRepository;
import br.com.fatec.ecommerce.compartilhado.dominio.RecursoNaoEncontradoException;
import br.com.fatec.ecommerce.seguranca.dominio.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Caso de uso: o cliente autenticado consulta e altera os proprios dados (somente nome e telefone). */
@Service
@Transactional
public class GerenciarCliente {

    private final ClienteRepository clientes;
    private final UsuarioRepository usuarios;

    public GerenciarCliente(ClienteRepository clientes, UsuarioRepository usuarios) {
        this.clientes = clientes;
        this.usuarios = usuarios;
    }

    @Transactional(readOnly = true)
    public DadosDoCliente consultar(Long usuarioId) {
        return dados(buscar(usuarioId));
    }

    public DadosDoCliente alterar(Long usuarioId, String nome, String telefone) {
        Cliente cliente = buscar(usuarioId);
        cliente.alterarDados(nome, telefone);
        return dados(clientes.salvar(cliente));
    }

    private Cliente buscar(Long usuarioId) {
        return clientes.buscarPorUsuarioId(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado."));
    }

    private DadosDoCliente dados(Cliente cliente) {
        String email = usuarios.buscarPorId(cliente.getUsuarioId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado.")).getEmail();
        return new DadosDoCliente(cliente.getId(), cliente.getNome(), email, cliente.getCpf().digitos(),
                cliente.getTelefone());
    }
}
