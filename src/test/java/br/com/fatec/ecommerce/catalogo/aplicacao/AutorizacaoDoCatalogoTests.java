package br.com.fatec.ecommerce.catalogo.aplicacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fatec.ecommerce.TesteDeIntegracao;
import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * As operacoes de escrita do catalogo exigem ADMIN ou OPERADOR_COMERCIAL ({@code @PreAuthorize} nos casos de uso);
 * a consulta publica e o contrato de itens vendaveis nao exigem perfil. A rota HTTP e testada no grupo da API.
 */
@TesteDeIntegracao
class AutorizacaoDoCatalogoTests {

    @Autowired
    GerenciarCategorias categorias;
    @Autowired
    GerenciarProdutos produtos;
    @Autowired
    GerenciarCombos combos;
    @Autowired
    ConsultarCatalogo consultarCatalogo;
    @Autowired
    ConsultarItensVendaveis consultarItens;

    private void todasAsEscritasSaoNegadas() {
        assertThatThrownBy(() -> categorias.criar("X", null)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> categorias.remover(1L)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> produtos.cadastrar("X", null, null, 1L,
                List.of(new NovaVariacao("X", Dinheiro.de("1"), Map.of())))).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> produtos.inativarProduto(1L)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> combos.cadastrar("X", "X", Dinheiro.de("1"), List.of(new ComponenteDeCombo("Y", 1))))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> combos.inativar(1L)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(roles = "ESTOQUE")
    void perfilDeEstoqueNaoAlteraOCatalogo() {
        todasAsEscritasSaoNegadas();
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void clienteNaoAlteraOCatalogo() {
        todasAsEscritasSaoNegadas();
    }

    @Test
    @WithAnonymousUser
    void visitanteNaoAlteraOCatalogoMasConsultaPublicamente() {
        todasAsEscritasSaoNegadas();

        assertThat(consultarCatalogo.arvoreDeCategorias()).isNotNull();
        assertThat(consultarCatalogo.combos()).isNotNull();
        assertThat(consultarItens.porSkus(List.of())).isEmpty();
    }
}
