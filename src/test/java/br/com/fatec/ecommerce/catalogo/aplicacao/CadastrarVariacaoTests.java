package br.com.fatec.ecommerce.catalogo.aplicacao;

import static br.com.fatec.ecommerce.catalogo.CatalogoParaTeste.MOMENTO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fatec.ecommerce.TesteDeIntegracao;
import br.com.fatec.ecommerce.catalogo.CapturaDeEventosDoCatalogo;
import br.com.fatec.ecommerce.catalogo.dominio.Categoria;
import br.com.fatec.ecommerce.catalogo.dominio.CategoriaRepository;
import br.com.fatec.ecommerce.catalogo.dominio.ArvoreDeCategorias;
import br.com.fatec.ecommerce.catalogo.dominio.Produto;
import br.com.fatec.ecommerce.catalogo.dominio.ProdutoRepository;
import br.com.fatec.ecommerce.catalogo.dominio.VariacaoProduto;
import br.com.fatec.ecommerce.catalogo.dominio.evento.VariacaoCadastrada;
import br.com.fatec.ecommerce.compartilhado.dominio.ConflitoException;
import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import br.com.fatec.ecommerce.seguranca.UsuariosParaTeste;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/** Cadastrar uma variacao publica VariacaoCadastrada (Observer) com o id gravado, depois de salvar. */
@TesteDeIntegracao
@Import(CapturaDeEventosDoCatalogo.class)
class CadastrarVariacaoTests {

    @Autowired
    CadastrarVariacao cadastrarVariacao;
    @Autowired
    CategoriaRepository categorias;
    @Autowired
    ProdutoRepository produtos;
    @Autowired
    CapturaDeEventosDoCatalogo captura;
    @Autowired
    UsuariosParaTeste helper;

    private Long produtoId;

    @BeforeEach
    void preparar() {
        helper.limpar();
        captura.limpar();
        Categoria camisetas = categorias.salvar(Categoria.criar("Camisetas", null, new ArvoreDeCategorias(List.of())));
        produtoId = produtos.salvar(Produto.criar("Camiseta Básica", null, null, camisetas, MOMENTO)).getId();
    }

    @Test
    void publicaOEventoComOIdDaVariacaoGravada() {
        VariacaoProduto variacao = cadastrarVariacao.cadastrar(produtoId, "CAM-M", Dinheiro.de("59.90"),
                Map.of("tamanho", "M", "cor", "azul"));

        assertThat(variacao.getId()).isNotNull();
        assertThat(captura.recebidos()).hasSize(1);
        VariacaoCadastrada evento = captura.recebidos().get(0);
        assertThat(evento.variacaoId()).isEqualTo(variacao.getId());
        assertThat(evento.sku()).isEqualTo("CAM-M");
        assertThat(evento.ocorridoEm()).isNotNull();
    }

    @Test
    void skuDuplicadoEhConflitoENaoPublicaEvento() {
        cadastrarVariacao.cadastrar(produtoId, "CAM-M", Dinheiro.de("59.90"), Map.of("tamanho", "M"));
        captura.limpar();

        assertThatThrownBy(() -> cadastrarVariacao.cadastrar(produtoId, "CAM-M", Dinheiro.de("10.00"), Map.of()))
                .isInstanceOf(ConflitoException.class);
        assertThat(captura.recebidos()).isEmpty();
    }

    @Test
    void segundaVariacaoDoMesmoProdutoPublicaSoOSeuEvento() {
        cadastrarVariacao.cadastrar(produtoId, "CAM-M", Dinheiro.de("59.90"), Map.of("tamanho", "M"));
        captura.limpar();

        VariacaoProduto g = cadastrarVariacao.cadastrar(produtoId, "CAM-G", Dinheiro.de("59.90"),
                Map.of("tamanho", "G"));

        assertThat(captura.recebidos()).extracting(VariacaoCadastrada::variacaoId).containsExactly(g.getId());
    }
}
