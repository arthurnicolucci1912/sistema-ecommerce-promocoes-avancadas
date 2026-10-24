package br.com.fatec.ecommerce.entrega.infra;

import br.com.fatec.ecommerce.entrega.dominio.*;
import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** Pattern Adapter, papel Adapter: traduz TransporteExpressoFicticio para o Target ProvedorFrete. */
@Component
public class AdaptadorEntregaExpressa implements ProvedorFrete {
    private final TransporteExpressoFicticio api = new TransporteExpressoFicticio();
    private final TabelaFreteSimulado tabela;
    public AdaptadorEntregaExpressa(Environment env) { tabela = new TabelaFreteSimulado(env, "expressa"); }
    @Override public String fornecedor() { return "expressa-simulada"; }
    @Override public String modalidade() { return "EXPRESSA"; }
    @Override public boolean atende(ContextoFrete c) { return tabela.atende(c.uf()); }
    @Override public CotacaoFrete cotar(ContextoFrete c) {
        var r = api.estimar(RegiaoEntrega.da(c.uf()).name(), tabela);
        return new CotacaoFrete(modalidade(), Dinheiro.de(r.get("preco")), Integer.parseInt(r.get("prazo")));
    }
}
