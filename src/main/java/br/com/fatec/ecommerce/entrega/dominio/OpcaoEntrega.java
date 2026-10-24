package br.com.fatec.ecommerce.entrega.dominio;

import java.math.BigDecimal;

/** Cotacao identificada; custo bruto e prazo, sem beneficios aplicados ainda. */
public record OpcaoEntrega(String codigo, String fornecedor, String modalidade, BigDecimal custoBruto, int prazoDias) {
    public OpcaoEntrega {
        if (fornecedor == null || !fornecedor.matches("[a-z0-9_-]{1,80}")
                || !java.util.Objects.equals(codigo, fornecedor + ":" + modalidade) || custoBruto == null)
            throw new br.com.fatec.ecommerce.compartilhado.dominio.DadoInvalidoException("Opção de entrega inválida.");
        var dinheiro = br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro.de(custoBruto);
        new CotacaoFrete(modalidade, dinheiro, prazoDias);
        custoBruto = dinheiro.valor();
    }
    public OpcaoEntrega(String fornecedor, CotacaoFrete cotacao) {
        this(fornecedor + ":" + cotacao.modalidade(), fornecedor, cotacao.modalidade(), cotacao.custo().valor(), cotacao.prazoDias());
    }
}
