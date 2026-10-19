package br.com.fatec.ecommerce.catalogo.web;

import br.com.fatec.ecommerce.catalogo.dominio.Categoria;

public record CategoriaResponse(Long id, String nome, Long paiId) {

    static CategoriaResponse de(Categoria categoria) {
        return new CategoriaResponse(categoria.getId(), categoria.getNome(), categoria.getPaiId());
    }
}
