package br.com.fatec.ecommerce.catalogo.infra.persistencia;

import br.com.fatec.ecommerce.catalogo.dominio.Produto;
import br.com.fatec.ecommerce.catalogo.dominio.ProdutoRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** Adaptador JPA da porta {@link ProdutoRepository}. */
@Repository
class ProdutoRepositoryJpa implements ProdutoRepository {

    private final ProdutoJpaRepository jpa;

    ProdutoRepositoryJpa(ProdutoJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Produto salvar(Produto produto) {
        return jpa.saveAndFlush(produto);
    }

    @Override
    public Optional<Produto> buscarPorId(Long id) {
        return jpa.findById(id);
    }
}
