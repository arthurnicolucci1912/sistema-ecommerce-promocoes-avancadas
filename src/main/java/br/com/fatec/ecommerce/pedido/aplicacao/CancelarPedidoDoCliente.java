package br.com.fatec.ecommerce.pedido.aplicacao;

import br.com.fatec.ecommerce.cliente.dominio.ClienteRepository;
import br.com.fatec.ecommerce.compartilhado.dominio.RecursoNaoEncontradoException;
import br.com.fatec.ecommerce.pedido.dominio.Pedido;
import br.com.fatec.ecommerce.pedido.dominio.PedidoRepository;
import br.com.fatec.ecommerce.pedido.dominio.Responsavel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: o cliente cancela o proprio pedido. Confere que o pedido e dele (por id e cliente; senao nao e
 * encontrado) e delega o cancelamento a {@link TransicionarPedido}, que o submete ao estado atual do pedido: e
 * o estado que decide se o cliente pode cancelar naquela situacao.
 */
@Service
@Transactional
public class CancelarPedidoDoCliente {

    private final PedidoRepository pedidos;
    private final ClienteRepository clientes;
    private final TransicionarPedido transicionar;

    public CancelarPedidoDoCliente(PedidoRepository pedidos, ClienteRepository clientes,
            TransicionarPedido transicionar) {
        this.pedidos = pedidos;
        this.clientes = clientes;
        this.transicionar = transicionar;
    }

    public Pedido executar(Long usuarioId, Long pedidoId, String motivo) {
        Long clienteId = clientes.buscarPorUsuarioId(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado.")).getId();
        if (pedidos.buscarPorIdECliente(pedidoId, clienteId).isEmpty()) {
            throw new RecursoNaoEncontradoException("Pedido não encontrado.");
        }
        return transicionar.cancelar(pedidoId, Responsavel.cliente(usuarioId), motivo);
    }
}
