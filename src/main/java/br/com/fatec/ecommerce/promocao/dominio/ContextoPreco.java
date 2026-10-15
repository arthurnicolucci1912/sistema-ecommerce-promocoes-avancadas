package br.com.fatec.ecommerce.promocao.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import java.time.Instant;
import java.util.List;

/**
 * Tudo o que as promocoes e condicoes de elegibilidade precisam saber para avaliar uma compra.
 * Fica em {@code promocao.dominio} (e nao em {@code preco}) porque {@code preco} depende de
 * {@code promocao}, e nao o contrario.
 *
 * <p>Contrato provisorio (onda 0): mudancas via PR revisado pelos consumidores e registradas em docs/03-arquitetura.md.
 *
 * @param itens          itens do carrinho
 * @param clienteId      cliente que compra
 * @param primeiraCompra se e a primeira compra do cliente
 * @param uf             unidade federativa (regiao) do endereco de entrega
 * @param subtotal       soma dos itens, sem descontos nem frete
 * @param momento        instante da avaliacao (vigencia das promocoes)
 */
public record ContextoPreco(
        List<ItemPreco> itens,
        Long clienteId,
        boolean primeiraCompra,
        String uf,
        Dinheiro subtotal,
        Instant momento) {

    public ContextoPreco {
        itens = itens == null ? List.of() : List.copyOf(itens);
    }
}
