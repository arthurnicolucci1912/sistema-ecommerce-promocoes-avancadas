package br.com.fatec.ecommerce.catalogo.aplicacao;

import br.com.fatec.ecommerce.catalogo.dominio.Produto;
import br.com.fatec.ecommerce.catalogo.dominio.VariacaoProduto;
import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Visao de um produto para a administracao: todas as variacoes, ativas ou nao. E um instantaneo (nao depende de
 * sessao aberta), para o controller montar a resposta sem carregamento tardio.
 */
public record ProdutoAdministrativo(Long id, String nome, String descricao, String marca, Long categoriaId,
        boolean ativo, List<Variacao> variacoes) {

    public record Variacao(Long id, String sku, String nome, Dinheiro preco, boolean ativo,
            Map<String, String> atributos) {

        static Variacao de(VariacaoProduto variacao) {
            return new Variacao(variacao.getId(), variacao.getSku(), variacao.getNome(), variacao.preco(),
                    variacao.isAtivo(), new LinkedHashMap<>(variacao.getAtributos()));
        }
    }

    static ProdutoAdministrativo de(Produto produto) {
        return new ProdutoAdministrativo(produto.getId(), produto.getNome(), produto.getDescricao(),
                produto.getMarca(), produto.getCategoria().getId(), produto.isAtivo(),
                produto.getVariacoes().stream().map(Variacao::de).toList());
    }
}
