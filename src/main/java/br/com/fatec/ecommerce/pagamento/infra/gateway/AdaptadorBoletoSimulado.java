package br.com.fatec.ecommerce.pagamento.infra.gateway;

import br.com.fatec.ecommerce.pagamento.dominio.*;
import org.springframework.stereotype.Component;

/** Pattern Adapter, papel Adapter: traduz titulos/comprovantes para o Target GatewayPagamento. */
@Component
public class AdaptadorBoletoSimulado implements GatewayPagamento {
    private final BoletoFicticio api = new BoletoFicticio();
    private final CenariosPagamento cenarios;
    public AdaptadorBoletoSimulado(CenariosPagamento cenarios) { this.cenarios = cenarios; }
    @Override public String fornecedor() { return "boleto-simulado"; }
    @Override public boolean suporta(FormaPagamento f) { return f == FormaPagamento.BOLETO; }
    @Override public ResultadoCobranca cobrar(SolicitacaoCobranca s) {
        return new ResultadoCobranca(ResultadoCobranca.Situacao.PENDENTE, api.emitir(s.pedidoId().toString(), s.valor().valor()).nossoNumero());
    }
    @Override public ResultadoEstorno estornar(SolicitacaoEstorno s) {
        String r = api.reembolsar(new BoletoFicticio.Titulo(s.idTransacaoExterna(), s.valor().valor()), cenarios.falharEstorno());
        return new ResultadoEstorno(r != null, r);
    }
}
