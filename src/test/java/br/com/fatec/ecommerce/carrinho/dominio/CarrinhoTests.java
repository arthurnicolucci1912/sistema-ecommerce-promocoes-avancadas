package br.com.fatec.ecommerce.carrinho.dominio;

import static org.assertj.core.api.Assertions.*;
import br.com.fatec.ecommerce.compartilhado.dominio.DadoInvalidoException;
import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import br.com.fatec.ecommerce.compartilhado.dominio.RecursoNaoEncontradoException;
import org.junit.jupiter.api.Test;

class CarrinhoTests {
    private final Carrinho carrinho = new Carrinho(1L);

    private void incluir(int quantidade) {
        carrinho.incluir("SKU", 1L, "Item", Dinheiro.de("10"), quantidade);
    }

    @Test
    void acumulaAlteraERemoveSemDuplicarLinha() {
        incluir(2);
        incluir(1);
        assertThat(carrinho.getItens()).hasSize(1);
        assertThat(carrinho.item("SKU").getQuantidade()).isEqualTo(3);
        carrinho.alterarQuantidade("SKU", 4);
        assertThat(carrinho.item("SKU").getQuantidade()).isEqualTo(4);
        carrinho.remover("ausente");
        assertThat(carrinho.getItens()).hasSize(1);
        carrinho.remover("SKU");
        assertThat(carrinho.getItens()).isEmpty();
    }

    @Test
    void rejeitaQuantidadeInvalidaEEstouroSemAlteracaoParcial() {
        incluir(Integer.MAX_VALUE);
        assertThatThrownBy(() -> incluir(1)).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> incluir(0)).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> carrinho.alterarQuantidade("SKU", -1)).isInstanceOf(DadoInvalidoException.class);
        assertThat(carrinho.item("SKU").getQuantidade()).isEqualTo(Integer.MAX_VALUE);
        assertThatThrownBy(() -> carrinho.alterarQuantidade("outro", 1))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void validaSkuEProtegeColecao() {
        assertThatThrownBy(() -> carrinho.incluir(" ", 1L, "Item", Dinheiro.de("1"), 1))
                .isInstanceOf(DadoInvalidoException.class);
        incluir(1);
        assertThatThrownBy(() -> carrinho.getItens().clear()).isInstanceOf(UnsupportedOperationException.class);
    }
}
