package br.com.fatec.ecommerce.catalogo.aplicacao;

import br.com.fatec.ecommerce.catalogo.dominio.ArvoreDeCategorias;
import br.com.fatec.ecommerce.catalogo.dominio.Categoria;
import br.com.fatec.ecommerce.catalogo.dominio.CategoriaRepository;
import br.com.fatec.ecommerce.catalogo.dominio.ItemVendavel;
import br.com.fatec.ecommerce.catalogo.dominio.ItemVendavelRepository;
import br.com.fatec.ecommerce.compartilhado.dominio.RecursoNaoEncontradoException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Contrato do catalogo para os proximos modulos (carrinho, checkout, promocoes): dado um SKU, devolve a visao
 * uniforme {@link ItemVendavelInfo}. Sem restricao de perfil: e consumido por outros servicos, nao exposto como rota.
 *
 * <p>Pattern: Composite - papel: cliente. So usa o contrato de {@link ItemVendavel} ({@code preco()},
 * {@code estaDisponivelParaVenda()}, {@code componentesDeEstoque(1)} e {@code categoriaIdsDiretas()}), sem
 * {@code instanceof} nem {@code switch} por tipo: um tipo novo de item vendavel passa por aqui sem alteracao.
 *
 * <p>Categorias: a variacao tem a categoria do produto e suas ancestrais; o combo nao tem categoria propria (o
 * conjunto e vazio), pois tem preco proprio e e enderecado pelo SKU. As ancestrais saem de
 * {@link ArvoreDeCategorias}, calculada uma vez por chamada.
 *
 * <p>Ver docs/04-design-patterns.md, secao 3.8 (Composite).
 */
@Service
@Transactional(readOnly = true)
public class ConsultarItensVendaveis {

    private final ItemVendavelRepository itens;
    private final CategoriaRepository categorias;

    public ConsultarItensVendaveis(ItemVendavelRepository itens, CategoriaRepository categorias) {
        this.itens = itens;
        this.categorias = categorias;
    }

    /** O item do SKU; 404 se nao existir. */
    public ItemVendavelInfo porSku(String sku) {
        return porSkus(List.of(sku)).get(0);
    }

    /**
     * Os itens dos SKUs, na ordem pedida e sem repeticao, em poucas consultas; 404 (nomeando os SKUs) se algum nao
     * existir.
     */
    public List<ItemVendavelInfo> porSkus(Collection<String> skus) {
        List<ItemVendavelInfo> encontrados = existentesPorSkus(skus);
        Set<String> presentes = new LinkedHashSet<>();
        encontrados.forEach(item -> presentes.add(item.sku()));
        List<String> ausentes = skus.stream().map(sku -> sku == null ? "" : sku.trim())
                .filter(sku -> !presentes.contains(sku)).distinct().toList();
        if (!ausentes.isEmpty()) {
            throw new RecursoNaoEncontradoException("Item não encontrado para o SKU: " + String.join(", ", ausentes));
        }
        return encontrados;
    }

    /** Consulta tolerante para carrinhos antigos; itens que sairam do catalogo ficam de fora. */
    public List<ItemVendavelInfo> existentesPorSkus(Collection<String> skus) {
        Set<String> pedidos = new LinkedHashSet<>();
        skus.forEach(sku -> pedidos.add(sku == null ? "" : sku.trim()));
        if (pedidos.isEmpty()) {
            return List.of();
        }
        Map<String, ItemVendavel> encontrados = new LinkedHashMap<>();
        itens.buscarPorSkus(pedidos).forEach(item -> encontrados.put(item.getSku(), item));
        ArvoreDeCategorias arvore = new ArvoreDeCategorias(categorias.listarTodas());
        return pedidos.stream().filter(encontrados::containsKey).map(sku -> visao(encontrados.get(sku), arvore)).toList();
    }

    private static ItemVendavelInfo visao(ItemVendavel item, ArvoreDeCategorias arvore) {
        Set<Long> categoriaIds = new LinkedHashSet<>();
        for (Long diretaId : item.categoriaIdsDiretas()) {
            arvore.categoriaEAncestrais(diretaId).stream().map(Categoria::getId).forEach(categoriaIds::add);
        }
        return new ItemVendavelInfo(item.getId(), item.getSku(), item.getNome(), item.preco(), item.isAtivo(),
                item.estaDisponivelParaVenda(), categoriaIds, item.componentesDeEstoque(1), item.descricaoVariacao());
    }
}
