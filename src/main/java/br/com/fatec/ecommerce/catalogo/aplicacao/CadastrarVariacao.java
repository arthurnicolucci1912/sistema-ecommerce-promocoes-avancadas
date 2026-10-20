package br.com.fatec.ecommerce.catalogo.aplicacao;

import br.com.fatec.ecommerce.catalogo.dominio.ItemVendavelRepository;
import br.com.fatec.ecommerce.catalogo.dominio.Produto;
import br.com.fatec.ecommerce.catalogo.dominio.ProdutoRepository;
import br.com.fatec.ecommerce.catalogo.dominio.VariacaoProduto;
import br.com.fatec.ecommerce.compartilhado.dominio.ConflitoException;
import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import br.com.fatec.ecommerce.compartilhado.dominio.PublicadorEventos;
import br.com.fatec.ecommerce.compartilhado.dominio.RecursoNaoEncontradoException;
import java.time.Instant;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso minimo de cadastro de uma variacao em um produto existente: confere o SKU unico no catalogo, adiciona
 * a variacao ao produto, salva e so entao publica os eventos acumulados pelo produto. {@link GerenciarProdutos}
 * delega a este caso de uso cada variacao nova, reaproveitando a checagem de SKU e a publicacao.
 *
 * <p>Pattern: Observer - papel: sujeito/publicador (via {@link PublicadorEventos}); os ouvintes (por exemplo,
 * o estoque) se inscrevem sem alterar esta classe.
 *
 * <p>Ver docs/04-design-patterns.md, secao 3.6 (Observer: eventos de dominio).
 */
@Service
@Transactional
public class CadastrarVariacao {

    private final ProdutoRepository produtos;
    private final ItemVendavelRepository itens;
    private final PublicadorEventos publicador;

    public CadastrarVariacao(ProdutoRepository produtos, ItemVendavelRepository itens, PublicadorEventos publicador) {
        this.produtos = produtos;
        this.itens = itens;
        this.publicador = publicador;
    }

    public VariacaoProduto cadastrar(Long produtoId, String sku, Dinheiro preco, Map<String, String> atributos) {
        Produto produto = produtos.buscarPorId(produtoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado."));
        if (sku != null && itens.existePorSku(sku.trim())) {
            throw new ConflitoException("Já existe um item do catálogo com o SKU " + sku.trim() + ".");
        }
        VariacaoProduto variacao = produto.adicionarVariacao(sku, preco, atributos, Instant.now());
        Produto salvo = produtos.salvar(produto);
        produto.consumirEventos().forEach(publicador::publicar);
        return salvo.getVariacoes().stream()
                .filter(v -> v.getSku().equals(variacao.getSku()))
                .findFirst()
                .orElseThrow();
    }
}
