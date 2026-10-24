package br.com.fatec.ecommerce.checkout.aplicacao;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.com.fatec.ecommerce.carrinho.dominio.*;
import br.com.fatec.ecommerce.catalogo.aplicacao.*;
import br.com.fatec.ecommerce.catalogo.dominio.*;
import br.com.fatec.ecommerce.cliente.dominio.*;
import br.com.fatec.ecommerce.compartilhado.dominio.*;
import br.com.fatec.ecommerce.estoque.aplicacao.ConsultarSaldo;
import br.com.fatec.ecommerce.estoque.dominio.ComponenteAReservar;
import br.com.fatec.ecommerce.preco.aplicacao.PipelinePreco;
import br.com.fatec.ecommerce.preco.dominio.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class RevalidarCheckoutTests {
    CarrinhoRepository carrinhos = mock(CarrinhoRepository.class);
    EnderecoRepository enderecos = mock(EnderecoRepository.class);
    ConsultarItensVendaveis catalogo = mock(ConsultarItensVendaveis.class);
    ConsultarSaldo saldos = mock(ConsultarSaldo.class);
    Carrinho carrinho = new Carrinho(1L);
    Endereco endereco = new Endereco(1L, "Casa", "01001000", "Rua", "10", null, "Centro", "São Paulo", "SP");
    PipelinePreco pipeline = new PipelinePreco(List.of(new EtapaSubtotal(), new EtapaTotal()));

    @BeforeEach
    void preparar() {
        ReflectionTestUtils.setField(carrinho, "versao", 2L);
        when(carrinhos.buscarPorClienteId(1L)).thenReturn(Optional.of(carrinho));
        when(enderecos.buscarAtivo(10L, 1L)).thenReturn(Optional.of(endereco));
        when(saldos.porVariacoes(anyCollection())).thenReturn(List.of());
    }

    RevalidarCheckout aplicacao() { return new RevalidarCheckout(carrinhos, enderecos, catalogo, saldos, pipeline); }

    ItemVendavelInfo item(String sku, int componentes, boolean ativo) {
        return new ItemVendavelInfo(7L, sku, "Item " + sku, Dinheiro.de("10"), ativo, ativo,
                Set.of(), List.of(new ComponenteEstoque(7L, componentes)));
    }

    void incluir(String sku, int quantidade) { carrinho.incluir(sku, 7L, sku, Dinheiro.de("10"), quantidade); }

    @Test
    void enderecoSempreBuscadoPeloParECarrinhoVazioNaoFormaCompra() {
        assertThatThrownBy(() -> aplicacao().executar(1L, 11L)).isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> aplicacao().executar(1L, 10L)).isInstanceOf(ConflitoException.class);
        verifyNoInteractions(catalogo, saldos);
        verify(carrinhos, never()).salvar(any());
    }

    @Test
    void rejeitaSkusAusentesEInativosSemUsarPrecoDeReferencia() {
        incluir("A", 1);
        incluir("B", 1);
        when(catalogo.existentesPorSkus(anyCollection())).thenReturn(List.of(item("B", 1, false)));
        assertThatThrownBy(() -> aplicacao().executar(1L, 10L))
                .isInstanceOfSatisfying(ConflitoException.class,
                        e -> assertThat(e.propriedades()).containsEntry("skus", List.of("A", "B")));
        verifyNoInteractions(saldos);
        verify(carrinhos, never()).salvar(any());
    }

    @Test
    void demandaCompartilhadaESaldoAusenteNaoReservamNemModificamSelecao() {
        incluir("KIT", 2);
        incluir("AVULSO", 1);
        when(catalogo.existentesPorSkus(anyCollection())).thenReturn(List.of(item("KIT", 2, true), item("AVULSO", 1, true)));
        var compra = aplicacao().executar(1L, 10L);
        assertThat(compra.componentes()).containsExactly(new ComponenteAReservar(7L, 5));
        assertThat(compra.previa().confirmacao().total()).isEqualByComparingTo("30");
        assertThat(compra.previa().insuficiencias().getFirst().disponivel()).isZero();
        assertThat(compra.previa().insuficiencias().getFirst().solicitada()).isEqualTo(5);
        assertThat(carrinho.getItens()).hasSize(2);
        assertThat(carrinho.getVersao()).isEqualTo(2L);
        verify(carrinhos, never()).salvar(any());
    }

    @Test
    void produtoDeQuantidadesAcimaDaCapacidadeDeReservaEhRejeitado() {
        incluir("A", Integer.MAX_VALUE);
        when(catalogo.existentesPorSkus(anyCollection())).thenReturn(List.of(item("A", 2, true)));
        assertThatThrownBy(() -> aplicacao().executar(1L, 10L)).isInstanceOf(DadoInvalidoException.class);
        verifyNoInteractions(saldos);
    }

    @Test
    void somaDeLinhasAcimaDaCapacidadeDeReservaEhRejeitada() {
        incluir("A", Integer.MAX_VALUE);
        incluir("B", 1);
        when(catalogo.existentesPorSkus(anyCollection())).thenReturn(List.of(item("A", 1, true), item("B", 1, true)));
        assertThatThrownBy(() -> aplicacao().executar(1L, 10L)).isInstanceOf(DadoInvalidoException.class);
    }

    @Test
    void novoTipoDoCompositeENovaEtapaDoPipelineFuncionamSemMudarCheckout() {
        var itens = mock(ItemVendavelRepository.class);
        var categorias = mock(CategoriaRepository.class);
        when(categorias.listarTodas()).thenReturn(List.of());
        ItemVendavel novo = new ItemVendavel("NOVO", "Pacote", Dinheiro.de("25")) {
            @Override public Long getId() { return 99L; }
            @Override public boolean estaDisponivelParaVenda() { return true; }
            @Override public List<ComponenteEstoque> componentesDeEstoque(int quantidade) {
                return List.of(new ComponenteEstoque(7L, quantidade * 3));
            }
        };
        when(itens.buscarPorSkus(anyCollection())).thenReturn(List.of(novo));
        var visitada = new AtomicBoolean();
        pipeline = new PipelinePreco(List.of(new EtapaSubtotal(), calculo -> {
            assertThat(calculo.contexto().uf()).isEqualTo("SP");
            visitada.set(true);
        }, new EtapaTotal()));
        incluir("NOVO", 2);
        var revalidar = new RevalidarCheckout(carrinhos, enderecos, new ConsultarItensVendaveis(itens, categorias), saldos, pipeline);
        var compra = revalidar.executar(1L, 10L);
        assertThat(visitada).isTrue();
        assertThat(compra.pedido().total()).isEqualTo(Dinheiro.de("50"));
        assertThat(compra.componentes()).containsExactly(new ComponenteAReservar(7L, 6));
    }

    @Test
    void descricaoComercialSaiDoContratoEmOrdemEstavelSemDescricaoArtificialNoCombo() {
        var categoria = br.com.fatec.ecommerce.catalogo.CatalogoParaTeste.categoria(1, "Roupas", null);
        var produto = br.com.fatec.ecommerce.catalogo.CatalogoParaTeste.produto("Camisa", categoria);
        var atributos = new LinkedHashMap<String, String>();
        atributos.put("tamanho", "M");
        atributos.put("cor", "azul");
        var variacao = produto.adicionarVariacao("CAMISA", Dinheiro.de("30"), atributos, java.time.Instant.now());
        assertThat(variacao.descricaoVariacao()).isEqualTo("cor: azul, tamanho: M");
        assertThat(new Combo("KIT", "Kit", Dinheiro.de("35")).descricaoVariacao()).isNull();
    }

    @Test
    void esvaziarRemoveSelecaoEPreservaIdentidade() {
        incluir("A", 2);
        carrinho.esvaziar();
        assertThat(carrinho.getItens()).isEmpty();
        assertThat(carrinho.getClienteId()).isEqualTo(1L);
        carrinho.esvaziar();
        assertThat(carrinho.getItens()).isEmpty();
    }
}
