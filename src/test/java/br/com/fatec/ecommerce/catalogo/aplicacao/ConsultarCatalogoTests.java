package br.com.fatec.ecommerce.catalogo.aplicacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import br.com.fatec.ecommerce.TesteDeIntegracao;
import br.com.fatec.ecommerce.catalogo.CatalogoPersistidoParaTeste;
import br.com.fatec.ecommerce.catalogo.dominio.Categoria;
import br.com.fatec.ecommerce.catalogo.dominio.Combo;
import br.com.fatec.ecommerce.catalogo.dominio.Produto;
import br.com.fatec.ecommerce.catalogo.dominio.VariacaoProduto;
import br.com.fatec.ecommerce.compartilhado.dominio.DadoInvalidoException;
import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import br.com.fatec.ecommerce.compartilhado.dominio.Pagina;
import br.com.fatec.ecommerce.compartilhado.dominio.RecursoNaoEncontradoException;
import br.com.fatec.ecommerce.estoque.EstoqueParaTeste;
import br.com.fatec.ecommerce.seguranca.UsuariosParaTeste;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Consulta publica do catalogo: roda sem usuario autenticado (nenhum teste desta classe tem token). A inativacao
 * dos dados de apoio e feita direto no banco, para nao exigir perfil.
 */
@TesteDeIntegracao
class ConsultarCatalogoTests {

    @Autowired
    ConsultarCatalogo consultar;
    @Autowired
    CatalogoPersistidoParaTeste catalogo;
    @Autowired
    EstoqueParaTeste estoque;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    UsuariosParaTeste helper;

    private Categoria roupas;
    private Categoria camisetas;
    private Categoria casa;

    @BeforeEach
    void preparar() {
        helper.limpar();
        roupas = catalogo.categoria("Roupas", null);
        camisetas = catalogo.categoria("Camisetas", roupas);
        casa = catalogo.categoria("Casa", null);
    }

    private static FiltroDeProdutos filtro(Long categoriaId, String nome, String min, String max) {
        return new FiltroDeProdutos(categoriaId, nome, min == null ? null : Dinheiro.de(min),
                max == null ? null : Dinheiro.de(max));
    }

    private static FiltroDeProdutos categoria(Long categoriaId) {
        return filtro(categoriaId, null, null, null);
    }

    @Test
    void arvoreDeCategoriasOrdenadaPeloNomeEmTodosOsNiveis() {
        catalogo.categoria("Regatas", roupas);
        catalogo.categoria("Cozinha", casa);

        assertThat(consultar.arvoreDeCategorias()).extracting(NoDeCategoria::nome).containsExactly("Casa", "Roupas");
        NoDeCategoria noRoupas = consultar.arvoreDeCategorias().get(1);
        assertThat(noRoupas.filhas()).extracting(NoDeCategoria::nome).containsExactly("Camisetas", "Regatas");
        assertThat(noRoupas.filhas().get(0).filhas()).isEmpty();
    }

    @Test
    void filtroPorCategoriaIncluiSubcategoriasSemToken() {
        Produto camiseta = catalogo.produto("Camiseta Básica", camisetas);
        catalogo.variacaoDe(camiseta, "CAM-M", "59.90");
        catalogo.variacao(roupas, "CASACO", "199.90");
        catalogo.variacao(casa, "CANECA", "29.90");

        Pagina<ProdutoResumo> roupasEncontradas = consultar.produtos(categoria(roupas.getId()), 0, 20);
        Pagina<ProdutoResumo> soCamisetas = consultar.produtos(categoria(camisetas.getId()), 0, 20);

        assertThat(roupasEncontradas.conteudo()).extracting(ProdutoResumo::nome)
                .containsExactly("Camiseta Básica", "Produto CASACO");
        assertThat(roupasEncontradas.totalElementos()).isEqualTo(2);
        assertThat(soCamisetas.conteudo()).extracting(ProdutoResumo::nome).containsExactly("Camiseta Básica");
        assertThat(consultar.produtos(FiltroDeProdutos.semFiltro(), 0, 20).totalElementos()).isEqualTo(3);
    }

    @Test
    void filtroSemResultadoDevolvePaginaVazia() {
        catalogo.variacao(camisetas, "CAM-M", "59.90");

        Pagina<ProdutoResumo> pagina = consultar.produtos(filtro(null, null, "10000", "20000"), 0, 20);

        assertThat(pagina.conteudo()).isEmpty();
        assertThat(pagina.totalElementos()).isZero();
        assertThat(consultar.produtos(categoria(9999L), 0, 20).conteudo()).isEmpty();
    }

    @Test
    void filtroPorNomeNaoDiferenciaCaixaETrataCuringasComoTexto() {
        catalogo.variacaoDe(catalogo.produto("Camiseta Básica", camisetas), "CAM-1", "59.90");
        catalogo.variacaoDe(catalogo.produto("Caneca 100% Cerâmica", casa), "CAN-1", "29.90");

        assertThat(consultar.produtos(filtro(null, "CAMISETA", null, null), 0, 20).conteudo())
                .extracting(ProdutoResumo::nome).containsExactly("Camiseta Básica");
        assertThat(consultar.produtos(filtro(null, "100%", null, null), 0, 20).conteudo())
                .extracting(ProdutoResumo::nome).containsExactly("Caneca 100% Cerâmica");
        assertThat(consultar.produtos(filtro(null, "a_a", null, null), 0, 20).conteudo()).isEmpty();
    }

    @Test
    void filtroPorFaixaDePrecoUsaAsVariacoesAtivas() {
        Produto camiseta = catalogo.produto("Camiseta", camisetas);
        catalogo.variacaoDe(camiseta, "CAM-P", "50.00");
        VariacaoProduto cara = catalogo.variacaoDe(camiseta, "CAM-G", "500.00");
        catalogo.variacao(casa, "CANECA", "30.00");

        assertThat(consultar.produtos(filtro(null, null, "400", "600"), 0, 20).conteudo())
                .extracting(ProdutoResumo::nome).containsExactly("Camiseta");
        assertThat(consultar.produtos(filtro(null, null, "50.00", "50.00"), 0, 20).conteudo())
                .extracting(ProdutoResumo::nome).containsExactly("Camiseta");
        assertThat(consultar.produtos(filtro(null, null, null, "40"), 0, 20).conteudo())
                .extracting(ProdutoResumo::nome).containsExactly("Produto CANECA");

        inativarVariacao(cara.getId());

        assertThat(consultar.produtos(filtro(null, null, "400", "600"), 0, 20).conteudo()).isEmpty();
        ProdutoResumo resumo = consultar.produtos(categoria(camisetas.getId()), 0, 20).conteudo().get(0);
        assertThat(resumo.precoMinimo()).isEqualTo(Dinheiro.de("50.00"));
        assertThat(resumo.precoMaximo()).isEqualTo(Dinheiro.de("50.00"));
    }

    @Test
    void produtosInativosOuSemVariacaoAtivaNaoSaoListados() {
        Produto inativo = catalogo.produto("Inativo", camisetas);
        catalogo.variacaoDe(inativo, "INA-1", "10.00");
        Produto semVariacao = catalogo.produto("Sem variação", camisetas);
        Produto semVariacaoAtiva = catalogo.produto("Sem variação ativa", camisetas);
        VariacaoProduto unica = catalogo.variacaoDe(semVariacaoAtiva, "SVA-1", "10.00");
        catalogo.variacao(camisetas, "OK-1", "10.00");
        inativarProduto(inativo.getId());
        inativarVariacao(unica.getId());

        assertThat(consultar.produtos(FiltroDeProdutos.semFiltro(), 0, 20).conteudo())
                .extracting(ProdutoResumo::nome).containsExactly("Produto OK-1");
        assertThat(semVariacao.getId()).isNotNull();
    }

    @Test
    void paginacaoOrdenadaPeloNomeComTotal() {
        catalogo.variacao(casa, "C", "10.00");
        catalogo.variacao(casa, "A", "10.00");
        catalogo.variacao(casa, "B", "10.00");

        Pagina<ProdutoResumo> primeira = consultar.produtos(FiltroDeProdutos.semFiltro(), 0, 2);
        Pagina<ProdutoResumo> segunda = consultar.produtos(FiltroDeProdutos.semFiltro(), 1, 2);

        assertThat(primeira.conteudo()).extracting(ProdutoResumo::nome).containsExactly("Produto A", "Produto B");
        assertThat(segunda.conteudo()).extracting(ProdutoResumo::nome).containsExactly("Produto C");
        assertThat(primeira.totalElementos()).isEqualTo(3);
        assertThat(primeira.totalPaginas()).isEqualTo(2);
        assertThatThrownBy(() -> consultar.produtos(FiltroDeProdutos.semFiltro(), -1, 2))
                .isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> consultar.produtos(FiltroDeProdutos.semFiltro(), 0, 0))
                .isInstanceOf(DadoInvalidoException.class);
        assertThat(consultar.produtos(FiltroDeProdutos.semFiltro(), 0, 1000).tamanho())
                .isEqualTo(ConsultarCatalogo.TAMANHO_MAXIMO);
    }

    @Test
    void detalheMostraAsVariacoesAtivasComADisponibilidadeDoEstoque() {
        Produto camiseta = catalogo.produto("Camiseta Básica", camisetas);
        VariacaoProduto m = catalogo.variacaoDe(camiseta, "CAM-M", "59.90");
        VariacaoProduto g = catalogo.variacaoDe(camiseta, "CAM-G", "59.90");
        VariacaoProduto gg = catalogo.variacaoDe(camiseta, "CAM-GG", "59.90");
        estoque.definirSaldo(m.getId(), 3, 1);
        jdbc.update("DELETE FROM saldo_estoque WHERE variacao_id = ?", g.getId());
        inativarVariacao(gg.getId());

        ProdutoDetalhe detalhe = consultar.detalheProduto(camiseta.getId());

        assertThat(detalhe.nome()).isEqualTo("Camiseta Básica");
        assertThat(detalhe.categoriaId()).isEqualTo(camisetas.getId());
        assertThat(detalhe.variacoes()).extracting(ProdutoDetalhe.Variacao::sku, ProdutoDetalhe.Variacao::disponivel)
                .containsExactly(tuple("CAM-M", 3),
                        tuple("CAM-G", 0));
        assertThat(detalhe.variacoes().get(0).preco()).isEqualTo(Dinheiro.de("59.90"));
        assertThat(detalhe.variacoes().get(0).atributos()).containsEntry("tamanho", "CAM-M");
    }

    @Test
    void produtoInativoOuInexistenteOuSemVariacaoAtivaEhNaoEncontrado() {
        Produto inativo = catalogo.produto("Inativo", camisetas);
        catalogo.variacaoDe(inativo, "INA-1", "10.00");
        inativarProduto(inativo.getId());
        Produto semVariacao = catalogo.produto("Sem variação", camisetas);

        assertThatThrownBy(() -> consultar.detalheProduto(inativo.getId()))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> consultar.detalheProduto(9999L)).isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> consultar.detalheProduto(semVariacao.getId()))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void disponibilidadeDoComboEhOMinimoDoQuocienteDeCadaComponente() {
        VariacaoProduto caneca = catalogo.variacao(casa, "CANECA", "29.90");
        VariacaoProduto cafe = catalogo.variacao(casa, "CAFE", "39.90");
        Combo kit = catalogo.combo("KIT-CAFE", "79.90", caneca, 1, cafe, 2);
        estoque.definirSaldo(cafe.getId(), 5, 0);
        estoque.definirSaldo(caneca.getId(), 10, 0);

        ComboDetalhe detalhe = consultar.detalheCombo(kit.getId());

        assertThat(detalhe.disponibilidade()).isEqualTo(2);
        assertThat(detalhe.sku()).isEqualTo("KIT-CAFE");
        assertThat(detalhe.preco()).isEqualTo(Dinheiro.de("79.90"));
        assertThat(detalhe.componentes()).extracting(ComboDetalhe.Componente::sku, ComboDetalhe.Componente::quantidade)
                .containsExactly(tuple("CANECA", 1),
                        tuple("CAFE", 2));
        assertThat(consultar.combos()).extracting(ComboDetalhe::disponibilidade).containsExactly(2);
    }

    @Test
    void disponibilidadeDoComboDentroDeComboUsaOsComponentesAgregados() {
        VariacaoProduto caneca = catalogo.variacao(casa, "CANECA", "29.90");
        VariacaoProduto cafe = catalogo.variacao(casa, "CAFE", "39.90");
        Combo kit = catalogo.combo("KIT-CAFE", "79.90", caneca, 1, cafe, 2);
        Combo maior = catalogo.combo("KIT-MAIOR", "129.90", kit, 2, caneca, 1);
        estoque.definirSaldo(cafe.getId(), 9, 0);   // maior usa 4 cafes: 9 / 4 = 2
        estoque.definirSaldo(caneca.getId(), 30, 0); // maior usa 3 canecas: 30 / 3 = 10

        assertThat(consultar.detalheCombo(maior.getId()).disponibilidade()).isEqualTo(2);
        assertThat(consultar.detalheCombo(kit.getId()).disponibilidade()).isEqualTo(4);
    }

    @Test
    void comboSemSaldoTemDisponibilidadeZeroMasContinuaListado() {
        VariacaoProduto caneca = catalogo.variacao(casa, "CANECA", "29.90");
        Combo kit = catalogo.combo("KIT-1", "20.00", caneca, 1);
        jdbc.update("DELETE FROM saldo_estoque WHERE variacao_id = ?", caneca.getId());

        assertThat(consultar.detalheCombo(kit.getId()).disponibilidade()).isZero();
        assertThat(consultar.combos()).hasSize(1);
    }

    @Test
    void combosInativosOuComComponenteInativoNaoSaoExpostos() {
        VariacaoProduto caneca = catalogo.variacao(casa, "CANECA", "29.90");
        VariacaoProduto cafe = catalogo.variacao(casa, "CAFE", "39.90");
        Combo semCafe = catalogo.combo("KIT-CAFE", "79.90", caneca, 1, cafe, 2);
        Combo inativo = catalogo.combo("KIT-INATIVO", "10.00", caneca, 1);
        Combo ok = catalogo.combo("KIT-OK", "10.00", caneca, 2);
        inativarCombo(inativo.getId());
        inativarVariacao(cafe.getId());

        assertThat(consultar.combos()).extracting(ComboDetalhe::sku).containsExactly("KIT-OK");
        assertThatThrownBy(() -> consultar.detalheCombo(inativo.getId()))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> consultar.detalheCombo(semCafe.getId()))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> consultar.detalheCombo(9999L)).isInstanceOf(RecursoNaoEncontradoException.class);
        assertThat(consultar.detalheCombo(ok.getId()).sku()).isEqualTo("KIT-OK");
    }

    private void inativarProduto(Long produtoId) {
        jdbc.update("UPDATE produto SET ativo = FALSE WHERE id = ?", produtoId);
        jdbc.update("UPDATE item_vendavel SET ativo = FALSE WHERE id IN "
                + "(SELECT id FROM variacao_produto WHERE produto_id = ?)", produtoId);
    }

    private void inativarVariacao(Long variacaoId) {
        jdbc.update("UPDATE item_vendavel SET ativo = FALSE WHERE id = ?", variacaoId);
    }

    private void inativarCombo(Long comboId) {
        jdbc.update("UPDATE item_vendavel SET ativo = FALSE WHERE id = ?", comboId);
    }
}
