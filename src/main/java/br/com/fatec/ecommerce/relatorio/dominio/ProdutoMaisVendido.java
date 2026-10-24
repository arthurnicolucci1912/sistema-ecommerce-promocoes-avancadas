package br.com.fatec.ecommerce.relatorio.dominio;

import java.math.BigDecimal;

/** Linha do ranking: SKU, nome e variação como registrados no pedido, com quantidade e valor das vendas. */
public record ProdutoMaisVendido(String sku, String nomeProduto, String descricaoVariacao, long quantidadeVendida,
        BigDecimal valorVendido) {
}
