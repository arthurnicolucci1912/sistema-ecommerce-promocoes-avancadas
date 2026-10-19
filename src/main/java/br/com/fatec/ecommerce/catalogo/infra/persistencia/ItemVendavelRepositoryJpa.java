package br.com.fatec.ecommerce.catalogo.infra.persistencia;

import br.com.fatec.ecommerce.catalogo.dominio.Combo;
import br.com.fatec.ecommerce.catalogo.dominio.ItemVendavel;
import br.com.fatec.ecommerce.catalogo.dominio.ItemVendavelRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
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
        try {
            return jpa.saveAndFlush(item);
        } catch (DataIntegrityViolationException erro) {
            throw ViolacoesDeUnicidade.deSku(erro);
        }
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

    @Override
    public List<ItemVendavel> buscarPorSkus(Collection<String> skus) {
        if (skus.isEmpty()) {
            return List.of();
        }
        return jpa.findBySkuIn(skus);
    }

    @Override
    public Optional<Combo> buscarComboPorId(Long id) {
        return jpa.buscarComboPorId(id);
    }

    @Override
    public List<Combo> listarCombos() {
        return jpa.listarCombos();
    }
}
