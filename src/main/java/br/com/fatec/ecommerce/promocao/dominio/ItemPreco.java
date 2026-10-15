package br.com.fatec.ecommerce.promocao.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import java.util.Set;

/**
 * Item do carrinho visto pelas regras de promocao e preco.
 *
 * <p>Contrato provisorio (onda 0): mudancas via PR revisado pelos consumidores e registradas em docs/03-arquitetura.md.
 *
 * @param varianteId     identificador da variante do produto
 * @param sku            codigo da variante
 * @param categoriaIds   categorias do produto (inclui ancestrais, quando houver hierarquia)
 * @param quantidade     quantidade de unidades
 * @param precoUnitario  preco de tabela de uma unidade
 */
public record ItemPreco(Long varianteId, String sku, Set<Long> categoriaIds, int quantidade, Dinheiro precoUnitario) {

    public ItemPreco {
        categoriaIds = categoriaIds == null ? Set.of() : Set.copyOf(categoriaIds);
    }

    /** Preco unitario vezes a quantidade. */
    public Dinheiro subtotal() {
        return precoUnitario.multiplicar(quantidade);
    }
}
