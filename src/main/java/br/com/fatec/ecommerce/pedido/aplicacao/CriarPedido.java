package br.com.fatec.ecommerce.pedido.aplicacao;

import br.com.fatec.ecommerce.cliente.dominio.ClienteRepository;
import br.com.fatec.ecommerce.compartilhado.dominio.RecursoNaoEncontradoException;
import br.com.fatec.ecommerce.pedido.dominio.DadosNovoPedido;
import br.com.fatec.ecommerce.pedido.dominio.Pedido;
import br.com.fatec.ecommerce.pedido.dominio.PedidoRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: cria o pedido em {@code CRIADO} a partir de um snapshot comercial (o checkout o usa). O cliente
 * precisa existir e o snapshot e conferido pelo agregado. Nao publica evento: o primeiro acontecimento
 * anunciado e {@code PedidoCriado}, na confirmacao da reserva ({@link ConfirmarReservaDoPedido}).
 */
@Service
@Transactional
public class CriarPedido {

    private final PedidoRepository pedidos;
    private final ClienteRepository clientes;

    public CriarPedido(PedidoRepository pedidos, ClienteRepository clientes) {
        this.pedidos = pedidos;
        this.clientes = clientes;
    }

    public Pedido executar(DadosNovoPedido dados) {
        if (!clientes.existePorId(dados.clienteId())) {
            throw new RecursoNaoEncontradoException("Cliente não encontrado.");
        }
        return pedidos.salvar(Pedido.criar(dados, Instant.now()));
    }
}
