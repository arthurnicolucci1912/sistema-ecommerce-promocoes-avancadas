package br.com.fatec.ecommerce.catalogo.infra.persistencia;

import br.com.fatec.ecommerce.catalogo.dominio.ItemVendavel;
import br.com.fatec.ecommerce.catalogo.dominio.ItemVendavelRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** Adaptador JPA da porta {@link ItemVendavelRepository}. */
@Repository
class ItemVendavelRepositoryJpa implements ItemVendavelRepository {

    private final ItemVendavelJpaRepository jpa;

    ItemVendavelRepositoryJpa(ItemVendavelJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public <T extends ItemVendavel> T salvar(T item) {
        return jpa.saveAndFlush(item);
    }

    @Override
    public Optional<ItemVendavel> buscarPorId(Long id) {
        return jpa.findById(id);
    }

    @Override
    public Optional<ItemVendavel> buscarPorSku(String sku) {
        return jpa.findBySku(sku);
    }

    @Override
    public boolean existePorSku(String sku) {
        return jpa.existsBySku(sku);
    }
}
