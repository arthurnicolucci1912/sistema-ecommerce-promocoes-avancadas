package br.com.fatec.ecommerce.entrega.infra;

import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import br.com.fatec.ecommerce.entrega.dominio.RegiaoEntrega;
import java.math.BigDecimal;

/** Adaptee: API por UF, peso em kg, quantidade, valor e custo em centavos, prazo inteiro. A tarifa vem da tabela. */
public class TransporteEconomicoFicticio {
    public record Orcamento(long centavos, int diasUteis) {}
    public Orcamento orcar(String uf, BigDecimal pesoKg, int itens, long valorCentavos, TabelaFreteSimulado tabela) {
        var regiao = RegiaoEntrega.da(uf);
        var t = tabela.tarifa(regiao);
        var custo = tabela.custo(regiao, pesoKg, itens, Dinheiro.deCentavos(valorCentavos));
        return new Orcamento(custo.valor().movePointRight(2).longValueExact(), t.dias());
    }
}
