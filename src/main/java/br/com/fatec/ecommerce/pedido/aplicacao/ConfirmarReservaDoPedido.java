package br.com.fatec.ecommerce.pedido.aplicacao;

import br.com.fatec.ecommerce.compartilhado.dominio.PublicadorEventos;
import br.com.fatec.ecommerce.compartilhado.dominio.RecursoNaoEncontradoException;
import br.com.fatec.ecommerce.pedido.dominio.Pedido;
import br.com.fatec.ecommerce.pedido.dominio.PedidoRepository;
import br.com.fatec.ecommerce.pedido.dominio.Responsavel;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: leva o pedido de {@code CRIADO} a {@code AGUARDANDO_PAGAMENTO} depois que o estoque foi reservado
 * (o checkout o chama na mesma transacao de {@link CriarPedido}). O responsavel e o sistema.
 *
 * <p>Pattern: Observer - papel: sujeito/publicador. Depois de salvar, publica pelo {@link PublicadorEventos} os
 * eventos acumulados pelo pedido ({@code PedidoCriado}), sem conhecer os interessados.
 *
 * <p>Ver docs/04-design-patterns.md, secao 3.6 (Observer: eventos de dominio).
 */
@Service
@Transactional
public class ConfirmarReservaDoPedido {

    private final PedidoRepository pedidos;
    private final PublicadorEventos publicador;

    public ConfirmarReservaDoPedido(PedidoRepository pedidos, PublicadorEventos publicador) {
        this.pedidos = pedidos;
        this.publicador = publicador;
    }

    public Pedido executar(Long pedidoId) {
        Pedido pedido = pedidos.buscarPorId(pedidoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado."));
        pedido.aguardarPagamento(Responsavel.sistema(), Instant.now());
        Pedido salvo = pedidos.salvar(pedido);
        pedido.consumirEventos().forEach(publicador::publicar);
        return salvo;
    }
}
