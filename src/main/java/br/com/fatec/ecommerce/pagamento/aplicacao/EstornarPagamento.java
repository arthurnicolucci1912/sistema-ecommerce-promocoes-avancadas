package br.com.fatec.ecommerce.pagamento.aplicacao;

import br.com.fatec.ecommerce.compartilhado.dominio.*;
import br.com.fatec.ecommerce.pagamento.dominio.*;
import br.com.fatec.ecommerce.pedido.dominio.*;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Recuperacao de estorno sem repetir o cancelamento nem seus efeitos de estoque e cupom. */
@Service
@Transactional
public class EstornarPagamento {
    private final PedidoRepository pedidos;
    private final PagamentosRepository pagamentos;
    private final RegistroGateways gateways;
    private final Clock clock;
    public EstornarPagamento(PedidoRepository pedidos, PagamentosRepository pagamentos, RegistroGateways gateways, Clock clock) {
        this.pedidos=pedidos; this.pagamentos=pagamentos; this.gateways=gateways; this.clock=clock;
    }
    public Estorno executar(Long pedidoId, boolean novaTentativa) {
        var pedido = pedidos.bloquearPorId(pedidoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado."));
        if (pedido.getSituacao() != SituacaoPedido.CANCELADO) throw new ConflitoException("Estorno exige pedido cancelado.");
        var anterior = pagamentos.buscarEstorno(pedidoId);
        if (anterior.isPresent() && anterior.get().situacao() == Estorno.Situacao.CONCLUIDO) return anterior.get();
        var cobranca = pagamentos.buscarPorPedido(pedidoId);
        if (cobranca.isEmpty() || cobranca.get().situacao() != ResultadoCobranca.Situacao.APROVADO) {
            if (novaTentativa) throw new ConflitoException("Não existe cobrança aprovada para conciliar o estorno.");
            var pendencia = new Estorno(pedidoId, null, pedido.getTotal(), Estorno.Situacao.FALHOU, null, 0,
                    "Pedido legado sem transação aprovada conhecida; exige conciliação manual.", clock.instant());
            pagamentos.salvar(pendencia);
            return pendencia;
        }
        var c = cobranca.get();
        var estorno = anterior.orElseGet(() -> new Estorno(pedidoId, c.idTransacaoExterna(), c.total(),
                Estorno.Situacao.PENDENTE, null, 0, null, clock.instant()));
        // Persistencia permanece fora do catch: erro no banco deve abortar a transacao.
        ResultadoEstorno resultado = null;
        String erro = null;
        try {
            resultado = gateways.fornecedor(c.fornecedor()).estornar(new SolicitacaoEstorno(c.idTransacaoExterna(), c.total()));
            if (resultado != null && resultado.estornado()
                    && (resultado.idTransacaoExterna() == null || resultado.idTransacaoExterna().isBlank()
                        || resultado.idTransacaoExterna().length() > 160)) {
                resultado = null;
                erro = "Fornecedor devolveu comprovante de estorno inválido.";
            }
        } catch (RuntimeException falhaFornecedor) {
            erro = "Falha no fornecedor simulado; tente novamente ou confira sua configuração.";
        }
        var atualizado = estorno.registrar(resultado, erro, clock.instant());
        pagamentos.salvar(atualizado);
        return atualizado;
    }
}
