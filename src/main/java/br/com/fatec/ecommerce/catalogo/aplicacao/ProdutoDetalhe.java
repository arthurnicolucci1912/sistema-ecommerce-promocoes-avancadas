package br.com.fatec.ecommerce.catalogo.aplicacao;

import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import java.util.List;
import java.util.Map;

/** Detalhe publico de um produto ativo: so as variacoes ativas, com atributos, preco e unidades disponiveis. */
public record ProdutoDetalhe(Long id, String nome, String descricao, String marca, Long categoriaId,
        List<Variacao> variacoes) {

    public ProdutoDetalhe {
        variacoes = List.copyOf(variacoes);
    }

    /** {@code disponivel} e o saldo disponivel do estoque (0 se a variacao ainda nao tem saldo). */
    public record Variacao(Long id, String sku, String nome, Dinheiro preco, Map<String, String> atributos,
            int disponivel) {
    }
}
