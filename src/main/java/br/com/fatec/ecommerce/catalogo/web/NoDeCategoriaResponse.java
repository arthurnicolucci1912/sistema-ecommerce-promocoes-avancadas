package br.com.fatec.ecommerce.catalogo.web;

import br.com.fatec.ecommerce.catalogo.aplicacao.NoDeCategoria;
import java.util.List;

/** Nó da árvore pública de categorias, com as subcategorias em {@code filhas}. */
public record NoDeCategoriaResponse(Long id, String nome, List<NoDeCategoriaResponse> filhas) {

    static NoDeCategoriaResponse de(NoDeCategoria no) {
        return new NoDeCategoriaResponse(no.id(), no.nome(),
                no.filhas().stream().map(NoDeCategoriaResponse::de).toList());
    }
}
