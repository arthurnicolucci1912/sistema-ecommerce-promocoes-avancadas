package br.com.fatec.ecommerce.catalogo.aplicacao;

import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;

/** Item da listagem publica de produtos: a faixa de preco considera todas as variacoes ativas do produto. */
public record ProdutoResumo(Long id, String nome, String marca, Long categoriaId, Dinheiro precoMinimo,
        Dinheiro precoMaximo) {
}
