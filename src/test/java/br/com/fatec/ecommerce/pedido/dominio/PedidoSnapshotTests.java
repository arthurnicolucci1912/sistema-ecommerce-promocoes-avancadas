package br.com.fatec.ecommerce.pedido.dominio;

import static br.com.fatec.ecommerce.pedido.PedidosParaTeste.MOMENTO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fatec.ecommerce.compartilhado.dominio.DadoInvalidoException;
import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import br.com.fatec.ecommerce.pedido.PedidosParaTeste;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Validacao do snapshot comercial na criacao do pedido: itens, descontos, frete, totais (valores monetarios com
 * duas casas), normalizacao de textos e frete liquido derivado. Complementa PedidoTests.
 */
class PedidoSnapshotTests {

    private static final EnderecoEntrega ENDERECO = PedidosParaTeste.enderecoPadrao();

    private static DadosNovoPedido.Item item(String sku, int qtd, String preco) {
        return new DadosNovoPedido.Item(sku, "Produto " + sku, null, qtd, Dinheiro.de(preco));
    }

    private static DadosNovoPedido.Desconto desconto(String valor, AlvoDoDesconto alvo) {
        return new DadosNovoPedido.Desconto("Promo", OrigemDesconto.PROMOCAO, alvo, Dinheiro.de(valor));
    }

    /** Monta o snapshot calculando os totais corretamente a partir dos itens, descontos e frete. */
    private static DadosNovoPedido snapshot(List<DadosNovoPedido.Item> itens, List<DadosNovoPedido.Desconto> descontos,
            String frete) {
        Dinheiro subtotal = itens.stream().map(i -> i.precoUnitario().multiplicar(i.quantidade()))
                .reduce(Dinheiro.zero(), Dinheiro::somar);
        Dinheiro totalDescontos = descontos.stream().map(DadosNovoPedido.Desconto::valor)
                .reduce(Dinheiro.zero(), Dinheiro::somar);
        Dinheiro f = Dinheiro.de(frete);
        return new DadosNovoPedido(1L, ENDERECO, itens, descontos, f, subtotal, totalDescontos,
                subtotal.subtrair(totalDescontos).somar(f));
    }

    // --- Valores monetarios ---

    @Test
    void totalComCentavosEhExatoSemErroDeArredondamento() {
        Pedido pedido = Pedido.criar(snapshot(List.of(item("A", 3, "19.99"), item("B", 7, "0.01")),
                List.of(desconto("1.11", AlvoDoDesconto.ITENS)), "12.34"), MOMENTO);
        assertThat(pedido.getSubtotal()).isEqualTo(Dinheiro.de("60.04"));
        assertThat(pedido.getTotalDescontos()).isEqualTo(Dinheiro.de("1.11"));
        assertThat(pedido.getFrete()).isEqualTo(Dinheiro.de("12.34"));
        assertThat(pedido.getTotal()).isEqualTo(Dinheiro.de("71.27"));
        assertThat(pedido.getTotal().valor().scale()).isEqualTo(2);
    }

    @Test
    void totalDeCadaItemEPrecoVezesQuantidade() {
        Pedido pedido = Pedido.criar(snapshot(List.of(item("A", 3, "19.99")), List.of(), "0"), MOMENTO);
        assertThat(pedido.getItens()).singleElement().satisfies(i -> {
            assertThat(i.getQuantidade()).isEqualTo(3);
            assertThat(i.getPrecoUnitario()).isEqualTo(Dinheiro.de("19.99"));
            assertThat(i.getTotalItem()).isEqualTo(Dinheiro.de("59.97"));
        });
    }

    @Test
    void valorMuitoGrandeNoLimiteDaPrecisaoEAceito() {
        Pedido pedido = Pedido.criar(snapshot(List.of(item("A", 1, "9999999999.99")), List.of(), "0"), MOMENTO);
        assertThat(pedido.getTotal()).isEqualTo(Dinheiro.de("9999999999.99"));
    }

    @Test
    void pedidoGratuitoPorDescontoIntegralEAceitoComTotalZero() {
        Pedido pedido = Pedido.criar(snapshot(List.of(item("A", 1, "50.00")),
                List.of(desconto("50.00", AlvoDoDesconto.ITENS)), "0"), MOMENTO);
        assertThat(pedido.getTotal().ehZero()).isTrue();
    }

    @Test
    void freteZeroEAceito() {
        Pedido pedido = Pedido.criar(snapshot(List.of(item("A", 1, "10.00")), List.of(), "0"), MOMENTO);
        assertThat(pedido.getFrete().ehZero()).isTrue();
        assertThat(pedido.getTotal()).isEqualTo(Dinheiro.de("10.00"));
    }

    @ParameterizedTest(name = "total {0} difere do esperado 70.00")
    @ValueSource(strings = {"70.01", "69.99", "0.00", "60.00", "85.00", "7.00"})
    void qualquerTotalDiferenteDoEsperadoRejeitaOSnapshot(String totalErrado) {
        DadosNovoPedido base = snapshot(List.of(item("A", 2, "30.00")), List.of(desconto("5.00", AlvoDoDesconto.ITENS)),
                "15.00");
        assertThat(base.total()).isEqualTo(Dinheiro.de("70.00"));
        DadosNovoPedido errado = PedidosParaTeste.comTotal(base, Dinheiro.de(totalErrado));
        assertThatThrownBy(() -> Pedido.criar(errado, MOMENTO)).isInstanceOf(DadoInvalidoException.class)
                .hasMessageContaining("Totais inconsistentes");
    }

    @Test
    void totalNegativoNaoDeveFormarPedido() {
        DadosNovoPedido negativo = snapshot(List.of(item("A", 1, "10.00")),
                List.of(desconto("50.00", AlvoDoDesconto.ITENS)), "5.00");
        assertThatThrownBy(() -> Pedido.criar(negativo, MOMENTO)).isInstanceOf(DadoInvalidoException.class);
    }

    // --- Itens invalidos ---

    @ParameterizedTest(name = "sku [{0}] invalido")
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t"})
    void itemSemSkuRejeitaOSnapshot(String sku) {
        DadosNovoPedido.Item item = new DadosNovoPedido.Item(sku, "Nome", null, 1, Dinheiro.de("1"));
        assertThatThrownBy(() -> Pedido.criar(snapshot(List.of(item), List.of(), "0"), MOMENTO))
                .isInstanceOf(DadoInvalidoException.class);
    }

    @ParameterizedTest(name = "nome [{0}] invalido")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void itemSemNomeRejeitaOSnapshot(String nome) {
        DadosNovoPedido.Item item = new DadosNovoPedido.Item("SKU", nome, null, 1, Dinheiro.de("1"));
        assertThatThrownBy(() -> Pedido.criar(snapshot(List.of(item), List.of(), "0"), MOMENTO))
                .isInstanceOf(DadoInvalidoException.class);
    }

    @Test
    void itemComPrecoNegativoRejeitaOSnapshot() {
        DadosNovoPedido.Item negativo = item("A", 1, "-1.00");
        DadosNovoPedido dados = new DadosNovoPedido(1L, ENDERECO, List.of(negativo), List.of(), Dinheiro.zero(),
                Dinheiro.de("-1.00"), Dinheiro.zero(), Dinheiro.de("-1.00"));
        assertThatThrownBy(() -> Pedido.criar(dados, MOMENTO)).isInstanceOf(DadoInvalidoException.class)
                .hasMessageContaining("preço");
    }

    @ParameterizedTest(name = "quantidade {0} invalida")
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    void quantidadeNaoPositivaRejeitaOSnapshot(int quantidade) {
        DadosNovoPedido.Item item = new DadosNovoPedido.Item("A", "Nome", null, quantidade, Dinheiro.de("1"));
        DadosNovoPedido dados = new DadosNovoPedido(1L, ENDERECO, List.of(item), List.of(), Dinheiro.zero(),
                Dinheiro.zero(), Dinheiro.zero(), Dinheiro.zero());
        assertThatThrownBy(() -> Pedido.criar(dados, MOMENTO)).isInstanceOf(DadoInvalidoException.class);
    }

    @Test
    void itemSemPrecoRejeitaOSnapshot() {
        DadosNovoPedido.Item item = new DadosNovoPedido.Item("A", "Nome", null, 1, null);
        DadosNovoPedido dados = new DadosNovoPedido(1L, ENDERECO, List.of(item), List.of(), Dinheiro.zero(),
                Dinheiro.zero(), Dinheiro.zero(), Dinheiro.zero());
        assertThatThrownBy(() -> Pedido.criar(dados, MOMENTO)).isInstanceOf(DadoInvalidoException.class);
    }

    // --- Descontos e frete invalidos ---

    @Test
    void descontoNegativoRejeitaOSnapshot() {
        DadosNovoPedido dados = new DadosNovoPedido(1L, ENDERECO, List.of(item("A", 1, "10.00")),
                List.of(desconto("-1.00", AlvoDoDesconto.ITENS)), Dinheiro.zero(), Dinheiro.de("10.00"),
                Dinheiro.de("-1.00"), Dinheiro.de("11.00"));
        assertThatThrownBy(() -> Pedido.criar(dados, MOMENTO)).isInstanceOf(DadoInvalidoException.class)
                .hasMessageContaining("Desconto");
    }

    @Test
    void descontoSemDescricaoOrigemOuAlvoRejeitaOSnapshot() {
        List<DadosNovoPedido.Desconto> invalidos = List.of(
                new DadosNovoPedido.Desconto(null, OrigemDesconto.PROMOCAO, AlvoDoDesconto.ITENS, Dinheiro.de("1")),
                new DadosNovoPedido.Desconto("  ", OrigemDesconto.PROMOCAO, AlvoDoDesconto.ITENS, Dinheiro.de("1")),
                new DadosNovoPedido.Desconto("X", null, AlvoDoDesconto.ITENS, Dinheiro.de("1")),
                new DadosNovoPedido.Desconto("X", OrigemDesconto.CUPOM, null, Dinheiro.de("1")),
                new DadosNovoPedido.Desconto("X", OrigemDesconto.CUPOM, AlvoDoDesconto.ITENS, null));
        for (DadosNovoPedido.Desconto invalido : invalidos) {
            DadosNovoPedido dados = new DadosNovoPedido(1L, ENDERECO, List.of(item("A", 1, "10.00")),
                    List.of(invalido), Dinheiro.zero(), Dinheiro.de("10.00"), Dinheiro.de("1.00"),
                    Dinheiro.de("9.00"));
            assertThatThrownBy(() -> Pedido.criar(dados, MOMENTO)).isInstanceOf(DadoInvalidoException.class);
        }
    }

    @Test
    void freteNegativoRejeitaOSnapshot() {
        DadosNovoPedido dados = new DadosNovoPedido(1L, ENDERECO, List.of(item("A", 1, "10.00")), List.of(),
                Dinheiro.de("-5.00"), Dinheiro.de("10.00"), Dinheiro.zero(), Dinheiro.de("5.00"));
        assertThatThrownBy(() -> Pedido.criar(dados, MOMENTO)).isInstanceOf(DadoInvalidoException.class)
                .hasMessageContaining("frete");
    }

    @Test
    void camposObrigatoriosNulosDoSnapshotLancamNullPointer() {
        assertThatThrownBy(() -> new DadosNovoPedido(null, ENDERECO, List.of(), List.of(), Dinheiro.zero(),
                Dinheiro.zero(), Dinheiro.zero(), Dinheiro.zero())).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new DadosNovoPedido(1L, null, List.of(), List.of(), Dinheiro.zero(),
                Dinheiro.zero(), Dinheiro.zero(), Dinheiro.zero())).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new DadosNovoPedido(1L, ENDERECO, null, List.of(), Dinheiro.zero(),
                Dinheiro.zero(), Dinheiro.zero(), Dinheiro.zero())).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new DadosNovoPedido(1L, ENDERECO, List.of(), List.of(), null,
                Dinheiro.zero(), Dinheiro.zero(), Dinheiro.zero())).isInstanceOf(NullPointerException.class);
    }

    @Test
    void descontosNulosSaoTratadosComoListaVazia() {
        DadosNovoPedido dados = new DadosNovoPedido(1L, ENDERECO, List.of(item("A", 1, "10.00")), null,
                Dinheiro.zero(), Dinheiro.de("10.00"), Dinheiro.zero(), Dinheiro.de("10.00"));
        assertThat(Pedido.criar(dados, MOMENTO).getDescontos()).isEmpty();
    }

    // --- Normalizacao e preservacao ---

    @Test
    void textosDoItemEDoDescontoSaoAparadosEDescricaoVazioViraNulo() {
        DadosNovoPedido.Item comEspacos = new DadosNovoPedido.Item("  SKU-1  ", "  Camiseta  ", "   ", 1,
                Dinheiro.de("10.00"));
        DadosNovoPedido.Desconto d = new DadosNovoPedido.Desconto("  Promo  ", OrigemDesconto.PROMOCAO,
                AlvoDoDesconto.ITENS, Dinheiro.de("1.00"));
        Pedido pedido = Pedido.criar(snapshot(List.of(comEspacos), List.of(d), "0"), MOMENTO);
        assertThat(pedido.getItens().getFirst().getSku()).isEqualTo("SKU-1");
        assertThat(pedido.getItens().getFirst().getNomeProduto()).isEqualTo("Camiseta");
        assertThat(pedido.getItens().getFirst().getDescricaoVariacao()).isNull();
        assertThat(pedido.getDescontos().getFirst().getDescricao()).isEqualTo("Promo");
    }

    @Test
    void identidadeEOrigemDoDescontoSaoPreservadas() {
        DadosNovoPedido.Desconto cupom = new DadosNovoPedido.Desconto("Cupom DEZ", OrigemDesconto.CUPOM,
                AlvoDoDesconto.ITENS, Dinheiro.de("2.50"), 42L, "DEZ");
        Pedido pedido = Pedido.criar(snapshot(List.of(item("A", 1, "25.00")), List.of(cupom), "0"), MOMENTO);
        DescontoPedido salvo = pedido.getDescontos().getFirst();
        assertThat(salvo.getOrigem()).isEqualTo(OrigemDesconto.CUPOM);
        assertThat(salvo.getAlvo()).isEqualTo(AlvoDoDesconto.ITENS);
        assertThat(salvo.getReferenciaId()).isEqualTo(42L);
        assertThat(salvo.getCodigo()).isEqualTo("DEZ");
        assertThat(salvo.getValor()).isEqualTo(Dinheiro.de("2.50"));
    }

    @Test
    void opcaoDeEntregaAusenteNoSnapshotLegadoPermaneceAusente() {
        Pedido pedido = Pedido.criar(PedidosParaTeste.validos(1L), MOMENTO);
        assertThat(pedido.getOpcaoEntrega()).isNull();
    }

    @Test
    void snapshotDeixaDeSerAfetadoPelaListaOriginalMutavel() {
        java.util.ArrayList<DadosNovoPedido.Item> itens = new java.util.ArrayList<>(List.of(item("A", 1, "10.00")));
        DadosNovoPedido dados = snapshot(itens, List.of(), "0");
        itens.add(item("B", 1, "99.00"));
        Pedido pedido = Pedido.criar(dados, MOMENTO);
        assertThat(pedido.getItens()).hasSize(1);
    }

    @Test
    void clienteEDataDeCriacaoSaoOsInformados() {
        Pedido pedido = Pedido.criar(PedidosParaTeste.validos(55L), MOMENTO);
        assertThat(pedido.getClienteId()).isEqualTo(55L);
        assertThat(pedido.getCriadoEm()).isEqualTo(MOMENTO);
        assertThat(pedido.getId()).isNull();
    }

    // --- Frete liquido derivado ---

    @Test
    void freteLiquidoSemDescontoDeFreteEOFreteBruto() {
        Pedido pedido = Pedido.criar(snapshot(List.of(item("A", 1, "100.00")),
                List.of(desconto("10.00", AlvoDoDesconto.ITENS)), "25.00"), MOMENTO);
        assertThat(pedido.getFreteLiquido()).isEqualTo(Dinheiro.de("25.00"));
    }

    @Test
    void freteLiquidoDescontaApenasDescontosDeFrete() {
        Pedido pedido = Pedido.criar(snapshot(List.of(item("A", 1, "100.00")),
                List.of(desconto("10.00", AlvoDoDesconto.ITENS), desconto("5.00", AlvoDoDesconto.FRETE),
                        desconto("3.00", AlvoDoDesconto.FRETE)), "25.00"), MOMENTO);
        assertThat(pedido.getFreteLiquido()).isEqualTo(Dinheiro.de("17.00"));
        assertThat(pedido.getFrete()).isEqualTo(Dinheiro.de("25.00"));
    }

    @Test
    void freteGratisZeraOFreteLiquidoSemFicarNegativo() {
        Pedido gratis = Pedido.criar(snapshot(List.of(item("A", 1, "100.00")),
                List.of(desconto("25.00", AlvoDoDesconto.FRETE)), "25.00"), MOMENTO);
        assertThat(gratis.getFreteLiquido().ehZero()).isTrue();
        assertThat(gratis.getTotal()).isEqualTo(Dinheiro.de("100.00"));
    }

    @Test
    void descontoDeFreteMaiorQueOFreteNaoGeraFreteLiquidoNegativo() {
        DadosNovoPedido dados = snapshot(List.of(item("A", 1, "100.00")),
                List.of(desconto("30.00", AlvoDoDesconto.FRETE)), "25.00");
        Pedido pedido = Pedido.criar(dados, MOMENTO);
        assertThat(pedido.getFreteLiquido().ehNegativo()).isFalse();
        assertThat(pedido.getFreteLiquido().ehZero()).isTrue();
    }

    // --- Endereco de entrega ---

    @ParameterizedTest(name = "cep [{0}] invalido")
    @NullAndEmptySource
    @ValueSource(strings = {"1234567", "123456789", "abcdefgh", "12345-67", "1301 000"})
    void cepForaDoFormatoRejeitaOEndereco(String cep) {
        assertThatThrownBy(() -> new EnderecoEntrega("Casa", cep, "Rua", "1", null, "Centro", "Campinas", "SP"))
                .isInstanceOf(DadoInvalidoException.class).hasMessageContaining("CEP");
    }

    @ParameterizedTest(name = "uf [{0}] invalida")
    @NullAndEmptySource
    @ValueSource(strings = {"S", "SPP", "S1", "1P", "  "})
    void ufForaDoFormatoRejeitaOEndereco(String uf) {
        assertThatThrownBy(() -> new EnderecoEntrega("Casa", "13010000", "Rua", "1", null, "Centro", "Campinas", uf))
                .isInstanceOf(DadoInvalidoException.class).hasMessageContaining("UF");
    }

    @ParameterizedTest(name = "campo obrigatorio {0}")
    @CsvSource({"logradouro", "numero", "bairro", "cidade"})
    void camposObrigatoriosDoEnderecoNaoAceitamVazio(String campo) {
        String logradouro = campo.equals("logradouro") ? " " : "Rua";
        String numero = campo.equals("numero") ? null : "1";
        String bairro = campo.equals("bairro") ? "" : "Centro";
        String cidade = campo.equals("cidade") ? "  " : "Campinas";
        assertThatThrownBy(() -> new EnderecoEntrega("Casa", "13010000", logradouro, numero, null, bairro, cidade, "SP"))
                .isInstanceOf(DadoInvalidoException.class);
    }

    @Test
    void enderecoNormalizaCepComHifenUfMinusculaEAparaTextos() {
        EnderecoEntrega e = new EnderecoEntrega("  ", "13010-000", "  Rua A  ", " 10 ", "  ", " Centro ", " Campinas ",
                " sp ");
        assertThat(e.getCep()).isEqualTo("13010000");
        assertThat(e.getUf()).isEqualTo("SP");
        assertThat(e.getApelido()).isNull();
        assertThat(e.getComplemento()).isNull();
        assertThat(e.getLogradouro()).isEqualTo("Rua A");
        assertThat(e.getNumero()).isEqualTo("10");
        assertThat(e.getBairro()).isEqualTo("Centro");
        assertThat(e.getCidade()).isEqualTo("Campinas");
    }

    @Test
    void apelidoEComplementoSaoOpcionais() {
        assertThatCode(() -> new EnderecoEntrega(null, "13010000", "Rua", "1", null, "Centro", "Campinas", "SP"))
                .doesNotThrowAnyException();
    }
}
