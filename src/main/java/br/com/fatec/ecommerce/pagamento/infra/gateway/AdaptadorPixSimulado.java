package br.com.fatec.ecommerce.pagamento.infra.gateway;

import br.com.fatec.ecommerce.pagamento.dominio.*;
import org.springframework.stereotype.Component;

/** Pattern Adapter, papel Adapter: traduz a API de documentos PIX para o Target GatewayPagamento. */
@Component
public class AdaptadorPixSimulado implements GatewayPagamento {
    private final PixFicticio api = new PixFicticio();
    private final CenariosPagamento cenarios;
    public AdaptadorPixSimulado(CenariosPagamento cenarios) { this.cenarios = cenarios; }
    @Override public String fornecedor() { return "pix-simulado"; }
    @Override public boolean suporta(FormaPagamento f) { return f == FormaPagamento.PIX; }
    @Override public ResultadoCobranca cobrar(SolicitacaoCobranca s) {
        return new ResultadoCobranca(ResultadoCobranca.Situacao.PENDENTE, api.gerar(s.valor().valor(), s.pedidoId().toString()).get("txid"));
    }
    @Override public ResultadoEstorno estornar(SolicitacaoEstorno s) {
        return new ResultadoEstorno(api.devolver(s.idTransacaoExterna(), s.valor().valor(), cenarios.falharEstorno()), "REF-" + s.idTransacaoExterna());
    }
}
