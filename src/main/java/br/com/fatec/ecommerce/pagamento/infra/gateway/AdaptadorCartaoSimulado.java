package br.com.fatec.ecommerce.pagamento.infra.gateway;

import br.com.fatec.ecommerce.pagamento.dominio.*;
import org.springframework.stereotype.Component;

/** Pattern Adapter, papel Adapter: traduz autorizacao em centavos para GatewayPagamento (Target). */
@Component
public class AdaptadorCartaoSimulado implements GatewayPagamento {
    private final CartaoFicticio api = new CartaoFicticio();
    private final CenariosPagamento cenarios;
    public AdaptadorCartaoSimulado(CenariosPagamento cenarios) { this.cenarios = cenarios; }
    @Override public String fornecedor() { return "cartao-simulado"; }
    @Override public boolean suporta(FormaPagamento f) { return f == FormaPagamento.CARTAO_CREDITO; }
    @Override public ResultadoCobranca cobrar(SolicitacaoCobranca s) {
        var r = api.autorizar(s.pedidoId().toString(), s.valor().valor().movePointRight(2).longValueExact(), cenarios.cartaoAprovado());
        return new ResultadoCobranca(r.codigo() == 0 ? ResultadoCobranca.Situacao.APROVADO : ResultadoCobranca.Situacao.RECUSADO, r.numero());
    }
    @Override public ResultadoEstorno estornar(SolicitacaoEstorno s) {
        var r = api.devolver(s.idTransacaoExterna(), s.valor().valor().movePointRight(2).longValueExact(), cenarios.falharEstorno());
        return new ResultadoEstorno(r.codigo() == 0, r.numero());
    }
}
