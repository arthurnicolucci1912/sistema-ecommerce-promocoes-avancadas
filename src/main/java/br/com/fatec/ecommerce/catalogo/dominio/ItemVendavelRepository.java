package br.com.fatec.ecommerce.catalogo.dominio;

import java.util.Optional;

/**
 * Porta de persistencia dos itens vendaveis (variacoes e combos, via a raiz do Composite), implementada em
 * {@code infra.persistencia}. O SKU e unico em todo o catalogo.
 */
public interface ItemVendavelRepository {

    <T extends ItemVendavel> T salvar(T item);

    Optional<ItemVendavel> buscarPorId(Long id);

    Optional<ItemVendavel> buscarPorSku(String sku);

    boolean existePorSku(String sku);
}
