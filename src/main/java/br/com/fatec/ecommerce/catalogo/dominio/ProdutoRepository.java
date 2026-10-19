package br.com.fatec.ecommerce.catalogo.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import br.com.fatec.ecommerce.compartilhado.dominio.Pagina;
import java.util.Collection;
import java.util.Optional;

/** Porta de persistencia de produtos (implementada em {@code infra.persistencia}). */
public interface ProdutoRepository {

    /** Grava o produto e suas variacoes e faz flush imediato (os ids ficam disponiveis para os eventos). */
    Produto salvar(Produto produto);

    Optional<Produto> buscarPorId(Long id);

    /** O produto que tem a variacao com o id dado. */
    Optional<Produto> buscarPorVariacaoId(Long variacaoId);

    boolean existePorCategoriaId(Long categoriaId);

    /**
     * Pagina de produtos ativos que tem ao menos uma variacao ativa, ordenados pelo nome. Todos os filtros sao
     * opcionais (nulo): categorias (qualquer uma das ids), trecho do nome (sem diferenciar maiusculas) e faixa de
     * preco, que vale para as variacoes ativas (o produto entra se alguma variacao ativa esta na faixa, extremos
     * incluidos).
     */
    Pagina<Produto> pesquisarVisiveis(Collection<Long> categoriaIds, String trechoDoNome, Dinheiro precoMinimo,
            Dinheiro precoMaximo, int pagina, int tamanho);
}
