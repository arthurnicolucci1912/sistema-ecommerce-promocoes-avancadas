package br.com.fatec.ecommerce.pedido.aplicacao;

import br.com.fatec.ecommerce.compartilhado.dominio.PublicadorEventos;
import br.com.fatec.ecommerce.compartilhado.aplicacao.PublicarOperacaoAdministrativa;
import br.com.fatec.ecommerce.compartilhado.dominio.RecursoNaoEncontradoException;
import br.com.fatec.ecommerce.pedido.dominio.Pedido;
import br.com.fatec.ecommerce.pedido.dominio.PedidoRepository;
import br.com.fatec.ecommerce.pedido.dominio.Responsavel;
import java.time.Instant;
import java.util.function.BiConsumer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>Observer administrativo: publica snapshots permitidos na transação original; falha da trilha reverte a operação.
 *
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

    private final PublicarOperacaoAdministrativa auditoria;
    private final PedidoRepository pedidos;
    private final PublicadorEventos publicador;

    public TransicionarPedido(PedidoRepository pedidos, PublicadorEventos publicador, PublicarOperacaoAdministrativa auditoria) {
        this.auditoria = auditoria;
        this.pedidos = pedidos;
        this.publicador = publicador;
    }

    /** O motivo e opcional; pagamento registra fornecedor e transacao externa para rastreabilidade. */
    public Pedido aprovarPagamento(Long pedidoId, Responsavel responsavel, String motivo) {
        return aplicar(pedidoId, (pedido, momento) -> pedido.confirmarPagamento(responsavel, motivo, momento));
    }

    public Pedido recusarPagamento(Long pedidoId, Responsavel responsavel, String motivo) {
        return aplicar(pedidoId, (pedido, momento) -> pedido.recusarPagamento(responsavel, motivo, momento));
    }

    public Pedido iniciarPreparacao(Long pedidoId, Responsavel responsavel) {
        return aplicarAdministrativa(pedidoId, responsavel, "PEDIDO_PREPARACAO_INICIADA", null,
                (pedido, momento) -> pedido.iniciarPreparacao(responsavel, momento));
    }

    public Pedido enviar(Long pedidoId, Responsavel responsavel) {
        return aplicarAdministrativa(pedidoId, responsavel, "PEDIDO_ENVIADO", null,
                (pedido, momento) -> pedido.enviar(responsavel, momento));
    }

    public Pedido entregar(Long pedidoId, Responsavel responsavel) {
        return aplicarAdministrativa(pedidoId, responsavel, "PEDIDO_ENTREGUE", null,
                (pedido, momento) -> pedido.entregar(responsavel, momento));
    }

    /** Motivo obrigatorio; a regra de quem pode cancelar em cada estado fica no proprio estado. */
    public Pedido cancelar(Long pedidoId, Responsavel responsavel, String motivo) {
        Pedido pedido = pedidos.bloquearPorId(pedidoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado."));
        var antes = pedido.getSituacao().name();
        Pedido salvo = aplicar(pedido, (atual, momento) -> atual.cancelar(responsavel, motivo, momento));
        auditar(salvo, responsavel, "PEDIDO_CANCELADO", antes, motivo);
        return salvo;
    }

    private Pedido aplicarAdministrativa(Long id, Responsavel responsavel, String operacao, String motivo,
            BiConsumer<Pedido, Instant> transicao) {
        Pedido pedido = pedidos.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado."));
        String antes = pedido.getSituacao().name();
        Pedido salvo = aplicar(pedido, transicao);
        auditar(salvo, responsavel, operacao, antes, motivo);
        return salvo;
    }

    private void auditar(Pedido pedido, Responsavel responsavel, String operacao, String antes, String motivo) {
        if (!responsavel.ehAdministrativo()) return;
        var depois = new java.util.LinkedHashMap<String,Object>();
        depois.put("estado", pedido.getSituacao().name()); depois.put("motivo", motivo);
        auditoria.publicar(operacao, "PEDIDO", pedido.getId(), java.util.Map.of("estado", antes), depois);
    }

    private Pedido aplicar(Long pedidoId, BiConsumer<Pedido, Instant> transicao) {
        Pedido pedido = pedidos.buscarPorId(pedidoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado."));
        return aplicar(pedido, transicao);
    }

    private Pedido aplicar(Pedido pedido, BiConsumer<Pedido, Instant> transicao) {
        transicao.accept(pedido, Instant.now());
        Pedido salvo = pedidos.salvar(pedido);
        pedido.consumirEventos().forEach(publicador::publicar);
        return salvo;
    }
}
