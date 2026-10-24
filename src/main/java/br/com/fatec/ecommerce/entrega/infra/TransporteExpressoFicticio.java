package br.com.fatec.ecommerce.entrega.infra;

import br.com.fatec.ecommerce.entrega.dominio.RegiaoEntrega;
import java.util.Map;

/** Adaptee distinto: documento textual por regiao, preco em reais e prazo. */
public class TransporteExpressoFicticio {
    public Map<String,String> estimar(String regiao, TabelaFreteSimulado tabela) {
        var t = tabela.tarifa(RegiaoEntrega.valueOf(regiao));
        return Map.of("preco", t.valor().toString(), "prazo", Integer.toString(t.dias()));
    }
}
