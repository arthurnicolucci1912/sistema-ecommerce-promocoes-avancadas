package br.com.fatec.ecommerce.catalogo.dominio;

import static br.com.fatec.ecommerce.catalogo.CatalogoParaTeste.MOMENTO;
import static br.com.fatec.ecommerce.catalogo.CatalogoParaTeste.categoria;
import static br.com.fatec.ecommerce.catalogo.CatalogoParaTeste.comId;
import static br.com.fatec.ecommerce.catalogo.CatalogoParaTeste.produto;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.fatec.ecommerce.catalogo.dominio.evento.VariacaoCadastrada;
import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import br.com.fatec.ecommerce.compartilhado.dominio.EventoDominio;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** O produto acumula VariacaoCadastrada a cada variacao cadastrada e entrega cada evento uma unica vez. */
class VariacaoCadastradaTests {

    @Test
    void cadastrarVariacaoAcumulaOEventoComIdESkuDaVariacao() {
        Produto produto = produto("Camiseta", categoria(1, "Camisetas", null));
        VariacaoProduto m = produto.adicionarVariacao("CAM-M", Dinheiro.de("59.90"), Map.of("tamanho", "M"), MOMENTO);
        comId(m, 77);

        List<EventoDominio> eventos = produto.consumirEventos();

        assertThat(eventos).containsExactly(new VariacaoCadastrada(77L, "CAM-M", MOMENTO));
        assertThat(produto.consumirEventos()).isEmpty();
    }

    @Test
    void cadaVariacaoGeraSeuProprioEvento() {
        Produto produto = produto("Camiseta", categoria(1, "Camisetas", null));
        comId(produto.adicionarVariacao("CAM-M", Dinheiro.de("59.90"), Map.of("tamanho", "M"), MOMENTO), 1);
        comId(produto.adicionarVariacao("CAM-G", Dinheiro.de("59.90"), Map.of("tamanho", "G"), MOMENTO), 2);

        assertThat(produto.consumirEventos()).extracting(e -> ((VariacaoCadastrada) e).variacaoId())
                .containsExactly(1L, 2L);
    }
}
