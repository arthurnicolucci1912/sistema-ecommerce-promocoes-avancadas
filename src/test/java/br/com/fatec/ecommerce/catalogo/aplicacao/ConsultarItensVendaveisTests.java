package br.com.fatec.ecommerce.catalogo.aplicacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fatec.ecommerce.TesteDeIntegracao;
import br.com.fatec.ecommerce.catalogo.CatalogoPersistidoParaTeste;
import br.com.fatec.ecommerce.catalogo.dominio.Categoria;
import br.com.fatec.ecommerce.catalogo.dominio.ComponenteEstoque;
import br.com.fatec.ecommerce.catalogo.dominio.Combo;
import br.com.fatec.ecommerce.catalogo.dominio.VariacaoProduto;
import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import br.com.fatec.ecommerce.compartilhado.dominio.RecursoNaoEncontradoException;
import br.com.fatec.ecommerce.seguranca.UsuariosParaTeste;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * Contrato do catalogo para os proximos modulos: a mesma visao para variacao e combo, sem restricao de perfil
 * (os testes rodam sem usuario autenticado) e sem o consumidor distinguir o tipo.
 */
@TesteDeIntegracao
class ConsultarItensVendaveisTests {

    @Autowired
    ConsultarItensVendaveis consultar;
    @Autowired
    GerenciarProdutos gerenciarProdutos;
    @Autowired
    CatalogoPersistidoParaTeste catalogo;
    @Autowired
    UsuariosParaTeste helper;

    private Categoria roupas;
    private Categoria camisetas;
    private VariacaoProduto camisetaM;
    private VariacaoProduto caneca;
    private VariacaoProduto cafe;
    private Combo kitCafe;

    @BeforeEach
    void preparar() {
        helper.limpar();
        roupas = catalogo.categoria("Roupas", null);
        camisetas = catalogo.categoria("Camisetas", roupas);
        Categoria casa = catalogo.categoria("Casa", null);
        camisetaM = catalogo.variacao(camisetas, "CAM-M", "59.90");
        caneca = catalogo.variacao(casa, "CANECA", "29.90");
        cafe = catalogo.variacao(casa, "CAFE", "39.90");
        kitCafe = catalogo.combo("KIT-CAFE", "79.90", caneca, 1, cafe, 2);
    }

    @Test
    void consultarVariacaoPorSkuTrazPrecoCategoriasAncestraisEUmComponente() {
        ItemVendavelInfo info = consultar.porSku("CAM-M");

        assertThat(info.id()).isEqualTo(camisetaM.getId());
        assertThat(info.sku()).isEqualTo("CAM-M");
        assertThat(info.nome()).isEqualTo(camisetaM.getNome());
        assertThat(info.preco()).isEqualTo(Dinheiro.de("59.90"));
        assertThat(info.ativo()).isTrue();
        assertThat(info.disponivelParaVenda()).isTrue();
        assertThat(info.categoriaIds()).containsExactlyInAnyOrder(camisetas.getId(), roupas.getId());
        assertThat(info.componentes()).containsExactly(new ComponenteEstoque(camisetaM.getId(), 1));
    }

    @Test
    void consultarComboPorSkuTrazPrecoEComponentesDeEstoque() {
        ItemVendavelInfo info = consultar.porSku("KIT-CAFE");

        assertThat(info.id()).isEqualTo(kitCafe.getId());
        assertThat(info.preco()).isEqualTo(Dinheiro.de("79.90"));
        assertThat(info.disponivelParaVenda()).isTrue();
        assertThat(info.componentes()).containsExactly(new ComponenteEstoque(caneca.getId(), 1),
                new ComponenteEstoque(cafe.getId(), 2));
        // o combo nao tem categoria propria (decisao documentada no caso de uso)
        assertThat(info.categoriaIds()).isEmpty();
    }

    @Test
    void comboDentroDeComboAgregaOsComponentes() {
        catalogo.combo("KIT-MAIOR", "129.90", kitCafe, 2, caneca, 1);

        assertThat(consultar.porSku("KIT-MAIOR").componentes()).containsExactlyInAnyOrder(
                new ComponenteEstoque(caneca.getId(), 3), new ComponenteEstoque(cafe.getId(), 4));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void itemInativoApareceComSituacaoEIndisponivelParaVenda() {
        gerenciarProdutos.inativarProduto(camisetaM.getProduto().getId());

        ItemVendavelInfo info = consultar.porSku("CAM-M");

        assertThat(info.ativo()).isFalse();
        assertThat(info.disponivelParaVenda()).isFalse();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void comboComComponenteInativoNaoEstaDisponivelParaVenda() {
        gerenciarProdutos.inativarProduto(cafe.getProduto().getId());

        ItemVendavelInfo info = consultar.porSku("KIT-CAFE");

        assertThat(info.ativo()).isTrue();
        assertThat(info.disponivelParaVenda()).isFalse();
    }

    @Test
    void porSkusPreservaAOrdemSemRepeticaoEUmSkuInexistenteEhNaoEncontrado() {
        assertThat(consultar.porSkus(List.of("KIT-CAFE", "CAM-M", "KIT-CAFE")))
                .extracting(ItemVendavelInfo::sku).containsExactly("KIT-CAFE", "CAM-M");
        assertThat(consultar.porSkus(List.of())).isEmpty();
        assertThatThrownBy(() -> consultar.porSku("NAO-EXISTE")).isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> consultar.porSkus(List.of("CAM-M", "X-1", "X-2")))
                .isInstanceOf(RecursoNaoEncontradoException.class).hasMessageContaining("X-1").hasMessageContaining("X-2");
    }
}
