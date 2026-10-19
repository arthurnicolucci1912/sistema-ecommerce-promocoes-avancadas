package br.com.fatec.ecommerce.catalogo.web;

import br.com.fatec.ecommerce.catalogo.aplicacao.ProdutoDetalhe;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Detalhe público do produto: variações ativas com a disponibilidade em estoque de cada uma. */
public record ProdutoDetalheResponse(Long id, String nome, String descricao, String marca, Long categoriaId,
        List<VariacaoResponse> variacoes) {

    public record VariacaoResponse(Long id, String sku, String nome, BigDecimal preco, Map<String, String> atributos,
            int disponivel) {
    }

    static ProdutoDetalheResponse de(ProdutoDetalhe produto) {
        return new ProdutoDetalheResponse(produto.id(), produto.nome(), produto.descricao(), produto.marca(),
                produto.categoriaId(),
                produto.variacoes().stream()
                        .map(v -> new VariacaoResponse(v.id(), v.sku(), v.nome(), v.preco().valor(), v.atributos(),
                                v.disponivel()))
                        .toList());
    }
}
