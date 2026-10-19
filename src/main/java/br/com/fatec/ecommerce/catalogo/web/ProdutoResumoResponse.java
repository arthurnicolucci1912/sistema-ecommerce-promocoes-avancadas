package br.com.fatec.ecommerce.catalogo.web;

import br.com.fatec.ecommerce.catalogo.aplicacao.ProdutoResumo;
import java.math.BigDecimal;

/** Linha da listagem pública de produtos: faixa de preço das variações ativas. */
public record ProdutoResumoResponse(Long id, String nome, String marca, Long categoriaId, BigDecimal precoMinimo,
        BigDecimal precoMaximo) {

    static ProdutoResumoResponse de(ProdutoResumo resumo) {
        return new ProdutoResumoResponse(resumo.id(), resumo.nome(), resumo.marca(), resumo.categoriaId(),
                resumo.precoMinimo().valor(), resumo.precoMaximo().valor());
    }
}
