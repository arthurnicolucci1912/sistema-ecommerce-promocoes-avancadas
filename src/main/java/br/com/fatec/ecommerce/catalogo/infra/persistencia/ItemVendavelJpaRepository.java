package br.com.fatec.ecommerce.catalogo.infra.persistencia;

import br.com.fatec.ecommerce.catalogo.dominio.Combo;
import br.com.fatec.ecommerce.catalogo.dominio.ItemVendavel;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ItemVendavelJpaRepository extends JpaRepository<ItemVendavel, Long> {

    Optional<ItemVendavel> findBySku(String sku);

    boolean existsBySku(String sku);

    List<ItemVendavel> findBySkuIn(Collection<String> skus);

    @Query("select c from Combo c where c.id = :id")
    Optional<Combo> buscarComboPorId(@Param("id") Long id);

    @Query("select c from Combo c order by c.nome, c.id")
    List<Combo> listarCombos();
}
