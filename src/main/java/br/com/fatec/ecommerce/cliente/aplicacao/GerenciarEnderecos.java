package br.com.fatec.ecommerce.cliente.aplicacao;

import br.com.fatec.ecommerce.cliente.dominio.Cliente;
import br.com.fatec.ecommerce.cliente.dominio.ClienteRepository;
import br.com.fatec.ecommerce.cliente.dominio.Endereco;
import br.com.fatec.ecommerce.cliente.dominio.EnderecoRepository;
import br.com.fatec.ecommerce.cliente.dominio.RegraEnderecoPrincipal;
import br.com.fatec.ecommerce.compartilhado.dominio.RecursoNaoEncontradoException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: gestao dos enderecos do cliente autenticado. Todo acesso e por (id, clienteId): endereco
 * inexistente, inativo ou de outro cliente resulta em "nao encontrado". A escolha do principal segue
 * {@link RegraEnderecoPrincipal}; aqui so se garante a ordem de gravacao exigida pelo indice unico parcial
 * (rebaixar o anterior e gravar antes de promover o novo; o repositorio faz flush a cada {@code salvar}).
 */
@Service
@Transactional
public class GerenciarEnderecos {

    private final ClienteRepository clientes;
    private final EnderecoRepository enderecos;

    public GerenciarEnderecos(ClienteRepository clientes, EnderecoRepository enderecos) {
        this.clientes = clientes;
        this.enderecos = enderecos;
    }

    @Transactional(readOnly = true)
    public List<Endereco> listar(Long usuarioId) {
        return enderecos.listarAtivos(clienteDe(usuarioId).getId());
    }

    public Endereco cadastrar(Long usuarioId, DadosDoEndereco dados) {
        Long clienteId = clienteDe(usuarioId).getId();
        Endereco novo = new Endereco(clienteId, dados.apelido(), dados.cep(), dados.logradouro(), dados.numero(),
                dados.complemento(), dados.bairro(), dados.cidade(), dados.uf());
        if (RegraEnderecoPrincipal.novoEnderecoDeveSerPrincipal(enderecos.listarAtivos(clienteId))) {
            novo.marcarPrincipal();
        }
        return enderecos.salvar(novo);
    }

    @Transactional(readOnly = true)
    public Endereco consultar(Long usuarioId, Long id) {
        return buscar(usuarioId, id);
    }

    public Endereco alterar(Long usuarioId, Long id, DadosDoEndereco dados) {
        Endereco endereco = buscar(usuarioId, id);
        endereco.alterarDados(dados.apelido(), dados.cep(), dados.logradouro(), dados.numero(),
                dados.complemento(), dados.bairro(), dados.cidade(), dados.uf());
        return enderecos.salvar(endereco);
    }

    /** Inativa o endereco; se era o principal, o ativo mais antigo restante assume. */
    public void remover(Long usuarioId, Long id) {
        Endereco endereco = buscar(usuarioId, id);
        endereco.inativar();
        enderecos.salvar(endereco);
        RegraEnderecoPrincipal.sucessorAposRemocao(enderecos.listarAtivos(endereco.getClienteId()))
                .ifPresent(sucessor -> {
                    sucessor.marcarPrincipal();
                    enderecos.salvar(sucessor);
                });
    }

    /** Marca o endereco como principal, desmarcando (e gravando) o anterior antes. */
    public Endereco marcarPrincipal(Long usuarioId, Long id) {
        Endereco alvo = buscar(usuarioId, id);
        if (alvo.isPrincipal()) {
            return alvo;
        }
        RegraEnderecoPrincipal.principalASerSubstituido(enderecos.listarAtivos(alvo.getClienteId()), alvo)
                .ifPresent(anterior -> {
                    anterior.desmarcarPrincipal();
                    enderecos.salvar(anterior);
                });
        alvo.marcarPrincipal();
        return enderecos.salvar(alvo);
    }

    private Endereco buscar(Long usuarioId, Long id) {
        Long clienteId = clienteDe(usuarioId).getId();
        return enderecos.buscarAtivo(id, clienteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Endereço não encontrado."));
    }

    private Cliente clienteDe(Long usuarioId) {
        return clientes.buscarPorUsuarioId(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado."));
    }
}
