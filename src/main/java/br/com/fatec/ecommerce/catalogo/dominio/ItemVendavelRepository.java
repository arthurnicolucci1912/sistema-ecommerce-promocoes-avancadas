package br.com.fatec.ecommerce.catalogo.dominio;

import java.util.Collection;
import java.util.List;
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

    /** Os itens com os SKUs dados (os que nao existem ficam de fora), em uma unica consulta. */
    List<ItemVendavel> buscarPorSkus(Collection<String> skus);

    /** O combo com o id dado; vazio se o id nao existir ou for de outro tipo de item. */
    Optional<Combo> buscarComboPorId(Long id);

    /** Todos os combos, ativos ou nao, ordenados pelo nome. */
    List<Combo> listarCombos();
}
