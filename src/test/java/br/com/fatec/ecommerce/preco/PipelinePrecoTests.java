package br.com.fatec.ecommerce.preco;

import static org.assertj.core.api.Assertions.*;
import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import br.com.fatec.ecommerce.preco.aplicacao.PipelinePreco;
import br.com.fatec.ecommerce.preco.dominio.*;
import br.com.fatec.ecommerce.promocao.dominio.*;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PipelinePrecoTests {
    private ContextoPreco contexto(List<ItemPreco> itens) {
        return new ContextoPreco(itens, 1L, false, null, Dinheiro.zero(), Instant.EPOCH);
    }

    private ItemPreco item(String preco, int quantidade) {
        return new ItemPreco(1L, "SKU", Set.of(), quantidade, Dinheiro.de(preco));
    }

    @Test
    void calculaLinhasComPrecisaoEVazioSemVazarEstado() {
        PipelinePreco pipeline = new PipelinePreco(List.of(new EtapaSubtotal(), new EtapaTotal()));
        var calculo = pipeline.calcular(contexto(List.of(item("59.90", 2), item("79.90", 1))));
        assertThat(calculo.subtotal()).isEqualTo(Dinheiro.de("199.70"));
        assertThat(calculo.total()).isEqualTo(calculo.subtotal());
        assertThat(calculo.descontos()).isEmpty();
        assertThat(calculo.frete()).isEqualTo(Dinheiro.zero());
        var vazio = pipeline.calcular(contexto(List.of()));
        assertThat(vazio.total()).isEqualTo(Dinheiro.zero());
        assertThat(vazio.subtotal()).isEqualTo(Dinheiro.zero());
        assertThat(pipeline.calcular(contexto(List.of(item("1.005", 2)))).total()).isEqualTo(Dinheiro.de("2.00"));
    }

    @Test
    void registraNovoHandlerSemAlterarSubtotalOuTotalEExecutaNaOrdem() {
        EtapaPreco frete = calculo -> {
            assertThat(calculo.subtotal()).isEqualTo(Dinheiro.de("100"));
            calculo.definirFrete(Dinheiro.de("5"));
        };
        var contexto = contexto(List.of(item("100", 1)));
        var estendido = new PipelinePreco(List.of(new EtapaSubtotal(), frete, new EtapaTotal())).calcular(contexto);
        assertThat(estendido.total()).isEqualTo(Dinheiro.de("105"));
        var normal = new PipelinePreco(List.of(new EtapaSubtotal(), new EtapaTotal())).calcular(contexto);
        assertThat(normal.total()).isEqualTo(Dinheiro.de("100"));
        assertThat(normal.frete()).isEqualTo(Dinheiro.zero());
    }

    @Test
    void descontoNoFreteNaoAbateItensNemGeraTotalNegativo() {
        EtapaPreco descontos = calculo -> {
            calculo.definirFrete(Dinheiro.de("5"));
            calculo.adicionarDesconto(new DescontoAplicado(1L, "Frete", Dinheiro.de("20"), AlvoDesconto.FRETE));
        };
        var resultado = new PipelinePreco(List.of(new EtapaSubtotal(), descontos, new EtapaTotal()))
                .calcular(contexto(List.of(item("100", 1))));
        assertThat(resultado.total()).isEqualTo(Dinheiro.de("100"));
    }
}
