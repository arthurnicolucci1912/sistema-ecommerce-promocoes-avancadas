package br.com.fatec.ecommerce.catalogo.aplicacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fatec.ecommerce.TesteDeIntegracao;
import br.com.fatec.ecommerce.catalogo.CatalogoPersistidoParaTeste;
import br.com.fatec.ecommerce.catalogo.dominio.Categoria;
import br.com.fatec.ecommerce.catalogo.dominio.CategoriaRepository;
import br.com.fatec.ecommerce.compartilhado.dominio.ConflitoException;
import br.com.fatec.ecommerce.compartilhado.dominio.DadoInvalidoException;
import br.com.fatec.ecommerce.compartilhado.dominio.RecursoNaoEncontradoException;
import br.com.fatec.ecommerce.seguranca.UsuariosParaTeste;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

/** Administracao de categorias: arvore, ciclo, nome unico por pai e remocao, chamada por um operador comercial. */
@TesteDeIntegracao
@WithMockUser(roles = "OPERADOR_COMERCIAL")
class GerenciarCategoriasTests {

    @Autowired
    GerenciarCategorias gerenciar;
    @Autowired
    ConsultarCatalogo consultar;
    @Autowired
    CategoriaRepository categorias;
    @Autowired
    CatalogoPersistidoParaTeste catalogo;
    @Autowired
    UsuariosParaTeste helper;

    @BeforeEach
    void preparar() {
        helper.limpar();
    }

    @Test
    void criarSubcategoriaApareceDentroDoPaiNaArvore() {
        Categoria roupas = gerenciar.criar("Roupas", null);

        Categoria camisetas = gerenciar.criar("Camisetas", roupas.getId());

        assertThat(camisetas.getPaiId()).isEqualTo(roupas.getId());
        assertThat(consultar.arvoreDeCategorias()).singleElement().satisfies(raiz -> {
            assertThat(raiz.nome()).isEqualTo("Roupas");
            assertThat(raiz.filhas()).extracting(NoDeCategoria::nome).containsExactly("Camisetas");
        });
    }

    @Test
    void moverParaDentroDeUmaDescendenteEhConflitoEAArvoreNaoMuda() {
        Categoria roupas = gerenciar.criar("Roupas", null);
        Categoria camisetas = gerenciar.criar("Camisetas", roupas.getId());

        assertThatThrownBy(() -> gerenciar.mover(roupas.getId(), camisetas.getId()))
                .isInstanceOf(ConflitoException.class);
        assertThatThrownBy(() -> gerenciar.mover(roupas.getId(), roupas.getId()))
                .isInstanceOf(ConflitoException.class);

        assertThat(categorias.buscarPorId(roupas.getId()).orElseThrow().getPaiId()).isNull();
        assertThat(categorias.buscarPorId(camisetas.getId()).orElseThrow().getPaiId()).isEqualTo(roupas.getId());
    }

    @Test
    void moverParaOutroPaiEParaARaiz() {
        Categoria roupas = gerenciar.criar("Roupas", null);
        Categoria casa = gerenciar.criar("Casa", null);
        Categoria camisetas = gerenciar.criar("Camisetas", roupas.getId());

        assertThat(gerenciar.mover(camisetas.getId(), casa.getId()).getPaiId()).isEqualTo(casa.getId());
        assertThat(gerenciar.mover(camisetas.getId(), null).getPaiId()).isNull();
    }

    @Test
    void nomeRepetidoEntreIrmasEhConflitoMesmoComOutraCaixa() {
        Categoria roupas = gerenciar.criar("Roupas", null);
        gerenciar.criar("Camisetas", roupas.getId());

        assertThatThrownBy(() -> gerenciar.criar("camisetas", roupas.getId())).isInstanceOf(ConflitoException.class);
        assertThatThrownBy(() -> gerenciar.criar("Roupas", null)).isInstanceOf(ConflitoException.class);
        // o mesmo nome em outro pai e permitido
        assertThat(gerenciar.criar("Camisetas", null).getId()).isNotNull();
    }

    @Test
    void renomearValidaONomeUnicoEAceitaOProprioNome() {
        Categoria roupas = gerenciar.criar("Roupas", null);
        gerenciar.criar("Casa", null);

        assertThat(gerenciar.renomear(roupas.getId(), "Vestuário").getNome()).isEqualTo("Vestuário");
        assertThat(gerenciar.renomear(roupas.getId(), "Vestuário").getNome()).isEqualTo("Vestuário");
        assertThatThrownBy(() -> gerenciar.renomear(roupas.getId(), "Casa")).isInstanceOf(ConflitoException.class);
    }

    @Test
    void paiInexistenteEDadoInvalidoEAlvoInexistenteEhNaoEncontrado() {
        Categoria roupas = gerenciar.criar("Roupas", null);

        assertThatThrownBy(() -> gerenciar.criar("Camisetas", 9999L)).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> gerenciar.mover(roupas.getId(), 9999L)).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> gerenciar.mover(9999L, null)).isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> gerenciar.renomear(9999L, "X")).isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> gerenciar.remover(9999L)).isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> gerenciar.criar(" ", null)).isInstanceOf(DadoInvalidoException.class);
    }

    @Test
    void removerCategoriaComProdutosEhConflito() {
        Categoria camisetas = gerenciar.criar("Camisetas", null);
        catalogo.produto("Camiseta Básica", camisetas);

        assertThatThrownBy(() -> gerenciar.remover(camisetas.getId())).isInstanceOf(ConflitoException.class);
        assertThat(categorias.buscarPorId(camisetas.getId())).isPresent();
    }

    @Test
    void removerCategoriaComSubcategoriasEhConflito() {
        Categoria roupas = gerenciar.criar("Roupas", null);
        gerenciar.criar("Camisetas", roupas.getId());

        assertThatThrownBy(() -> gerenciar.remover(roupas.getId())).isInstanceOf(ConflitoException.class);
    }

    @Test
    void removerCategoriaVaziaRemove() {
        Categoria vazia = gerenciar.criar("Vazia", null);

        gerenciar.remover(vazia.getId());

        assertThat(categorias.buscarPorId(vazia.getId())).isEmpty();
    }
}
