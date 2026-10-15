package br.com.fatec.ecommerce.catalogo.dominio;

import static br.com.fatec.ecommerce.catalogo.CatalogoParaTeste.categoria;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fatec.ecommerce.compartilhado.dominio.ConflitoException;
import br.com.fatec.ecommerce.compartilhado.dominio.DadoInvalidoException;
import br.com.fatec.ecommerce.compartilhado.dominio.RecursoNaoEncontradoException;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Arvore de categorias: ancestrais, descendentes, ciclo no movimento e nome unico entre irmas. */
class CategoriaTests {

    // Roupas(1) > Camisetas(2) > Polos(3); Casa(4) > Cozinha(5); Roupas(1) > Calcas(6)
    private final Categoria roupas = categoria(1, "Roupas", null);
    private final Categoria camisetas = categoria(2, "Camisetas", roupas);
    private final Categoria polos = categoria(3, "Polos", camisetas);
    private final Categoria casa = categoria(4, "Casa", null);
    private final Categoria cozinha = categoria(5, "Cozinha", casa);
    private final Categoria calcas = categoria(6, "Calças", roupas);
    private final ArvoreDeCategorias arvore = new ArvoreDeCategorias(
            List.of(roupas, camisetas, polos, casa, cozinha, calcas));

    @Test
    void ancestraisVaoDoPaiAteARaizSemIncluirACategoria() {
        assertThat(arvore.ancestrais(3L)).containsExactly(camisetas, roupas);
        assertThat(arvore.ancestrais(1L)).isEmpty();
    }

    @Test
    void categoriaEAncestraisIncluiAPropriaCategoriaPrimeiro() {
        assertThat(arvore.categoriaEAncestrais(2L)).containsExactly(camisetas, roupas);
    }

    @Test
    void descendentesIncluemFilhasENetasSemIncluirACategoria() {
        assertThat(arvore.descendentes(1L)).containsExactlyInAnyOrder(camisetas, polos, calcas);
        assertThat(arvore.descendentes(4L)).containsExactly(cozinha);
        assertThat(arvore.descendentes(3L)).isEmpty();
    }

    @Test
    void categoriaInexistenteEhRecursoNaoEncontrado() {
        assertThatThrownBy(() -> arvore.ancestrais(99L)).isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> arvore.descendentes(99L)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void moverParaSiMesmaERejeitado() {
        assertThatThrownBy(() -> roupas.mover(roupas, arvore)).isInstanceOf(ConflitoException.class);
        assertThat(roupas.getPai()).isNull();
    }

    @Test
    void moverParaUmaDescendenteERejeitadoEAArvoreNaoMuda() {
        assertThatThrownBy(() -> roupas.mover(camisetas, arvore)).isInstanceOf(ConflitoException.class);
        assertThatThrownBy(() -> roupas.mover(polos, arvore)).isInstanceOf(ConflitoException.class);
        assertThat(roupas.getPai()).isNull();
    }

    @Test
    void moverParaForaDaPropriaSubarvoreEPermitido() {
        camisetas.mover(casa, arvore);

        assertThat(camisetas.getPai()).isSameAs(casa);
        ArvoreDeCategorias depois = new ArvoreDeCategorias(List.of(roupas, camisetas, polos, casa, cozinha, calcas));
        assertThat(depois.ancestrais(3L)).containsExactly(camisetas, casa);
    }

    @Test
    void moverParaARaizEPermitido() {
        camisetas.mover(null, arvore);

        assertThat(camisetas.getPai()).isNull();
    }

    @Test
    void nomeRepetidoEntreIrmasEhRejeitadoSemDiferenciarCaixa() {
        assertThatThrownBy(() -> Categoria.criar("camisetas", roupas, arvore)).isInstanceOf(ConflitoException.class);
        assertThatThrownBy(() -> Categoria.criar("CASA", null, arvore)).isInstanceOf(ConflitoException.class);
        assertThatThrownBy(() -> calcas.renomear("CAMISETAS", arvore)).isInstanceOf(ConflitoException.class);
    }

    @Test
    void mesmoNomeEmPaisDiferentesEhPermitido() {
        Categoria outras = Categoria.criar("Polos", casa, arvore);

        assertThat(outras.getNome()).isEqualTo("Polos");
        assertThat(outras.getPai()).isSameAs(casa);
    }

    @Test
    void moverNaoPodeRepetirNomeDeIrmaDoNovoPai() {
        Categoria polosDaCasa = categoria(7, "Polos", casa);
        ArvoreDeCategorias comRepetida = new ArvoreDeCategorias(
                List.of(roupas, camisetas, polos, casa, cozinha, calcas, polosDaCasa));

        assertThatThrownBy(() -> polos.mover(casa, comRepetida)).isInstanceOf(ConflitoException.class);
    }

    @Test
    void renomearParaOProprioNomeEPermitido() {
        camisetas.renomear("camisetas", arvore);

        assertThat(camisetas.getNome()).isEqualTo("camisetas");
    }

    @Test
    void nomeEmBrancoEhRejeitado() {
        assertThatThrownBy(() -> Categoria.criar("  ", null, arvore)).isInstanceOf(DadoInvalidoException.class);
    }
}
