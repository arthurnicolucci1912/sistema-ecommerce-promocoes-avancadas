package br.com.fatec.ecommerce.catalogo;

import static br.com.fatec.ecommerce.catalogo.CatalogoParaTeste.MOMENTO;

import br.com.fatec.ecommerce.catalogo.aplicacao.CadastrarVariacao;
import br.com.fatec.ecommerce.catalogo.dominio.ArvoreDeCategorias;
import br.com.fatec.ecommerce.catalogo.dominio.Categoria;
import br.com.fatec.ecommerce.catalogo.dominio.CategoriaRepository;
import br.com.fatec.ecommerce.catalogo.dominio.Combo;
import br.com.fatec.ecommerce.catalogo.dominio.ItemVendavel;
import br.com.fatec.ecommerce.catalogo.dominio.ItemVendavelRepository;
import br.com.fatec.ecommerce.catalogo.dominio.Produto;
import br.com.fatec.ecommerce.catalogo.dominio.ProdutoRepository;
import br.com.fatec.ecommerce.catalogo.dominio.VariacaoProduto;
import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Apoio aos testes de integracao do catalogo: grava categorias, produtos, variacoes e combos direto pelas portas
 * (sem passar pelos casos de uso de administracao, que exigem perfil). A variacao entra por
 * {@link CadastrarVariacao}, entao publica {@code VariacaoCadastrada} e o estoque cria o saldo zero.
 */
@Component
public class CatalogoPersistidoParaTeste {

    private final CategoriaRepository categorias;
    private final ProdutoRepository produtos;
    private final ItemVendavelRepository itens;
    private final CadastrarVariacao cadastrarVariacao;

    public CatalogoPersistidoParaTeste(CategoriaRepository categorias, ProdutoRepository produtos,
            ItemVendavelRepository itens, CadastrarVariacao cadastrarVariacao) {
        this.categorias = categorias;
        this.produtos = produtos;
        this.itens = itens;
        this.cadastrarVariacao = cadastrarVariacao;
    }

    public Categoria categoria(String nome, Categoria pai) {
        return categorias.salvar(Categoria.criar(nome, pai, new ArvoreDeCategorias(categorias.listarTodas())));
    }

    /** Produto ativo, sem variacoes. */
    public Produto produto(String nome, Categoria categoria) {
        return produtos.salvar(Produto.criar(nome, "Descrição de " + nome, null, categoria, MOMENTO));
    }

    /** Variacao ativa em um produto novo (nome "Produto <sku>") da categoria dada. */
    public VariacaoProduto variacao(Categoria categoria, String sku, String preco) {
        return variacaoDe(produto("Produto " + sku, categoria), sku, preco);
    }

    public VariacaoProduto variacaoDe(Produto produto, String sku, String preco) {
        return cadastrarVariacao.cadastrar(produto.getId(), sku, Dinheiro.de(preco), Map.of("tamanho", sku));
    }

    public Combo combo(String sku, String preco, ItemVendavel item, int quantidade) {
        Combo combo = new Combo(sku, "Combo " + sku, Dinheiro.de(preco));
        combo.adicionarItem(item, quantidade);
        return itens.salvar(combo);
    }

    public Combo combo(String sku, String preco, ItemVendavel primeiro, int quantidadeDoPrimeiro,
            ItemVendavel segundo, int quantidadeDoSegundo) {
        Combo combo = new Combo(sku, "Combo " + sku, Dinheiro.de(preco));
        combo.adicionarItem(primeiro, quantidadeDoPrimeiro);
        combo.adicionarItem(segundo, quantidadeDoSegundo);
        return itens.salvar(combo);
    }
}
