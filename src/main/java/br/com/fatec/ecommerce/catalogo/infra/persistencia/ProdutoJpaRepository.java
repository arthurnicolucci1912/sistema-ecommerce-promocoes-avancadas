package br.com.fatec.ecommerce.catalogo.infra.persistencia;

import br.com.fatec.ecommerce.catalogo.dominio.Produto;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ProdutoJpaRepository extends JpaRepository<Produto, Long>, JpaSpecificationExecutor<Produto> {

    @Query("select p from Produto p join p.variacoes v where v.id = :variacaoId")
    Optional<Produto> buscarPorVariacaoId(@Param("variacaoId") Long variacaoId);

    boolean existsByCategoriaId(Long categoriaId);
}
