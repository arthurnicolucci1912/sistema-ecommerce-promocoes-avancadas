package br.com.fatec.ecommerce.pedido.aplicacao;

import br.com.fatec.ecommerce.compartilhado.dominio.PublicadorEventos;
import br.com.fatec.ecommerce.compartilhado.dominio.RecursoNaoEncontradoException;
import br.com.fatec.ecommerce.pedido.dominio.Pedido;
import br.com.fatec.ecommerce.pedido.dominio.PedidoRepository;
import br.com.fatec.ecommerce.pedido.dominio.Responsavel;
import java.time.Instant;
import java.util.function.BiConsumer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso das transicoes do pedido apos a criacao: aprovar ou recusar o pagamento, iniciar a preparacao,
 * enviar, entregar e cancelar. Cada operacao carrega o pedido, delega a transicao ao agregado (que a valida pelo
 * estado atual), salva e so entao publica os eventos acumulados. Uma transicao rejeitada nao salva nem publica;
 * uma disputa pelo mesmo pedido vira conflito (409) no repositorio.
 *
 * <p>Pattern: Observer - papel: sujeito/publicador (via {@link PublicadorEventos}); os ouvintes se inscrevem
 * sem alterar esta classe.
 *
 * <p>Ver docs/04-design-patterns.md, secao 3.6 (Observer: eventos de dominio).
 */
@Service
@Transactional
public class TransicionarPedido {

    private final PedidoRepository pedidos;
    private final PublicadorEventos publicador;

    public TransicionarPedido(PedidoRepository pedidos, PublicadorEventos publicador) {
        this.pedidos = pedidos;
        this.publicador = publicador;
    }

    /** O motivo e opcional (a simulacao de pagamento registra "pagamento simulado"). */
    public Pedido aprovarPagamento(Long pedidoId, Responsavel responsavel, String motivo) {
        return aplicar(pedidoId, (pedido, momento) -> pedido.confirmarPagamento(responsavel, motivo, momento));
    }

    public Pedido recusarPagamento(Long pedidoId, Responsavel responsavel, String motivo) {
        return aplicar(pedidoId, (pedido, momento) -> pedido.recusarPagamento(responsavel, motivo, momento));
    }

    public Pedido iniciarPreparacao(Long pedidoId, Responsavel responsavel) {
        return aplicar(pedidoId, (pedido, momento) -> pedido.iniciarPreparacao(responsavel, momento));
    }

    public Pedido enviar(Long pedidoId, Responsavel responsavel) {
        return aplicar(pedidoId, (pedido, momento) -> pedido.enviar(responsavel, momento));
    }

    public Pedido entregar(Long pedidoId, Responsavel responsavel) {
        return aplicar(pedidoId, (pedido, momento) -> pedido.entregar(responsavel, momento));
    }

    /** Motivo obrigatorio; a regra de quem pode cancelar em cada estado fica no proprio estado. */
    public Pedido cancelar(Long pedidoId, Responsavel responsavel, String motivo) {
        return aplicar(pedidoId, (pedido, momento) -> pedido.cancelar(responsavel, motivo, momento));
    }

    private Pedido aplicar(Long pedidoId, BiConsumer<Pedido, Instant> transicao) {
        Pedido pedido = pedidos.buscarPorId(pedidoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado."));
        transicao.accept(pedido, Instant.now());
        Pedido salvo = pedidos.salvar(pedido);
        pedido.consumirEventos().forEach(publicador::publicar);
        return salvo;
    }
}
