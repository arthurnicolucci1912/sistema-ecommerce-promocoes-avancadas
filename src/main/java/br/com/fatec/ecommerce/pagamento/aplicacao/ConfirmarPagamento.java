package br.com.fatec.ecommerce.pagamento.aplicacao;

import br.com.fatec.ecommerce.compartilhado.dominio.*;
import br.com.fatec.ecommerce.pagamento.dominio.*;
import br.com.fatec.ecommerce.pedido.aplicacao.TransicionarPedido;
import br.com.fatec.ecommerce.pedido.dominio.*;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Confirma uma unica vez na mesma transacao do pedido e dos ouvintes comerciais. */
@Service
@Transactional
public class ConfirmarPagamento {
    private final PagamentosRepository pagamentos;
    private final PedidoRepository pedidos;
    private final TransicionarPedido transicionar;
    private final Clock clock;
    public ConfirmarPagamento(PagamentosRepository pagamentos, PedidoRepository pedidos, TransicionarPedido transicionar, Clock clock) {
        this.pagamentos = pagamentos; this.pedidos = pedidos; this.transicionar = transicionar; this.clock = clock;
    }
    public Cobranca executar(String id, ResultadoCobranca.Situacao resultado) {
        var confirmacao = new ConfirmacaoPagamento(id, resultado, clock.instant());
        var referencia = pagamentos.buscarPorTransacao(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cobrança não encontrada."));
        var pedido = pedidos.bloquearPorId(referencia.pedidoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado."));
        var cobranca = pagamentos.buscarPorTransacao(id).orElseThrow();
        var anterior = pagamentos.buscarConfirmacao(id);
        if (anterior.isPresent()) {
            conferirResultado(anterior.get(), resultado);
            return cobranca;
        }
        if (pedido.getSituacao() != SituacaoPedido.AGUARDANDO_PAGAMENTO) {
            org.slf4j.LoggerFactory.getLogger(getClass()).info("Confirmação rejeitada para pedido {} no estado {}", pedido.getId(), pedido.getSituacao());
            throw new ConflitoException("O pedido não está aguardando pagamento.");
        }
        if (!pagamentos.registrar(confirmacao)) {
            conferirResultado(pagamentos.buscarConfirmacao(id).orElseThrow(), resultado);
            return pagamentos.buscarPorTransacao(id).orElseThrow();
        }
        var confirmada = cobranca.confirmar(resultado, clock.instant());
        pagamentos.atualizar(confirmada);
        String motivo = "pagamento simulado: " + cobranca.fornecedor() + " / " + id;
        if (resultado == ResultadoCobranca.Situacao.APROVADO)
            transicionar.aprovarPagamento(pedido.getId(), Responsavel.sistema(), motivo);
        else transicionar.recusarPagamento(pedido.getId(), Responsavel.sistema(), motivo);
        return confirmada;
    }
    private static void conferirResultado(ConfirmacaoPagamento anterior, ResultadoCobranca.Situacao resultado) {
        if (anterior.resultado() != resultado) throw new ConflitoException("A transação já possui outro resultado.");
    }
}
