package br.com.fatec.ecommerce.pedido.aplicacao;

import br.com.fatec.ecommerce.cliente.dominio.ClienteRepository;
import br.com.fatec.ecommerce.compartilhado.dominio.DadoInvalidoException;
import br.com.fatec.ecommerce.compartilhado.dominio.Pagina;
import br.com.fatec.ecommerce.compartilhado.dominio.RecursoNaoEncontradoException;
import br.com.fatec.ecommerce.pedido.dominio.Pedido;
import br.com.fatec.ecommerce.pedido.dominio.PedidoRepository;
import br.com.fatec.ecommerce.pedido.dominio.ResumoPedido;
import br.com.fatec.ecommerce.pedido.dominio.SituacaoPedido;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de consulta de pedidos: o cliente enxerga so os proprios (sempre por id e cliente, e o que nao e
 * dele nao e encontrado) e a administracao enxerga todos, com filtro opcional por situacao. As listagens devolvem
 * resumos, sem carregar itens, descontos nem historico; o detalhe devolve o pedido completo.
 */
@Service
@Transactional(readOnly = true)
public class ConsultarPedidos {

    public static final int TAMANHO_PADRAO = 20;
    public static final int TAMANHO_MAXIMO = 100;

    private final PedidoRepository pedidos;
    private final ClienteRepository clientes;

    public ConsultarPedidos(PedidoRepository pedidos, ClienteRepository clientes) {
        this.pedidos = pedidos;
        this.clientes = clientes;
    }

    public Pagina<ResumoPedido> listarDoCliente(Long usuarioId, int pagina, int tamanho) {
        validar(pagina, tamanho);
        return pedidos.listarResumosDoCliente(clienteDe(usuarioId), pagina, limitar(tamanho));
    }

    public Pedido consultarDoCliente(Long usuarioId, Long pedidoId) {
        return pedidos.buscarPorIdECliente(pedidoId, clienteDe(usuarioId))
                .orElseThrow(ConsultarPedidos::naoEncontrado);
    }

    public Pagina<ResumoPedido> listar(SituacaoPedido situacao, int pagina, int tamanho) {
        validar(pagina, tamanho);
        return pedidos.listarResumos(situacao, pagina, limitar(tamanho));
    }

    public Pedido consultar(Long pedidoId) {
        return pedidos.buscarPorId(pedidoId).orElseThrow(ConsultarPedidos::naoEncontrado);
    }

    private Long clienteDe(Long usuarioId) {
        return clientes.buscarPorUsuarioId(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado.")).getId();
    }

    /** Pedido inexistente ou de outro cliente: a mesma resposta nos dois casos. */
    private static RecursoNaoEncontradoException naoEncontrado() {
        return new RecursoNaoEncontradoException("Pedido não encontrado.");
    }

    private static void validar(int pagina, int tamanho) {
        if (pagina < 0) {
            throw new DadoInvalidoException("A página não pode ser negativa.");
        }
        if (tamanho < 1) {
            throw new DadoInvalidoException("O tamanho da página deve ser de ao menos 1.");
        }
    }

    private static int limitar(int tamanho) {
        return Math.min(tamanho, TAMANHO_MAXIMO);
    }
}
