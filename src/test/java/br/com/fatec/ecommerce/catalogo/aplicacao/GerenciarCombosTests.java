package br.com.fatec.ecommerce.catalogo.aplicacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import br.com.fatec.ecommerce.TesteDeIntegracao;
import br.com.fatec.ecommerce.catalogo.CatalogoPersistidoParaTeste;
import br.com.fatec.ecommerce.catalogo.dominio.Categoria;
import br.com.fatec.ecommerce.catalogo.dominio.ComponenteEstoque;
import br.com.fatec.ecommerce.catalogo.dominio.Combo;
import br.com.fatec.ecommerce.catalogo.dominio.ItemCombo;
import br.com.fatec.ecommerce.catalogo.dominio.ItemVendavelRepository;
import br.com.fatec.ecommerce.catalogo.dominio.VariacaoProduto;
import br.com.fatec.ecommerce.compartilhado.dominio.ConflitoException;
import br.com.fatec.ecommerce.compartilhado.dominio.DadoInvalidoException;
import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import br.com.fatec.ecommerce.compartilhado.dominio.RecursoNaoEncontradoException;
import br.com.fatec.ecommerce.seguranca.UsuariosParaTeste;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

/** Administracao de combos: cadastro, combo dentro de combo, ciclo, SKU unico, alteracao e inativacao. */
@TesteDeIntegracao
@WithMockUser(roles = "ADMIN")
class GerenciarCombosTests {

    @Autowired
    GerenciarCombos gerenciar;
    @Autowired
    ItemVendavelRepository itens;
    @Autowired
    CatalogoPersistidoParaTeste catalogo;
    @Autowired
    UsuariosParaTeste helper;

    private VariacaoProduto caneca;
    private VariacaoProduto cafe;

    @BeforeEach
    void preparar() {
        helper.limpar();
        Categoria casa = catalogo.categoria("Casa", null);
        caneca = catalogo.variacao(casa, "CANECA", "29.90");
        cafe = catalogo.variacao(casa, "CAFE", "39.90");
    }

    private static ComponenteDeCombo componente(String sku, int quantidade) {
        return new ComponenteDeCombo(sku, quantidade);
    }

    private Combo kitCafe() {
        return gerenciar.cadastrar("KIT-CAFE", "Kit Café", Dinheiro.de("79.90"),
                List.of(componente("CANECA", 1), componente("CAFE", 2)));
    }

    @Test
    void cadastrarComboListaOsComponentesEAsQuantidades() {
        Combo combo = kitCafe();

        assertThat(combo.getId()).isNotNull();
        assertThat(combo.preco()).isEqualTo(Dinheiro.de("79.90"));
        assertThat(combo.getItens()).extracting(i -> i.getItem().getSku(), ItemCombo::getQuantidade)
                .containsExactly(tuple("CANECA", 1),
                        tuple("CAFE", 2));
        assertThat(combo.componentesDeEstoque(1)).containsExactly(new ComponenteEstoque(caneca.getId(), 1),
                new ComponenteEstoque(cafe.getId(), 2));
    }

    @Test
    void comboDentroDeComboEhAceitoEMultiplicaOsComponentesDeEstoque() {
        kitCafe();

        Combo kitMaior = gerenciar.cadastrar("KIT-MAIOR", "Kit Maior", Dinheiro.de("129.90"),
                List.of(componente("KIT-CAFE", 2), componente("CANECA", 1)));

        assertThat(kitMaior.componentesDeEstoque(1)).containsExactlyInAnyOrder(
                new ComponenteEstoque(caneca.getId(), 3), new ComponenteEstoque(cafe.getId(), 4));
    }

    @Test
    void comboCircularEhConflitoAoAlterarOsComponentes() {
        Combo interno = kitCafe();
        Combo externo = gerenciar.cadastrar("KIT-MAIOR", "Kit Maior", Dinheiro.de("129.90"),
                List.of(componente("KIT-CAFE", 1), componente("CANECA", 1)));

        assertThatThrownBy(() -> gerenciar.substituirComponentes(interno.getId(),
                List.of(componente("KIT-MAIOR", 1), componente("CAFE", 1))))
                .isInstanceOf(ConflitoException.class);
        assertThatThrownBy(() -> gerenciar.substituirComponentes(interno.getId(), List.of(componente("KIT-CAFE", 1))))
                .isInstanceOf(ConflitoException.class);

        // nada mudou
        assertThat(itens.buscarComboPorId(interno.getId()).orElseThrow().componentesDeEstoque(1))
                .containsExactly(new ComponenteEstoque(caneca.getId(), 1), new ComponenteEstoque(cafe.getId(), 2));
        assertThat(itens.buscarComboPorId(externo.getId())).isPresent();
    }

    @Test
    void skuDeComboRepetidoContraVariacaoOuComboEhConflito() {
        kitCafe();

        assertThatThrownBy(() -> gerenciar.cadastrar("CANECA", "X", Dinheiro.de("10.00"), List.of(componente("CAFE", 1))))
                .isInstanceOf(ConflitoException.class);
        assertThatThrownBy(() -> gerenciar.cadastrar("KIT-CAFE", "X", Dinheiro.de("10.00"),
                List.of(componente("CAFE", 1))))
                .isInstanceOf(ConflitoException.class);
    }

    @Test
    void dadosInvalidosDoComboSao400() {
        assertThatThrownBy(() -> gerenciar.cadastrar("K1", "Kit", Dinheiro.de("10.00"), List.of()))
                .isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> gerenciar.cadastrar("K1", "Kit", Dinheiro.de("10.00"), null))
                .isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> gerenciar.cadastrar("K1", "Kit", Dinheiro.de("0"), List.of(componente("CAFE", 1))))
                .isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> gerenciar.cadastrar("K1", "Kit", Dinheiro.de("10.00"),
                List.of(componente("CAFE", 0))))
                .isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> gerenciar.cadastrar("K1", "Kit", Dinheiro.de("10.00"),
                List.of(componente("NAO-EXISTE", 1))))
                .isInstanceOf(DadoInvalidoException.class);

        assertThat(itens.existePorSku("K1")).isFalse();
    }

    @Test
    void alterarPrecoEComponentes() {
        Combo combo = kitCafe();

        assertThat(gerenciar.alterarPreco(combo.getId(), Dinheiro.de("69.90")).preco())
                .isEqualTo(Dinheiro.de("69.90"));
        Combo alterado = gerenciar.substituirComponentes(combo.getId(),
                List.of(componente("CAFE", 3), componente("CANECA", 2)));

        assertThat(alterado.componentesDeEstoque(1)).containsExactlyInAnyOrder(
                new ComponenteEstoque(cafe.getId(), 3), new ComponenteEstoque(caneca.getId(), 2));
        assertThat(itens.buscarComboPorId(combo.getId()).orElseThrow().getItens()).hasSize(2);
        assertThatThrownBy(() -> gerenciar.alterarPreco(combo.getId(), Dinheiro.de("0")))
                .isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> gerenciar.substituirComponentes(combo.getId(), List.of()))
                .isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> gerenciar.alterarPreco(9999L, Dinheiro.de("10")))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> gerenciar.substituirComponentes(9999L, List.of(componente("CAFE", 1))))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void aVariacaoNaoEhUmComboParaOsCasosDeUsoDeCombo() {
        assertThatThrownBy(() -> gerenciar.alterarPreco(cafe.getId(), Dinheiro.de("10")))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void inativarComboNaoInativaOsComponentes() {
        Combo combo = kitCafe();

        gerenciar.inativar(combo.getId());

        assertThat(itens.buscarComboPorId(combo.getId()).orElseThrow().isAtivo()).isFalse();
        assertThat(itens.buscarPorSku("CAFE").orElseThrow().isAtivo()).isTrue();
        assertThatThrownBy(() -> gerenciar.inativar(9999L)).isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
