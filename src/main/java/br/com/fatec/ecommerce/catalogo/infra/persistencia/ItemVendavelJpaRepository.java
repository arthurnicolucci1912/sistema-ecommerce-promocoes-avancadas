package br.com.fatec.ecommerce.catalogo.infra.persistencia;

import br.com.fatec.ecommerce.catalogo.dominio.ItemVendavel;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface ItemVendavelJpaRepository extends JpaRepository<ItemVendavel, Long> {

    Optional<ItemVendavel> findBySku(String sku);

    boolean existsBySku(String sku);
}
