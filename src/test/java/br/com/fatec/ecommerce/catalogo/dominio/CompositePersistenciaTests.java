package br.com.fatec.ecommerce.catalogo.dominio;

import static br.com.fatec.ecommerce.catalogo.CatalogoParaTeste.MOMENTO;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.fatec.ecommerce.TesteDeIntegracao;
import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import br.com.fatec.ecommerce.seguranca.UsuariosParaTeste;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/** Valida o mapeamento JOINED do Composite contra o esquema da V3 (ddl-auto=validate) e o recarregamento. */
@TesteDeIntegracao
class CompositePersistenciaTests {

    @Autowired
    CategoriaRepository categorias;
    @Autowired
    ProdutoRepository produtos;
    @Autowired
    ItemVendavelRepository itens;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    TransactionTemplate transacao;
    @Autowired
    UsuariosParaTeste helper;

    @BeforeEach
    void limpar() {
        helper.limpar();
    }

    @Test
    void comboComVariacoesEComboInternoEhGravadoERecarregadoComOsComponentesCorretos() {
        Categoria cozinha = categorias.salvar(Categoria.criar("Cozinha", null, new ArvoreDeCategorias(List.of())));
        Produto produto = Produto.criar("Cozinha", "Itens de cozinha", "Marca", cozinha, MOMENTO);
        produto.adicionarVariacao("CANECA", Dinheiro.de("25.00"), Map.of("volume", "300 ml"), MOMENTO);
        produto.adicionarVariacao("CAFE", Dinheiro.de("30.00"), Map.of("peso", "250 g", "torra", "media"), MOMENTO);
        Produto salvo = produtos.salvar(produto);
        Long canecaId = itens.buscarPorSku("CANECA").orElseThrow().getId();
        Long cafeId = itens.buscarPorSku("CAFE").orElseThrow().getId();
        assertThat(salvo.getVariacoes()).hasSize(2);

        Combo kitCafe = new Combo("KIT-CAFE", "Kit Café", Dinheiro.de("79.90"));
        kitCafe.adicionarItem(itens.buscarPorSku("CANECA").orElseThrow(), 1);
        kitCafe.adicionarItem(itens.buscarPorSku("CAFE").orElseThrow(), 2);
        kitCafe = itens.salvar(kitCafe);

        Combo presente = new Combo("PRESENTE", "Presente", Dinheiro.de("150.00"));
        presente.adicionarItem(kitCafe, 3);
        presente.adicionarItem(itens.buscarPorSku("CAFE").orElseThrow(), 1);
        Long presenteId = itens.salvar(presente).getId();

        ItemVendavel recarregado = itens.buscarPorId(presenteId).orElseThrow();
        assertThat(recarregado).isInstanceOf(Combo.class);
        assertThat(recarregado.preco()).isEqualTo(Dinheiro.de("150.00"));
        assertThat(recarregado.estaDisponivelParaVenda()).isTrue();
        assertThat(recarregado.componentesDeEstoque(2)).containsExactlyInAnyOrder(
                new ComponenteEstoque(canecaId, 6),
                new ComponenteEstoque(cafeId, 14));

        ItemVendavel variacao = itens.buscarPorSku("CAFE").orElseThrow();
        assertThat(variacao).isInstanceOf(VariacaoProduto.class);
        assertThat(((VariacaoProduto) variacao).getAtributos()).containsEntry("peso", "250 g")
                .containsEntry("torra", "media");
        assertThat(variacao.componentesDeEstoque(4)).containsExactly(new ComponenteEstoque(cafeId, 4));

        assertThat(jdbc.queryForList("SELECT tipo FROM item_vendavel ORDER BY sku", String.class))
                .containsExactly("VARIACAO", "VARIACAO", "COMBO", "COMBO");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM combo_item", Integer.class)).isEqualTo(4);
    }

    @Test
    void inativarOProdutoPersisteAInativacaoDasVariacoesETornaOComboIndisponivel() {
        Categoria cat = categorias.salvar(Categoria.criar("Cat", null, new ArvoreDeCategorias(List.of())));
        Produto produto = Produto.criar("P", null, null, cat, MOMENTO);
        produto.adicionarVariacao("V1", Dinheiro.de("10.00"), Map.of(), MOMENTO);
        produtos.salvar(produto);
        Combo combo = new Combo("C1", "Combo", Dinheiro.de("9.00"));
        combo.adicionarItem(itens.buscarPorSku("V1").orElseThrow(), 1);
        Long comboId = itens.salvar(combo).getId();

        // As variacoes do produto sao carregadas sob demanda: o caso de uso real roda em transacao.
        transacao.executeWithoutResult(status -> {
            Produto carregado = produtos.buscarPorId(produto.getId()).orElseThrow();
            carregado.inativar();
            produtos.salvar(carregado);
        });

        assertThat(jdbc.queryForObject("SELECT ativo FROM item_vendavel WHERE sku = 'V1'", Boolean.class)).isFalse();
        assertThat(itens.buscarPorId(comboId).orElseThrow().estaDisponivelParaVenda()).isFalse();
    }
}
