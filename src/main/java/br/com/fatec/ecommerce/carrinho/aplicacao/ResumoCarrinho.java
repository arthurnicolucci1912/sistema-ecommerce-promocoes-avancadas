package br.com.fatec.ecommerce.carrinho.aplicacao;

import java.math.BigDecimal;
import java.util.List;
import br.com.fatec.ecommerce.carrinho.aplicacao.ConsultarDisponibilidadeDoCarrinho.Insuficiencia;

/** Visao recalculada; precos de referencia de linhas ausentes sao identificados por disponivelParaVenda=false. */
public record ResumoCarrinho(List<Linha> itens, BigDecimal subtotal, List<Desconto> descontos,
        BigDecimal frete, BigDecimal total, List<Insuficiencia> insuficiencias,
        br.com.fatec.ecommerce.promocao.dominio.SituacaoCupom cupom) {
    public ResumoCarrinho(List<Linha> itens, BigDecimal subtotal, List<Desconto> descontos,
            BigDecimal frete, BigDecimal total, List<Insuficiencia> insuficiencias) {
        this(itens, subtotal, descontos, frete, total, insuficiencias, null);
    }
    public record Linha(String sku, String nome, int quantidade, BigDecimal precoUnitario, BigDecimal subtotal,
            boolean disponivelParaVenda, boolean precoDeReferencia) { }
    public record Desconto(String nome, BigDecimal valor, String alvo, String origem, Long referenciaId, String codigo) {
        public Desconto(String nome, BigDecimal valor, String alvo) { this(nome, valor, alvo, null, null, null); }
    }

    public ResumoCarrinho {
        itens = List.copyOf(itens);
        descontos = List.copyOf(descontos);
        insuficiencias = List.copyOf(insuficiencias);
    }
}
