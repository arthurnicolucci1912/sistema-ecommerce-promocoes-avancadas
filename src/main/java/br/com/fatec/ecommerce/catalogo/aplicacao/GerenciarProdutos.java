package br.com.fatec.ecommerce.catalogo.aplicacao;

import br.com.fatec.ecommerce.catalogo.dominio.Categoria;
import br.com.fatec.ecommerce.catalogo.dominio.CategoriaRepository;
import br.com.fatec.ecommerce.catalogo.dominio.Produto;
import br.com.fatec.ecommerce.catalogo.dominio.ProdutoRepository;
import br.com.fatec.ecommerce.compartilhado.dominio.DadoInvalidoException;
import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import br.com.fatec.ecommerce.compartilhado.dominio.RecursoNaoEncontradoException;
import java.time.Instant;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de administracao de produtos e variacoes (ADMIN e OPERADOR_COMERCIAL): cadastrar produto com
 * variacoes, alterar dados, adicionar variacao, alterar preco, inativar produto e inativar variacao. Produtos e
 * variacoes nunca sao excluidos.
 *
 * <p>Toda variacao nova passa por {@link CadastrarVariacao}, o ponto unico de checagem de SKU e de publicacao de
 * {@code VariacaoCadastrada} (o estoque cria o saldo zero pelo ouvinte). SKU repetido, contra variacoes e combos, e
 * 409. Categoria inexistente e dado invalido (400).
 */
@Service
@Transactional
@PreAuthorize("hasAnyRole('ADMIN','OPERADOR_COMERCIAL')")
public class GerenciarProdutos {

    private final ProdutoRepository produtos;
    private final CategoriaRepository categorias;
    private final CadastrarVariacao cadastrarVariacao;

    public GerenciarProdutos(ProdutoRepository produtos, CategoriaRepository categorias,
            CadastrarVariacao cadastrarVariacao) {
        this.produtos = produtos;
        this.categorias = categorias;
        this.cadastrarVariacao = cadastrarVariacao;
    }

    /** Cadastra o produto com ao menos uma variacao; falhando qualquer uma, nada e gravado (mesma transacao). */
    public ProdutoAdministrativo cadastrar(String nome, String descricao, String marca, Long categoriaId,
            List<NovaVariacao> variacoes) {
        if (variacoes == null || variacoes.isEmpty()) {
            throw new DadoInvalidoException("O produto precisa de ao menos uma variação.");
        }
        Produto produto = produtos.salvar(Produto.criar(nome, descricao, marca, categoria(categoriaId), Instant.now()));
        variacoes.forEach(v -> cadastrarVariacao.cadastrar(produto.getId(), v.sku(), v.preco(), v.atributos()));
        return ProdutoAdministrativo.de(produto(produto.getId()));
    }

    public ProdutoAdministrativo alterarDados(Long produtoId, String nome, String descricao, String marca,
            Long categoriaId) {
        Produto produto = produto(produtoId);
        produto.alterarDados(nome, descricao, marca, categoria(categoriaId));
        return ProdutoAdministrativo.de(produtos.salvar(produto));
    }

    public ProdutoAdministrativo adicionarVariacao(Long produtoId, NovaVariacao variacao) {
        cadastrarVariacao.cadastrar(produtoId, variacao.sku(), variacao.preco(), variacao.atributos());
        return ProdutoAdministrativo.de(produto(produtoId));
    }

    public ProdutoAdministrativo alterarPrecoDaVariacao(Long variacaoId, Dinheiro novoPreco) {
        Produto produto = produtoDaVariacao(variacaoId);
        produto.variacaoPorId(variacaoId).alterarPreco(novoPreco);
        return ProdutoAdministrativo.de(produtos.salvar(produto));
    }

    /** Inativa o produto e todas as suas variacoes; pedidos existentes nao sao afetados. */
    public void inativarProduto(Long produtoId) {
        Produto produto = produto(produtoId);
        produto.inativar();
        produtos.salvar(produto);
    }

    public void inativarVariacao(Long variacaoId) {
        Produto produto = produtoDaVariacao(variacaoId);
        produto.variacaoPorId(variacaoId).inativar();
        produtos.salvar(produto);
    }

    private Produto produto(Long id) {
        return produtos.buscarPorId(id).orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado."));
    }

    private Produto produtoDaVariacao(Long variacaoId) {
        return produtos.buscarPorVariacaoId(variacaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Variação não encontrada."));
    }

    private Categoria categoria(Long categoriaId) {
        if (categoriaId == null) {
            throw new DadoInvalidoException("A categoria do produto é obrigatória.");
        }
        return categorias.buscarPorId(categoriaId)
                .orElseThrow(() -> new DadoInvalidoException("A categoria informada não existe."));
    }
}
