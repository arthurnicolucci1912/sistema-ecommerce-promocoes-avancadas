package br.com.fatec.ecommerce.catalogo.aplicacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fatec.ecommerce.TesteDeIntegracao;
import br.com.fatec.ecommerce.catalogo.CatalogoPersistidoParaTeste;
import br.com.fatec.ecommerce.catalogo.dominio.Categoria;
import br.com.fatec.ecommerce.catalogo.dominio.CategoriaRepository;
import br.com.fatec.ecommerce.catalogo.dominio.Combo;
import br.com.fatec.ecommerce.catalogo.dominio.ItemVendavelRepository;
import br.com.fatec.ecommerce.catalogo.dominio.ProdutoRepository;
import br.com.fatec.ecommerce.compartilhado.dominio.ConflitoException;
import br.com.fatec.ecommerce.compartilhado.dominio.DadoInvalidoException;
import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import br.com.fatec.ecommerce.compartilhado.dominio.RecursoNaoEncontradoException;
import br.com.fatec.ecommerce.estoque.EstoqueParaTeste;
import br.com.fatec.ecommerce.seguranca.UsuariosParaTeste;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;

/** Casos de borda dos casos de uso de administracao (categorias, produtos e combos) com banco real. */
@TesteDeIntegracao
@WithMockUser(roles = "OPERADOR_COMERCIAL")
class GerenciarCatalogoBordasTests {

    @Autowired
    GerenciarCategorias categoriasUc;
    @Autowired
    GerenciarProdutos produtosUc;
    @Autowired
    GerenciarCombos combosUc;
    @Autowired
    CategoriaRepository categorias;
    @Autowired
    ProdutoRepository produtos;
    @Autowired
    ItemVendavelRepository itens;
    @Autowired
    CatalogoPersistidoParaTeste catalogo;
    @Autowired
    EstoqueParaTeste estoque;
    @Autowired
    ConsultarCatalogo consultar;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    UsuariosParaTeste helper;

    @BeforeEach
    void preparar() {
        helper.limpar();
    }

    private static NovaVariacao variacao(String sku, String preco) {
        return new NovaVariacao(sku, Dinheiro.de(preco), Map.of("tamanho", sku));
    }

    private int contar(String tabela) {
        return jdbc.queryForObject("SELECT count(*) FROM " + tabela, Integer.class);
    }

    // --- Categorias ---

    @Test
    void renomearEMoverNaMesmaOperacaoValidaONomeContraOPaiDeDestino() {
        Categoria roupas = categoriasUc.criar("Roupas", null);
        categoriasUc.criar("A", roupas.getId());
        Categoria b = categoriasUc.criar("B", roupas.getId());

        Categoria alterada = categoriasUc.alterar(b.getId(), "A", null);

        assertThat(alterada.getNome()).isEqualTo("A");
        assertThat(alterada.getPaiId()).isNull();
    }

    @Test
    void alterarRenomeiaEMoveNaMesmaTransacaoQuandoNaoHaConflito() {
        Categoria roupas = categoriasUc.criar("Roupas", null);
        Categoria casa = categoriasUc.criar("Casa", null);
        Categoria camisetas = categoriasUc.criar("Camisetas", roupas.getId());

        Categoria alterada = categoriasUc.alterar(camisetas.getId(), "Utensilios", casa.getId());

        assertThat(alterada.getNome()).isEqualTo("Utensilios");
        assertThat(alterada.getPaiId()).isEqualTo(casa.getId());
    }

    @Test
    void alterarComMovimentoInvalidoNaoGravaOnomeNovo() {
        Categoria roupas = categoriasUc.criar("Roupas", null);
        Categoria camisetas = categoriasUc.criar("Camisetas", roupas.getId());

        assertThatThrownBy(() -> categoriasUc.alterar(roupas.getId(), "Vestuario", camisetas.getId()))
                .isInstanceOf(ConflitoException.class);

        assertThat(categorias.buscarPorId(roupas.getId()).orElseThrow().getNome()).isEqualTo("Roupas");
        assertThat(categorias.buscarPorId(roupas.getId()).orElseThrow().getPaiId()).isNull();
    }

    @Test
    void alterarComPaiInexistenteEDadoInvalidoENaoRenomeia() {
        Categoria roupas = categoriasUc.criar("Roupas", null);

        assertThatThrownBy(() -> categoriasUc.alterar(roupas.getId(), "Novo", 999999L))
                .isInstanceOf(DadoInvalidoException.class);

        assertThat(categorias.buscarPorId(roupas.getId()).orElseThrow().getNome()).isEqualTo("Roupas");
    }

    @Test
    void criarComPaiInexistenteNaoGravaNada() {
        assertThatThrownBy(() -> categoriasUc.criar("Filha", 999999L)).isInstanceOf(DadoInvalidoException.class);

        assertThat(contar("categoria")).isZero();
    }

    @Test
    void categoriaInexistenteEhNaoEncontradaEmTodasAsOperacoesDeAlteracao() {
        assertThatThrownBy(() -> categoriasUc.renomear(999999L, "X")).isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> categoriasUc.mover(999999L, null)).isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> categoriasUc.alterar(999999L, "X", null))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> categoriasUc.remover(999999L)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void arvoreProfundaDeDezNiveisPersisteEAncestraisSaoConsultaveis() {
        Long paiId = null;
        for (int i = 1; i <= 10; i++) {
            paiId = categoriasUc.criar("Nivel " + i, paiId).getId();
        }
        var variacao = catalogo.variacao(categorias.buscarPorId(paiId).orElseThrow(), "FUNDO", "1.00");

        var info = new ConsultarItensVendaveis(itens, categorias).porSku("FUNDO");

        assertThat(info.categoriaIds()).hasSize(10);
        assertThat(variacao.getSku()).isEqualTo("FUNDO");
    }

    @Test
    void removerFolhaAposRemoverSubcategoriasEmCascataManualFunciona() {
        Categoria raiz = categoriasUc.criar("Raiz", null);
        Categoria filha = categoriasUc.criar("Filha", raiz.getId());

        assertThatThrownBy(() -> categoriasUc.remover(raiz.getId())).isInstanceOf(ConflitoException.class);
        categoriasUc.remover(filha.getId());
        categoriasUc.remover(raiz.getId());

        assertThat(contar("categoria")).isZero();
    }

    @Test
    void moverRaizParaOutraRaizDeMesmoNomeComCaixaDiferenteEhConflito() {
        Categoria a = categoriasUc.criar("Casa", null);
        Categoria b = categoriasUc.criar("Lar", null);
        Categoria filhaDeB = categoriasUc.criar("CASA", b.getId());

        assertThatThrownBy(() -> categoriasUc.mover(filhaDeB.getId(), null)).isInstanceOf(ConflitoException.class);
        assertThat(a.getNome()).isEqualTo("Casa");
    }

    // --- Produtos ---

    @Test
    void cadastrarProdutoSemVariacoesOuComListaNulaEDadoInvalido() {
        Categoria cat = catalogo.categoria("Casa", null);

        assertThatThrownBy(() -> produtosUc.cadastrar("P", null, null, cat.getId(), List.of()))
                .isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> produtosUc.cadastrar("P", null, null, cat.getId(), null))
                .isInstanceOf(DadoInvalidoException.class);

        assertThat(contar("produto")).isZero();
    }

    @Test
    void cadastrarProdutoComCategoriaNulaEDadoInvalido() {
        assertThatThrownBy(() -> produtosUc.cadastrar("P", null, null, null, List.of(variacao("S", "1"))))
                .isInstanceOf(DadoInvalidoException.class);
        assertThat(contar("produto")).isZero();
    }

    @Test
    void segundaVariacaoInvalidaDesfazOProdutoEAPrimeiraVariacao() {
        Categoria cat = catalogo.categoria("Casa", null);

        assertThatThrownBy(() -> produtosUc.cadastrar("P", null, null, cat.getId(),
                List.of(variacao("OK", "10"), variacao("RUIM", "0"))))
                .isInstanceOf(DadoInvalidoException.class);

        assertThat(contar("produto")).isZero();
        assertThat(contar("variacao_produto")).isZero();
        assertThat(itens.existePorSku("OK")).isFalse();
    }

    @Test
    void segundaVariacaoComSkuDeComboExistenteDesfazTudo() {
        Categoria cat = catalogo.categoria("Casa", null);
        var base = catalogo.variacao(cat, "BASE", "5.00");
        catalogo.combo("COMBO-X", "9.00", base, 1);

        assertThatThrownBy(() -> produtosUc.cadastrar("P", null, null, cat.getId(),
                List.of(variacao("NOVA", "10"), variacao("COMBO-X", "10"))))
                .isInstanceOf(ConflitoException.class);

        assertThat(itens.existePorSku("NOVA")).isFalse();
        assertThat(contar("produto")).isEqualTo(1);
    }

    @Test
    void cadastrarCriaSaldoZeroParaCadaVariacaoPeloObservador() {
        Categoria cat = catalogo.categoria("Casa", null);

        var produto = produtosUc.cadastrar("P", null, null, cat.getId(),
                List.of(variacao("V1", "1"), variacao("V2", "2"), variacao("V3", "3")));

        assertThat(produto.variacoes()).hasSize(3).allSatisfy(v -> {
            assertThat(estoque.existeSaldo(v.id())).isTrue();
            assertThat(estoque.disponivel(v.id())).isZero();
        });
    }

    @Test
    void alterarDadosDeProdutoInexistenteEhNaoEncontrado() {
        Categoria cat = catalogo.categoria("Casa", null);

        assertThatThrownBy(() -> produtosUc.alterarDados(999999L, "X", null, null, cat.getId()))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void alterarDadosLimpaDescricaoEMarcaQuandoVazias() {
        Categoria cat = catalogo.categoria("Casa", null);
        var produto = produtosUc.cadastrar("P", "descricao", "Marca", cat.getId(), List.of(variacao("S", "1")));

        var alterado = produtosUc.alterarDados(produto.id(), "P2", " ", "", cat.getId());

        assertThat(alterado.descricao()).isNull();
        assertThat(alterado.marca()).isNull();
        assertThat(alterado.nome()).isEqualTo("P2");
    }

    @Test
    void alterarPrecoDeVariacaoInexistenteEhNaoEncontradoEPrecoNaoPositivoEhInvalido() {
        Categoria cat = catalogo.categoria("Casa", null);
        var produto = produtosUc.cadastrar("P", null, null, cat.getId(), List.of(variacao("S", "10")));
        Long variacaoId = produto.variacoes().get(0).id();

        assertThatThrownBy(() -> produtosUc.alterarPrecoDaVariacao(999999L, Dinheiro.de("1")))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> produtosUc.alterarPrecoDaVariacao(variacaoId, Dinheiro.zero()))
                .isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> produtosUc.alterarPrecoDaVariacao(variacaoId, null))
                .isInstanceOf(DadoInvalidoException.class);

        assertThat(itens.buscarPorSku("S").orElseThrow().preco()).isEqualTo(Dinheiro.de("10.00"));
    }

    @Test
    void alterarPrecoDeVariacaoNaoMudaOPrecoDoComboQueAContem() {
        Categoria cat = catalogo.categoria("Casa", null);
        var caneca = catalogo.variacao(cat, "CANECA", "25.00");
        catalogo.combo("KIT", "40.00", caneca, 2);

        produtosUc.alterarPrecoDaVariacao(caneca.getId(), Dinheiro.de("99.99"));

        assertThat(itens.buscarPorSku("KIT").orElseThrow().preco()).isEqualTo(Dinheiro.de("40.00"));
        assertThat(itens.buscarPorSku("CANECA").orElseThrow().preco()).isEqualTo(Dinheiro.de("99.99"));
    }

    @Test
    void adicionarVariacaoEmProdutoInexistenteEhNaoEncontradoESkuDuplicadoEhConflito() {
        Categoria cat = catalogo.categoria("Casa", null);
        var produto = produtosUc.cadastrar("P", null, null, cat.getId(), List.of(variacao("S", "10")));

        assertThatThrownBy(() -> produtosUc.adicionarVariacao(999999L, variacao("Z", "1")))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> produtosUc.adicionarVariacao(produto.id(), variacao("S", "1")))
                .isInstanceOf(ConflitoException.class);
        assertThatThrownBy(() -> produtosUc.adicionarVariacao(produto.id(), variacao(" S ", "1")))
                .isInstanceOf(ConflitoException.class);

        assertThat(contar("variacao_produto")).isEqualTo(1);
    }

    @Test
    void inativarVariacaoDeOutroProdutoNaoAfetaOsDemais() {
        Categoria cat = catalogo.categoria("Casa", null);
        var p1 = produtosUc.cadastrar("P1", null, null, cat.getId(), List.of(variacao("A", "1")));
        var p2 = produtosUc.cadastrar("P2", null, null, cat.getId(), List.of(variacao("B", "1")));

        produtosUc.inativarVariacao(p1.variacoes().get(0).id());

        assertThat(itens.buscarPorSku("A").orElseThrow().isAtivo()).isFalse();
        assertThat(itens.buscarPorSku("B").orElseThrow().isAtivo()).isTrue();
        assertThat(produtos.buscarPorId(p2.id()).orElseThrow().isAtivo()).isTrue();
    }

    @Test
    void produtoInativadoSomeDaListagemPublicaMasAindaEhGravado() {
        Categoria cat = catalogo.categoria("Casa", null);
        var produto = produtosUc.cadastrar("P", null, null, cat.getId(), List.of(variacao("S", "10")));

        produtosUc.inativarProduto(produto.id());

        assertThat(consultar.produtos(FiltroDeProdutos.semFiltro(), 0, 20).conteudo()).isEmpty();
        assertThat(produtos.buscarPorId(produto.id())).isPresent();
        assertThat(itens.buscarPorSku("S")).isPresent();
    }

    // --- Combos ---

    @Test
    void cadastrarComboComSkuComEspacosEAparadoEConflitaComExistente() {
        Categoria cat = catalogo.categoria("Casa", null);
        catalogo.variacao(cat, "CANECA", "25.00");
        Combo kit = combosUc.cadastrar("  KIT  ", "Kit", Dinheiro.de("10"), List.of(new ComponenteDeCombo("CANECA", 1)));

        assertThat(kit.getSku()).isEqualTo("KIT");
        assertThatThrownBy(() -> combosUc.cadastrar(" KIT ", "Outro", Dinheiro.de("10"),
                List.of(new ComponenteDeCombo("CANECA", 1)))).isInstanceOf(ConflitoException.class);
    }

    @Test
    void cadastrarComboComComponentesNulosOuVaziosOuComElementoNuloEDadoInvalido() {
        Categoria cat = catalogo.categoria("Casa", null);
        catalogo.variacao(cat, "CANECA", "25.00");

        assertThatThrownBy(() -> combosUc.cadastrar("K", "K", Dinheiro.de("1"), null))
                .isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> combosUc.cadastrar("K", "K", Dinheiro.de("1"), List.of()))
                .isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> combosUc.cadastrar("K", "K", Dinheiro.de("1"),
                java.util.Arrays.asList(new ComponenteDeCombo("CANECA", 1), null)))
                .isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> combosUc.cadastrar("K", "K", Dinheiro.de("1"),
                List.of(new ComponenteDeCombo("CANECA", 0)))).isInstanceOf(DadoInvalidoException.class);

        assertThat(contar("combo")).isZero();
        assertThat(itens.existePorSku("K")).isFalse();
    }

    @Test
    void comboComSegundoComponenteInexistenteNaoGravaOCombo() {
        Categoria cat = catalogo.categoria("Casa", null);
        catalogo.variacao(cat, "CANECA", "25.00");

        assertThatThrownBy(() -> combosUc.cadastrar("K", "K", Dinheiro.de("1"),
                List.of(new ComponenteDeCombo("CANECA", 1), new ComponenteDeCombo("FANTASMA", 1))))
                .isInstanceOf(DadoInvalidoException.class);

        assertThat(itens.existePorSku("K")).isFalse();
        assertThat(contar("combo_item")).isZero();
    }

    @Test
    void trocarComponentesPorListaInvalidaMantemOsComponentesAnteriores() {
        Categoria cat = catalogo.categoria("Casa", null);
        catalogo.variacao(cat, "CANECA", "25.00");
        catalogo.variacao(cat, "CAFE", "30.00");
        Combo kit = combosUc.cadastrar("KIT", "Kit", Dinheiro.de("10"), List.of(new ComponenteDeCombo("CANECA", 2)));

        assertThatThrownBy(() -> combosUc.substituirComponentes(kit.getId(),
                List.of(new ComponenteDeCombo("CAFE", 1), new ComponenteDeCombo("FANTASMA", 1))))
                .isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> combosUc.substituirComponentes(kit.getId(), List.of()))
                .isInstanceOf(DadoInvalidoException.class);

        Combo recarregado = itens.buscarComboPorId(kit.getId()).orElseThrow();
        assertThat(recarregado.getItens()).extracting(i -> i.getItem().getSku()).containsExactly("CANECA");
        assertThat(recarregado.getItens().get(0).getQuantidade()).isEqualTo(2);
    }

    @Test
    void alterarComboSemPrecoNemComponentesEDadoInvalido() {
        Categoria cat = catalogo.categoria("Casa", null);
        catalogo.variacao(cat, "CANECA", "25.00");
        Combo kit = combosUc.cadastrar("KIT", "Kit", Dinheiro.de("10"), List.of(new ComponenteDeCombo("CANECA", 1)));

        assertThatThrownBy(() -> combosUc.alterar(kit.getId(), null, null)).isInstanceOf(DadoInvalidoException.class);
    }

    @Test
    void alterarPrecoDoComboComValorNaoPositivoEDadoInvalidoEMantemOPreco() {
        Categoria cat = catalogo.categoria("Casa", null);
        catalogo.variacao(cat, "CANECA", "25.00");
        Combo kit = combosUc.cadastrar("KIT", "Kit", Dinheiro.de("10"), List.of(new ComponenteDeCombo("CANECA", 1)));

        assertThatThrownBy(() -> combosUc.alterarPreco(kit.getId(), Dinheiro.zero()))
                .isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> combosUc.alterarPreco(kit.getId(), Dinheiro.de("-1")))
                .isInstanceOf(DadoInvalidoException.class);

        assertThat(itens.buscarComboPorId(kit.getId()).orElseThrow().preco()).isEqualTo(Dinheiro.de("10.00"));
    }

    @Test
    void comboPodeConterComboInativoEFicaIndisponivel() {
        Categoria cat = catalogo.categoria("Casa", null);
        var caneca = catalogo.variacao(cat, "CANECA", "25.00");
        Combo interno = catalogo.combo("INTERNO", "10.00", caneca, 1);
        combosUc.inativar(interno.getId());

        Combo externo = combosUc.cadastrar("EXTERNO", "Externo", Dinheiro.de("20"),
                List.of(new ComponenteDeCombo("INTERNO", 1)));

        assertThat(externo.estaDisponivelParaVenda()).isFalse();
        assertThat(consultar.combos()).isEmpty();
    }

    @Test
    void inativarComboDuasVezesEhIdempotente() {
        Categoria cat = catalogo.categoria("Casa", null);
        var caneca = catalogo.variacao(cat, "CANECA", "25.00");
        Combo kit = catalogo.combo("KIT", "10.00", caneca, 1);

        combosUc.inativar(kit.getId());
        combosUc.inativar(kit.getId());

        assertThat(itens.buscarComboPorId(kit.getId()).orElseThrow().isAtivo()).isFalse();
    }

    @Test
    void aVariacaoNaoEhAceitaComoComboNasOperacoesDeCombo() {
        Categoria cat = catalogo.categoria("Casa", null);
        var caneca = catalogo.variacao(cat, "CANECA", "25.00");

        assertThatThrownBy(() -> combosUc.alterarPreco(caneca.getId(), Dinheiro.de("1")))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> combosUc.inativar(caneca.getId())).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void disponibilidadeDoComboComSaldosDeEstoqueDefinidosEOMenorQuociente() {
        Categoria cat = catalogo.categoria("Casa", null);
        var caneca = catalogo.variacao(cat, "CANECA", "25.00");
        var cafe = catalogo.variacao(cat, "CAFE", "30.00");
        catalogo.combo("KIT", "70.00", caneca, 3, cafe, 2);
        estoque.definirSaldo(caneca.getId(), 10, 0);
        estoque.definirSaldo(cafe.getId(), 5, 0);

        // min(10 / 3, 5 / 2) = min(3, 2) = 2
        assertThat(consultar.combos()).singleElement().satisfies(c -> assertThat(c.disponibilidade()).isEqualTo(2));

        estoque.definirSaldo(cafe.getId(), 1, 0);
        assertThat(consultar.combos().get(0).disponibilidade()).isZero();
    }
}
