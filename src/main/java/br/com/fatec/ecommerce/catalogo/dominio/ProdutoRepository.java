package br.com.fatec.ecommerce.catalogo.dominio;

import java.util.Optional;

/** Porta de persistencia de produtos (implementada em {@code infra.persistencia}). */
public interface ProdutoRepository {

    /** Grava o produto e suas variacoes e faz flush imediato (os ids ficam disponiveis para os eventos). */
    Produto salvar(Produto produto);

    Optional<Produto> buscarPorId(Long id);
}
