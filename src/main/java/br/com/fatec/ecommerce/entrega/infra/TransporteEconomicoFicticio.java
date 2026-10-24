package br.com.fatec.ecommerce.entrega.infra;

import br.com.fatec.ecommerce.entrega.dominio.RegiaoEntrega;

/** Adaptee: API por UF, centavos e prazo inteiro. */
public class TransporteEconomicoFicticio {
    public record Orcamento(long centavos, int diasUteis) {}
    public Orcamento orcar(String uf, TabelaFreteSimulado tabela) {
        var t = tabela.tarifa(RegiaoEntrega.da(uf));
        return new Orcamento(t.valor().valor().movePointRight(2).longValueExact(), t.dias());
    }
}
