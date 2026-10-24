package br.com.fatec.ecommerce.entrega.infra;

import br.com.fatec.ecommerce.entrega.dominio.*;
import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** Pattern Adapter, papel Adapter: traduz TransporteEconomicoFicticio para o Target ProvedorFrete. */
@Component
public class AdaptadorEntregaEconomica implements ProvedorFrete {
    private final TransporteEconomicoFicticio api = new TransporteEconomicoFicticio();
    private final TabelaFreteSimulado tabela;
    public AdaptadorEntregaEconomica(Environment env) { tabela = new TabelaFreteSimulado(env, "economica"); }
    @Override public String fornecedor() { return "economica-simulada"; }
    @Override public String modalidade() { return "ECONOMICA"; }
    @Override public boolean atende(ContextoFrete c) { return tabela.atende(c.uf()); }
    @Override public CotacaoFrete cotar(ContextoFrete c) {
        var r = api.orcar(c.uf(), tabela);
        return new CotacaoFrete(modalidade(), Dinheiro.deCentavos(r.centavos()), r.diasUteis());
    }
}
