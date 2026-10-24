package br.com.fatec.ecommerce.catalogo.aplicacao;

import br.com.fatec.ecommerce.catalogo.dominio.ComponenteEstoque;
import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import java.util.List;
import java.util.Set;

/**
 * Visao uniforme de um item vendavel (variacao, combo ou qualquer tipo futuro) para quem consome o catalogo
 * (carrinho, checkout, promocoes): sem distinguir o tipo.
 *
 * @param ativo                 situacao do proprio item
 * @param disponivelParaVenda   o item e todos os seus componentes estao ativos (o produto, no caso da variacao)
 * @param categoriaIds          ids das categorias do item e de todas as ancestrais (vazio se o item nao tem categoria)
 * @param componentes           componentes de estoque para uma unidade do item (variacao e quantidade)
 */
public record ItemVendavelInfo(Long id, String sku, String nome, Dinheiro preco, boolean ativo,
        boolean disponivelParaVenda, Set<Long> categoriaIds, List<ComponenteEstoque> componentes,
        String descricaoVariacao) {

    /** Compatibilidade com consumidores que nao precisam da descricao comercial adicionada no checkout. */
    public ItemVendavelInfo(Long id, String sku, String nome, Dinheiro preco, boolean ativo,
            boolean disponivelParaVenda, Set<Long> categoriaIds, List<ComponenteEstoque> componentes) {
        this(id, sku, nome, preco, ativo, disponivelParaVenda, categoriaIds, componentes, null);
    }

    public ItemVendavelInfo {
        categoriaIds = Set.copyOf(categoriaIds);
        componentes = List.copyOf(componentes);
    }
}
