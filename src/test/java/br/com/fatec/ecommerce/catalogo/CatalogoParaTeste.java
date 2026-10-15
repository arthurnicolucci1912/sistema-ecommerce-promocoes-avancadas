package br.com.fatec.ecommerce.catalogo;

import br.com.fatec.ecommerce.catalogo.dominio.ArvoreDeCategorias;
import br.com.fatec.ecommerce.catalogo.dominio.Categoria;
import br.com.fatec.ecommerce.catalogo.dominio.Combo;
import br.com.fatec.ecommerce.catalogo.dominio.Produto;
import br.com.fatec.ecommerce.catalogo.dominio.VariacaoProduto;
import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.test.util.ReflectionTestUtils;

/** Apoio aos testes unitarios do catalogo: monta objetos em memoria e atribui ids (que o banco atribuiria). */
public final class CatalogoParaTeste {

    public static final Instant MOMENTO = Instant.parse("2026-01-10T12:00:00Z");

    private CatalogoParaTeste() {
    }

    public static <T> T comId(T objeto, long id) {
        ReflectionTestUtils.setField(objeto, "id", id);
        return objeto;
    }

    /** Categoria em memoria com id; o pai pode ser nulo. */
    public static Categoria categoria(long id, String nome, Categoria pai) {
        return comId(Categoria.criar(nome, pai, new ArvoreDeCategorias(List.of())), id);
    }

    public static Produto produto(String nome, Categoria categoria) {
        return Produto.criar(nome, "descricao", null, categoria, MOMENTO);
    }

    /** Variacao com id, cadastrada em um produto novo da categoria dada. */
    public static VariacaoProduto variacao(long id, String sku, String preco) {
        Produto produto = produto("Produto " + sku, categoria(1000 + id, "Cat " + sku, null));
        return variacaoDe(produto, id, sku, preco);
    }

    public static VariacaoProduto variacaoDe(Produto produto, long id, String sku, String preco) {
        return comId(produto.adicionarVariacao(sku, Dinheiro.de(preco), Map.of("tamanho", "M"), MOMENTO), id);
    }

    public static Combo combo(long id, String sku, String preco) {
        return comId(new Combo(sku, "Combo " + sku, Dinheiro.de(preco)), id);
    }
}
