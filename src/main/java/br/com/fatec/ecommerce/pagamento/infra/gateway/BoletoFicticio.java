package br.com.fatec.ecommerce.pagamento.infra.gateway;

import java.math.BigDecimal;

/** API de boleto distinta: emissao de titulo e comprovante de reembolso. */
public class BoletoFicticio {
    public record Titulo(String nossoNumero, BigDecimal valor) {}
    public Titulo emitir(String referencia, BigDecimal valor) { return new Titulo("BOL-" + referencia, valor); }
    public String reembolsar(Titulo titulo, boolean falhar) { return falhar ? null : "REF-" + titulo.nossoNumero(); }
}
