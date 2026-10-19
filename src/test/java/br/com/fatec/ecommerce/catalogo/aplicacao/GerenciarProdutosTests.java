package br.com.fatec.ecommerce.catalogo.aplicacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fatec.ecommerce.TesteDeIntegracao;
import br.com.fatec.ecommerce.catalogo.CapturaDeEventosDoCatalogo;
import br.com.fatec.ecommerce.catalogo.CatalogoPersistidoParaTeste;
import br.com.fatec.ecommerce.catalogo.dominio.Categoria;
import br.com.fatec.ecommerce.catalogo.dominio.Combo;
import br.com.fatec.ecommerce.catalogo.dominio.CategoriaRepository;
import br.com.fatec.ecommerce.catalogo.dominio.ItemVendavel;
import br.com.fatec.ecommerce.catalogo.dominio.ItemVendavelRepository;
import br.com.fatec.ecommerce.catalogo.dominio.ProdutoRepository;
import br.com.fatec.ecommerce.catalogo.dominio.evento.VariacaoCadastrada;
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
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;

/** Administracao de produtos e variacoes: cadastro, SKU unico, dados invalidos, preco e inativacao. */
@TesteDeIntegracao
@Import(CapturaDeEventosDoCatalogo.class)
@WithMockUser(roles = "OPERADOR_COMERCIAL")
class GerenciarProdutosTests {

    @Autowired
    GerenciarProdutos gerenciar;
    @Autowired
    CategoriaRepository categorias;
    @Autowired
    ProdutoRepository produtos;
    @Autowired
    ItemVendavelRepository itens;
    @Autowired
    CatalogoPersistidoParaTeste catalogo;
    @Autowired
    CapturaDeEventosDoCatalogo captura;
    @Autowired
    EstoqueParaTeste estoque;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    UsuariosParaTeste helper;

    private Categoria camisetas;

    @BeforeEach
    void preparar() {
        helper.limpar();
        captura.limpar();
        camisetas = catalogo.categoria("Camisetas", null);
    }

    private static NovaVariacao variacao(String sku, String preco, Map<String, String> atributos) {
        return new NovaVariacao(sku, Dinheiro.de(preco), atributos);
    }

    private ProdutoAdministrativo camisetaBasica() {
        return gerenciar.cadastrar("Camiseta Básica", "Algodão", "Marca X", camisetas.getId(), List.of(
                variacao("CAM-M-AZ", "59.90", Map.of("tamanho", "M", "cor", "azul")),
                variacao("CAM-G-AZ", "59.90", Map.of("tamanho", "G", "cor", "azul"))));
    }

    private int quantidadeDeProdutos() {
        return jdbc.queryForObject("SELECT count(*) FROM produto", Integer.class);
    }

    @Test
    void produtoComVariacoesTemSkusDistintosEPublicaUmEventoPorVariacao() {
        ProdutoAdministrativo produto = camisetaBasica();

        assertThat(produto.id()).isNotNull();
        assertThat(produto.ativo()).isTrue();
        assertThat(produto.categoriaId()).isEqualTo(camisetas.getId());
        assertThat(produto.variacoes()).extracting(ProdutoAdministrativo.Variacao::sku)
                .containsExactly("CAM-M-AZ", "CAM-G-AZ");
        assertThat(produto.variacoes()).allSatisfy(v -> {
            assertThat(v.preco()).isEqualTo(Dinheiro.de("59.90"));
            assertThat(v.ativo()).isTrue();
        });
        assertThat(captura.recebidos()).extracting(VariacaoCadastrada::sku)
                .containsExactly("CAM-M-AZ", "CAM-G-AZ");
        // o ouvinte do estoque criou o saldo zero de cada variacao
        assertThat(produto.variacoes()).allSatisfy(v -> {
            assertThat(estoque.existeSaldo(v.id())).isTrue();
            assertThat(estoque.disponivel(v.id())).isZero();
        });
    }

    @Test
    void novoTipoDeCaracteristicaEhAceitoSemConfiguracaoPrevia() {
        ProdutoAdministrativo notebook = gerenciar.cadastrar("Notebook", null, null, camisetas.getId(),
                List.of(variacao("NB-512", "4999.90", Map.of("Armazenamento", "512 GB"))));

        assertThat(notebook.variacoes()).singleElement()
                .satisfies(v -> assertThat(v.atributos()).containsEntry("Armazenamento", "512 GB"));
    }

    @Test
    void dadosInvalidosSaoRejeitadosENadaEhGravado() {
        Long categoriaId = camisetas.getId();

        assertThatThrownBy(() -> gerenciar.cadastrar("Camiseta", null, null, categoriaId,
                List.of(variacao("A-1", "0.00", Map.of()))))
                .isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> gerenciar.cadastrar("Camiseta", null, null, categoriaId,
                List.of(variacao("A-2", "-5.00", Map.of()))))
                .isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> gerenciar.cadastrar("Camiseta", null, null, 9999L,
                List.of(variacao("A-3", "10.00", Map.of()))))
                .isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> gerenciar.cadastrar("Camiseta", null, null, null,
                List.of(variacao("A-3", "10.00", Map.of()))))
                .isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> gerenciar.cadastrar("Camiseta", null, null, categoriaId, List.of()))
                .isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> gerenciar.cadastrar(" ", null, null, categoriaId,
                List.of(variacao("A-4", "10.00", Map.of()))))
                .isInstanceOf(DadoInvalidoException.class);
        // a segunda variacao e invalida: o produto e a primeira variacao tambem nao ficam gravados
        assertThatThrownBy(() -> gerenciar.cadastrar("Camiseta", null, null, categoriaId, List.of(
                variacao("A-5", "10.00", Map.of()), variacao("A-6", "0", Map.of()))))
                .isInstanceOf(DadoInvalidoException.class);

        assertThat(quantidadeDeProdutos()).isZero();
        assertThat(itens.existePorSku("A-5")).isFalse();
    }

    @Test
    void skuDuplicadoContraVariacaoEhConflito() {
        camisetaBasica();
        captura.limpar();

        assertThatThrownBy(() -> gerenciar.cadastrar("Outra", null, null, camisetas.getId(),
                List.of(variacao("CAM-M-AZ", "10.00", Map.of()))))
                .isInstanceOf(ConflitoException.class);

        assertThat(quantidadeDeProdutos()).isEqualTo(1);
        assertThat(captura.recebidos()).isEmpty();
    }

    @Test
    void skuDeVariacaoContraSkuDeComboEhConflito() {
        ProdutoAdministrativo produto = camisetaBasica();
        ItemVendavel camisetaM = itens.buscarPorSku("CAM-M-AZ").orElseThrow();
        catalogo.combo("KIT-1", "99.90", camisetaM, 2);

        assertThatThrownBy(() -> gerenciar.adicionarVariacao(produto.id(), variacao("KIT-1", "10.00", Map.of())))
                .isInstanceOf(ConflitoException.class);
    }

    @Test
    void skuRepetidoNaMesmaRequisicaoEhConflito() {
        assertThatThrownBy(() -> gerenciar.cadastrar("Camiseta", null, null, camisetas.getId(), List.of(
                variacao("DUP", "10.00", Map.of()), variacao("DUP", "11.00", Map.of()))))
                .isInstanceOf(ConflitoException.class);

        assertThat(quantidadeDeProdutos()).isZero();
    }

    @Test
    void violacaoDaRestricaoDeSkuNoBancoViraConflito() {
        // Corrida: duas requisicoes passam juntas pela checagem previa e a segunda grava o SKU repetido.
        catalogo.variacao(camisetas, "CORRIDA", "10.00");
        ItemVendavel repetido = new Combo("CORRIDA", "Combo", Dinheiro.de("5.00"));

        assertThatThrownBy(() -> itens.salvar(repetido)).isInstanceOf(ConflitoException.class);
    }

    @Test
    void adicionarVariacaoPublicaOEventoESoDaNova() {
        ProdutoAdministrativo produto = camisetaBasica();
        captura.limpar();

        ProdutoAdministrativo atualizado = gerenciar.adicionarVariacao(produto.id(),
                variacao("CAM-GG-AZ", "64.90", Map.of("tamanho", "GG")));

        assertThat(atualizado.variacoes()).extracting(ProdutoAdministrativo.Variacao::sku)
                .containsExactly("CAM-M-AZ", "CAM-G-AZ", "CAM-GG-AZ");
        assertThat(captura.recebidos()).extracting(VariacaoCadastrada::sku).containsExactly("CAM-GG-AZ");
        assertThatThrownBy(() -> gerenciar.adicionarVariacao(9999L, variacao("X-1", "1.00", Map.of())))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void alterarDadosDoProdutoIncluindoACategoria() {
        ProdutoAdministrativo produto = camisetaBasica();
        Categoria regatas = catalogo.categoria("Regatas", null);

        ProdutoAdministrativo alterado = gerenciar.alterarDados(produto.id(), "Camiseta Premium", "Nova descrição",
                "Marca Y", regatas.getId());

        assertThat(alterado.nome()).isEqualTo("Camiseta Premium");
        assertThat(alterado.descricao()).isEqualTo("Nova descrição");
        assertThat(alterado.marca()).isEqualTo("Marca Y");
        assertThat(alterado.categoriaId()).isEqualTo(regatas.getId());
        assertThatThrownBy(() -> gerenciar.alterarDados(produto.id(), "X", null, null, 9999L))
                .isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> gerenciar.alterarDados(9999L, "X", null, null, regatas.getId()))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void alterarPrecoDaVariacaoValidaOPreco() {
        ProdutoAdministrativo produto = camisetaBasica();
        Long variacaoId = produto.variacoes().get(0).id();

        ProdutoAdministrativo alterado = gerenciar.alterarPrecoDaVariacao(variacaoId, Dinheiro.de("49.90"));

        assertThat(alterado.variacoes().get(0).preco()).isEqualTo(Dinheiro.de("49.90"));
        assertThat(alterado.variacoes().get(1).preco()).isEqualTo(Dinheiro.de("59.90"));
        assertThatThrownBy(() -> gerenciar.alterarPrecoDaVariacao(variacaoId, Dinheiro.de("0")))
                .isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> gerenciar.alterarPrecoDaVariacao(9999L, Dinheiro.de("10")))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void inativarProdutoInativaTambemAsVariacoes() {
        ProdutoAdministrativo produto = camisetaBasica();
        ItemVendavel camisetaM = itens.buscarPorSku("CAM-M-AZ").orElseThrow();
        assertThat(camisetaM.estaDisponivelParaVenda()).isTrue();

        gerenciar.inativarProduto(produto.id());

        assertThat(produtos.buscarPorId(produto.id()).orElseThrow().isAtivo()).isFalse();
        assertThat(itens.buscarPorSku("CAM-M-AZ").orElseThrow().isAtivo()).isFalse();
        assertThat(itens.buscarPorSku("CAM-G-AZ").orElseThrow().isAtivo()).isFalse();
        assertThatThrownBy(() -> gerenciar.inativarProduto(9999L)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void inativarVariacaoNaoAfetaAsOutras() {
        ProdutoAdministrativo produto = camisetaBasica();

        gerenciar.inativarVariacao(produto.variacoes().get(0).id());

        assertThat(itens.buscarPorSku("CAM-M-AZ").orElseThrow().isAtivo()).isFalse();
        assertThat(itens.buscarPorSku("CAM-G-AZ").orElseThrow().isAtivo()).isTrue();
        assertThat(produtos.buscarPorId(produto.id()).orElseThrow().isAtivo()).isTrue();
        assertThatThrownBy(() -> gerenciar.inativarVariacao(9999L)).isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
