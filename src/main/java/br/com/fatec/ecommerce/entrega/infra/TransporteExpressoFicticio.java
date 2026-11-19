package br.com.fatec.ecommerce.entrega.infra;

import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import br.com.fatec.ecommerce.entrega.dominio.RegiaoEntrega;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Adaptee distinto: pedido e resposta em documento textual (regiao, pesoKg, itens, valor em reais); a tarifa vem
 * da tabela.
 */
public class TransporteExpressoFicticio {
    public Map<String,String> estimar(Map<String,String> pedido, TabelaFreteSimulado tabela) {
        var regiao = RegiaoEntrega.valueOf(pedido.get("regiao"));
        var t = tabela.tarifa(regiao);
        var custo = tabela.custo(regiao, new BigDecimal(pedido.get("pesoKg")),
                Integer.parseInt(pedido.get("itens")), Dinheiro.de(pedido.get("valor")));
        return Map.of("preco", custo.toString(), "prazo", Integer.toString(t.dias()));
    }
}
