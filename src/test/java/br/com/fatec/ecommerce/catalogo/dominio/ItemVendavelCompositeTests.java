package br.com.fatec.ecommerce.catalogo.dominio;

import static br.com.fatec.ecommerce.catalogo.CatalogoParaTeste.categoria;
import static br.com.fatec.ecommerce.catalogo.CatalogoParaTeste.combo;
import static br.com.fatec.ecommerce.catalogo.CatalogoParaTeste.produto;
import static br.com.fatec.ecommerce.catalogo.CatalogoParaTeste.variacao;
import static br.com.fatec.ecommerce.catalogo.CatalogoParaTeste.variacaoDe;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fatec.ecommerce.compartilhado.dominio.ConflitoException;
import br.com.fatec.ecommerce.compartilhado.dominio.DadoInvalidoException;
import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Composite do item vendavel: componentes de estoque, disponibilidade, combo circular e extensibilidade. */
class ItemVendavelCompositeTests {

    private final VariacaoProduto caneca = variacao(10, "CANECA", "25.00");
    private final VariacaoProduto cafe = variacao(20, "CAFE", "30.00");
    private final VariacaoProduto teclado = variacao(30, "TECLADO", "300.00");

    private Combo kitCafe() {
        Combo kit = combo(100, "KIT-CAFE", "79.90");
        kit.adicionarItem(caneca, 1);
        kit.adicionarItem(cafe, 2);
        return kit;
    }

    @Test
    void variacaoTemComoComponenteDeEstoqueElaMesmaComAQuantidade() {
        assertThat(caneca.componentesDeEstoque(3)).containsExactly(new ComponenteEstoque(10L, 3));
    }

    @Test
    void comboMultiplicaAQuantidadePelosComponentes() {
        assertThat(kitCafe().componentesDeEstoque(1))
                .containsExactly(new ComponenteEstoque(10L, 1), new ComponenteEstoque(20L, 2));
        assertThat(kitCafe().componentesDeEstoque(3))
                .containsExactly(new ComponenteEstoque(10L, 3), new ComponenteEstoque(20L, 6));
    }

    @Test
    void comboDentroDeComboMultiplicaEmCadeiaEAgregaVariacoesRepetidas() {
        Combo presente = combo(200, "PRESENTE", "150.00");
        presente.adicionarItem(kitCafe(), 2);
        presente.adicionarItem(cafe, 1);
        presente.adicionarItem(teclado, 1);

        // 2 x (1 caneca + 2 cafes) + 1 cafe + 1 teclado, para 3 presentes.
        assertThat(presente.componentesDeEstoque(3)).containsExactlyInAnyOrder(
                new ComponenteEstoque(10L, 6),
                new ComponenteEstoque(20L, 15),
                new ComponenteEstoque(30L, 3));
    }

    @Test
    void variacaoRepetidaEmRamosDiferentesEhSomadaEmUmaUnicaParcela() {
        Combo a = combo(201, "A", "10.00");
        a.adicionarItem(caneca, 1);
        Combo b = combo(202, "B", "10.00");
        b.adicionarItem(caneca, 2);
        Combo pai = combo(203, "PAI", "10.00");
        pai.adicionarItem(a, 1);
        pai.adicionarItem(b, 1);

        assertThat(pai.componentesDeEstoque(1)).containsExactly(new ComponenteEstoque(10L, 3));
    }

    @Test
    void quantidadeNaoPositivaEhRejeitada() {
        assertThatThrownBy(() -> caneca.componentesDeEstoque(0)).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> kitCafe().componentesDeEstoque(-1)).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> kitCafe().adicionarItem(teclado, 0)).isInstanceOf(DadoInvalidoException.class);
    }

    @Test
    void comboComTodosOsComponentesAtivosEstaDisponivel() {
        assertThat(kitCafe().estaDisponivelParaVenda()).isTrue();
    }

    @Test
    void comboComComponenteInativoNaoEstaDisponivel() {
        Combo kit = kitCafe();
        cafe.inativar();

        assertThat(kit.estaDisponivelParaVenda()).isFalse();
    }

    @Test
    void comboInativoNaoEstaDisponivelMesmoComComponentesAtivos() {
        Combo kit = kitCafe();
        kit.inativar();

        assertThat(kit.estaDisponivelParaVenda()).isFalse();
    }

    @Test
    void componenteInativoDeUmComboInternoTornaOComboExternoIndisponivel() {
        Combo externo = combo(300, "EXTERNO", "200.00");
        externo.adicionarItem(kitCafe(), 1);
        externo.adicionarItem(teclado, 1);
        assertThat(externo.estaDisponivelParaVenda()).isTrue();

        caneca.inativar();

        assertThat(externo.estaDisponivelParaVenda()).isFalse();
    }

    @Test
    void variacaoDeProdutoInativoNaoEstaDisponivelEInativarOProdutoInativaAsVariacoes() {
        Produto produto = produto("Camiseta", categoria(1, "Camisetas", null));
        VariacaoProduto m = variacaoDe(produto, 40, "CAM-M", "59.90");
        VariacaoProduto g = variacaoDe(produto, 41, "CAM-G", "59.90");
        Combo kit = combo(400, "KIT", "100.00");
        kit.adicionarItem(m, 1);
        assertThat(kit.estaDisponivelParaVenda()).isTrue();

        produto.inativar();

        assertThat(m.isAtivo()).isFalse();
        assertThat(g.isAtivo()).isFalse();
        assertThat(m.estaDisponivelParaVenda()).isFalse();
        assertThat(kit.estaDisponivelParaVenda()).isFalse();
    }

    @Test
    void comboVazioNaoEstaDisponivel() {
        assertThat(combo(500, "VAZIO", "10.00").estaDisponivelParaVenda()).isFalse();
    }

    @Test
    void comboNaoPodeConterASiMesmo() {
        Combo kit = kitCafe();

        assertThatThrownBy(() -> kit.adicionarItem(kit, 1)).isInstanceOf(ConflitoException.class);
    }

    @Test
    void comboNaoPodeConterIndiretamenteASiMesmo() {
        Combo a = combo(601, "A", "10.00");
        Combo b = combo(602, "B", "10.00");
        Combo c = combo(603, "C", "10.00");
        a.adicionarItem(b, 1);
        b.adicionarItem(c, 1);

        // c -> a fecharia o ciclo a -> b -> c -> a
        assertThatThrownBy(() -> c.adicionarItem(a, 1)).isInstanceOf(ConflitoException.class);
        assertThatThrownBy(() -> b.adicionarItem(a, 1)).isInstanceOf(ConflitoException.class);
        assertThat(c.getItens()).isEmpty();
    }

    @Test
    void mesmoItemNaoPodeEntrarDuasVezesNoCombo() {
        Combo kit = kitCafe();

        assertThatThrownBy(() -> kit.adicionarItem(caneca, 1)).isInstanceOf(ConflitoException.class);
    }

    @Test
    void precoDeveSerMaiorQueZero() {
        assertThatThrownBy(() -> new Combo("X", "Combo", Dinheiro.zero())).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> caneca.alterarPreco(Dinheiro.de("-1"))).isInstanceOf(DadoInvalidoException.class);
        caneca.alterarPreco(Dinheiro.de("26.50"));
        assertThat(caneca.preco()).isEqualTo(Dinheiro.de("26.50"));
    }

    @Test
    void produtoNovoNomeiaAVariacaoPeloProdutoEPelosAtributos() {
        assertThat(caneca.getNome()).isEqualTo("Produto CANECA M");
        assertThat(caneca.getAtributos()).containsEntry("tamanho", "M");
    }

    /**
     * Extensibilidade: um tipo novo de item vendavel (subclasse que so existe em src/test) e tratado por um combo
     * existente e pelo calculo de componentes, sem nenhuma alteracao nas classes do catalogo.
     */
    @Test
    void novoTipoDeItemVendavelEhTratadoPeloComboSemAlterarClassesExistentes() {
        AssinaturaDeTeste assinatura = new AssinaturaDeTeste("ASSINA-3M", "Assinatura de 3 meses", "90.00", 20L, 3);
        Combo kit = combo(700, "KIT-ASSINA", "200.00");
        kit.adicionarItem(assinatura, 2);
        kit.adicionarItem(teclado, 1);

        // A assinatura consome 3 cafes por unidade; o kit leva 2 assinaturas.
        assertThat(kit.componentesDeEstoque(1)).containsExactlyInAnyOrder(
                new ComponenteEstoque(20L, 6),
                new ComponenteEstoque(30L, 1));
        assertThat(kit.estaDisponivelParaVenda()).isTrue();
        assertThat(assinatura.contemOuE(kit)).isFalse();
        assertThat(kit.contemOuE(assinatura)).isTrue();

        assinatura.inativar();
        assertThat(kit.estaDisponivelParaVenda()).isFalse();
    }

    @Test
    void novoTipoTambemParticipaDaAgregacaoComOutrasVariacoes() {
        AssinaturaDeTeste assinatura = new AssinaturaDeTeste("ASSINA-1M", "Assinatura", "30.00", 20L, 3);
        Combo kit = combo(701, "KIT-MISTO", "100.00");
        kit.adicionarItem(assinatura, 1);
        kit.adicionarItem(cafe, 2);

        assertThat(kit.componentesDeEstoque(2)).containsExactly(new ComponenteEstoque(20L, 10));
        assertThat(List.of(assinatura, kit, cafe)).allMatch(i -> !i.componentesDeEstoque(1).isEmpty());
    }

    /** Subclasse nova de ItemVendavel: nao e entidade e consome {@code porUnidade} de uma variacao por venda. */
    static class AssinaturaDeTeste extends ItemVendavel {

        private final Long variacaoId;
        private final int porUnidade;

        AssinaturaDeTeste(String sku, String nome, String preco, Long variacaoId, int porUnidade) {
            super(sku, nome, Dinheiro.de(preco));
            this.variacaoId = variacaoId;
            this.porUnidade = porUnidade;
        }

        @Override
        public boolean estaDisponivelParaVenda() {
            return isAtivo();
        }

        @Override
        public List<ComponenteEstoque> componentesDeEstoque(int quantidade) {
            return List.of(new ComponenteEstoque(variacaoId, quantidade * porUnidade));
        }
    }
}
