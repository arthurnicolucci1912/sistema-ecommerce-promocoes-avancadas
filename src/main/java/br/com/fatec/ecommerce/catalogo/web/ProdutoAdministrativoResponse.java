package br.com.fatec.ecommerce.catalogo.web;

import br.com.fatec.ecommerce.catalogo.aplicacao.ProdutoAdministrativo;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Visão administrativa do produto: inclui itens inativos e o estado de cada variação. */
public record ProdutoAdministrativoResponse(Long id, String nome, String descricao, String marca, Long categoriaId,
        boolean ativo, List<VariacaoResponse> variacoes) {

    public record VariacaoResponse(Long id, String sku, String nome, BigDecimal preco, boolean ativo,
            Map<String, String> atributos) {
    }

    static ProdutoAdministrativoResponse de(ProdutoAdministrativo produto) {
        return new ProdutoAdministrativoResponse(produto.id(), produto.nome(), produto.descricao(), produto.marca(),
                produto.categoriaId(), produto.ativo(),
                produto.variacoes().stream()
                        .map(v -> new VariacaoResponse(v.id(), v.sku(), v.nome(), v.preco().valor(), v.ativo(),
                                v.atributos()))
                        .toList());
    }
}
