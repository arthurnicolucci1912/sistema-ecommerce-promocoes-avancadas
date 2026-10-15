package br.com.fatec.ecommerce.catalogo.infra.persistencia;

import br.com.fatec.ecommerce.catalogo.dominio.Categoria;
import br.com.fatec.ecommerce.catalogo.dominio.CategoriaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** Adaptador JPA da porta {@link CategoriaRepository}. */
@Repository
class CategoriaRepositoryJpa implements CategoriaRepository {

    private final CategoriaJpaRepository jpa;

    CategoriaRepositoryJpa(CategoriaJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Categoria salvar(Categoria categoria) {
        return jpa.saveAndFlush(categoria);
    }

    @Override
    public Optional<Categoria> buscarPorId(Long id) {
        return jpa.findById(id);
    }

    @Override
    public List<Categoria> listarTodas() {
        return jpa.findAll();
    }
}
