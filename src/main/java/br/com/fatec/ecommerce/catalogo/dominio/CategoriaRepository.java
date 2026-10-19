package br.com.fatec.ecommerce.catalogo.dominio;

import java.util.List;
import java.util.Optional;

/** Porta de persistencia de categorias (implementada em {@code infra.persistencia}). */
public interface CategoriaRepository {

    Categoria salvar(Categoria categoria);

    Optional<Categoria> buscarPorId(Long id);

    /** Remove a categoria e faz flush imediato. */
    void remover(Categoria categoria);

    /** Todas as categorias, a entrada de {@link ArvoreDeCategorias}. */
    List<Categoria> listarTodas();
}
