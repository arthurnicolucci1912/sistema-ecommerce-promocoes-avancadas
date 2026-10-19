package br.com.fatec.ecommerce.catalogo.aplicacao;

import java.util.List;

/** No da arvore publica de categorias: a categoria e as suas subcategorias diretas, ordenadas pelo nome. */
public record NoDeCategoria(Long id, String nome, List<NoDeCategoria> filhas) {

    public NoDeCategoria {
        filhas = List.copyOf(filhas);
    }
}
