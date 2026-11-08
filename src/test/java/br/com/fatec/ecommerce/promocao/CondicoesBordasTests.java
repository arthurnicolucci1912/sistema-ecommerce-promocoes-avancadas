package br.com.fatec.ecommerce.promocao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fatec.ecommerce.compartilhado.dominio.DadoInvalidoException;
import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import br.com.fatec.ecommerce.promocao.dominio.CategoriaDoProduto;
import br.com.fatec.ecommerce.promocao.dominio.CondicaoConfig;
import br.com.fatec.ecommerce.promocao.dominio.CondicaoE;
import br.com.fatec.ecommerce.promocao.dominio.CondicaoElegibilidade;
import br.com.fatec.ecommerce.promocao.dominio.CondicaoNao;
import br.com.fatec.ecommerce.promocao.dominio.CondicaoOu;
import br.com.fatec.ecommerce.promocao.dominio.CondicaoRegistry;
import br.com.fatec.ecommerce.promocao.dominio.ContextoPreco;
import br.com.fatec.ecommerce.promocao.dominio.CriadorCondicao;
import br.com.fatec.ecommerce.promocao.dominio.CriadoresDeCondicao;
import br.com.fatec.ecommerce.promocao.dominio.ItemPorSku;
import br.com.fatec.ecommerce.promocao.dominio.ItemPreco;
import br.com.fatec.ecommerce.promocao.dominio.PerfilDoCliente;
import br.com.fatec.ecommerce.promocao.dominio.PeriodoDeVigencia;
import br.com.fatec.ecommerce.promocao.dominio.PrimeiraCompra;
import br.com.fatec.ecommerce.promocao.dominio.RegiaoDeEntrega;
import br.com.fatec.ecommerce.promocao.dominio.ValorMinimo;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Condicoes de elegibilidade (Composite/Specification): folhas, compostos, limites da arvore e extensao. */
class CondicoesBordasTests {

    private static final Instant AGORA = Instant.parse("2026-10-22T12:00:00Z");

    private final CondicaoRegistry registro = new CondicaoRegistry(CriadoresDeCondicao.padrao());

    private static ContextoPreco contexto(String uf, boolean primeira, String subtotal, Set<String> perfis,
            ItemPreco... itens) {
        return new ContextoPreco(List.of(itens), 1L, primeira, uf, Dinheiro.de(subtotal), AGORA, null, perfis,
                Dinheiro.zero());
    }

    private static ContextoPreco contexto() {
        return contexto("SP", true, "400", Set.of("CLIENTE"),
                new ItemPreco(1L, "LIVRO", Set.of(1L, 2L), 1, Dinheiro.de("100")));
    }

    private static CondicaoConfig folha(String tipo, Map<String, Object> parametros) {
        return new CondicaoConfig(tipo, parametros, List.of());
    }

    private static CondicaoConfig composta(String tipo, CondicaoConfig... filhos) {
        return new CondicaoConfig(tipo, null, List.of(filhos));
    }

    private static CondicaoConfig verdadeira() {
        return folha("PRIMEIRA_COMPRA", Map.of());
    }

    private boolean avalia(CondicaoConfig config, ContextoPreco contexto) {
        return registro.criar(config).avaliar(contexto);
    }

    // --- Folhas ---

    @ParameterizedTest(name = "subtotal {0} contra minimo {1} => {2}")
    @CsvSource({"299.99, 300, false", "300.00, 300, true", "300.01, 300, true", "0.00, 0, true", "0.00, 0.01, false"})
    void valorMinimoEhInclusivoNoLimite(String subtotal, String minimo, boolean esperado) {
        var ctx = contexto("SP", true, subtotal, Set.of());

        assertThat(new ValorMinimo(Dinheiro.de(minimo)).avaliar(ctx)).isEqualTo(esperado);
    }

    @Test
    void valorMinimoUsaOSubtotalDoContextoNaoOMomentoNemOsItens() {
        var ctx = contexto("SP", true, "50", Set.of(), new ItemPreco(1L, "A", Set.of(), 100, Dinheiro.de("100")));

        assertThat(new ValorMinimo(Dinheiro.de("100")).avaliar(ctx)).isFalse();
    }

    @Test
    void categoriaCasaPeloAncestralIncluidoNosItensENaoPelaCategoriaFilha() {
        var ctx = contexto();

        assertThat(new CategoriaDoProduto(1L).avaliar(ctx)).isTrue();
        assertThat(new CategoriaDoProduto(2L).avaliar(ctx)).isTrue();
        assertThat(new CategoriaDoProduto(3L).avaliar(ctx)).isFalse();
        assertThat(new CategoriaDoProduto(1L).avaliar(contexto("SP", true, "0", Set.of()))).isFalse();
    }

    @Test
    void skuEhComparadoExatamenteComCaixa() {
        var ctx = contexto();

        assertThat(new ItemPorSku("LIVRO").avaliar(ctx)).isTrue();
        assertThat(new ItemPorSku("livro").avaliar(ctx)).isFalse();
        assertThat(new ItemPorSku("LIVRO ").avaliar(ctx)).isFalse();
        assertThat(new ItemPorSku("LIVRO").avaliar(contexto("SP", true, "0", Set.of()))).isFalse();
    }

    @Test
    void regiaoRequerUfConhecidaEPertencenteAoConjunto() {
        var regiao = new RegiaoDeEntrega(Set.of("SP", "RJ"));

        assertThat(regiao.avaliar(contexto("SP", true, "0", Set.of()))).isTrue();
        assertThat(regiao.avaliar(contexto("RJ", true, "0", Set.of()))).isTrue();
        assertThat(regiao.avaliar(contexto("MG", true, "0", Set.of()))).isFalse();
        assertThat(regiao.avaliar(contexto(null, true, "0", Set.of()))).as("sem endereco, condicao de regiao e falsa")
                .isFalse();
        assertThat(regiao.avaliar(contexto("", true, "0", Set.of()))).isFalse();
    }

    @Test
    void regiaoCopiaOConjuntoDeUfs() {
        var origem = new java.util.HashSet<>(Set.of("SP"));
        var regiao = new RegiaoDeEntrega(origem);
        origem.add("RJ");

        assertThat(regiao.ufs()).containsExactly("SP");
        assertThat(regiao.avaliar(contexto("RJ", true, "0", Set.of()))).isFalse();
    }

    @Test
    void perfilExigeAAutoridadeExataNoContexto() {
        var ctx = contexto("SP", true, "0", Set.of("CLIENTE", "PROMOCOES"));

        assertThat(new PerfilDoCliente("CLIENTE").avaliar(ctx)).isTrue();
        assertThat(new PerfilDoCliente("cliente").avaliar(ctx)).isFalse();
        assertThat(new PerfilDoCliente("ADMIN").avaliar(ctx)).isFalse();
        assertThat(new PerfilDoCliente("CLIENTE").avaliar(contexto("SP", true, "0", Set.of()))).isFalse();
    }

    @Test
    void primeiraCompraLeSomenteODadoDoServidorNoContexto() {
        assertThat(new PrimeiraCompra().avaliar(contexto("SP", true, "0", Set.of()))).isTrue();
        assertThat(new PrimeiraCompra().avaliar(contexto("SP", false, "0", Set.of()))).isFalse();
    }

    @Test
    void periodoEhSemiAbertoInicioInclusivoFimExclusivo() {
        var periodo = new PeriodoDeVigencia(AGORA, AGORA.plusSeconds(10));
        ContextoPreco noInstante = contexto();
        var noFim = new ContextoPreco(List.of(), 1L, true, "SP", Dinheiro.zero(), AGORA.plusSeconds(10));
        var antes = new ContextoPreco(List.of(), 1L, true, "SP", Dinheiro.zero(), AGORA.minusNanos(1));
        var ultimoNano = new ContextoPreco(List.of(), 1L, true, "SP", Dinheiro.zero(), AGORA.plusSeconds(10).minusNanos(1));

        assertThat(periodo.avaliar(noInstante)).isTrue();
        assertThat(periodo.avaliar(ultimoNano)).isTrue();
        assertThat(periodo.avaliar(noFim)).isFalse();
        assertThat(periodo.avaliar(antes)).isFalse();
    }

    // --- Compostos ---

    @Test
    void compostoERequerTodosOsFilhosEPara() {
        int[] avaliacoes = {0};
        CondicaoElegibilidade falsa = c -> {
            avaliacoes[0]++;
            return false;
        };
        CondicaoElegibilidade nunca = c -> {
            avaliacoes[0] += 100;
            return true;
        };

        assertThat(new CondicaoE(List.of(falsa, nunca)).avaliar(contexto())).isFalse();
        assertThat(avaliacoes[0]).as("curto-circuito: o segundo filho nao e avaliado").isEqualTo(1);
    }

    @Test
    void compostoOuParaNoPrimeiroFilhoVerdadeiro() {
        int[] avaliacoes = {0};
        CondicaoElegibilidade verdadeira = c -> {
            avaliacoes[0]++;
            return true;
        };
        CondicaoElegibilidade nunca = c -> {
            avaliacoes[0] += 100;
            return false;
        };

        assertThat(new CondicaoOu(List.of(verdadeira, nunca)).avaliar(contexto())).isTrue();
        assertThat(avaliacoes[0]).isEqualTo(1);
    }

    @Test
    void compostosExigemAoMenosDoisFilhosENaoAceitamNulos() {
        CondicaoElegibilidade qualquer = c -> true;

        assertThatThrownBy(() -> new CondicaoE(List.of())).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> new CondicaoE(List.of(qualquer))).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> new CondicaoOu(List.of())).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> new CondicaoOu(List.of(qualquer))).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> new CondicaoNao(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void compostosCopiamAListaDeFilhos() {
        var filhos = new ArrayList<CondicaoElegibilidade>(List.of(c -> true, c -> true));
        var e = new CondicaoE(filhos);
        filhos.clear();

        assertThat(e.filhos()).hasSize(2);
        assertThat(e.avaliar(contexto())).isTrue();
    }

    @Test
    void naoPodeNegarOutroCompostoEDuplaNegacaoRestauraOValor() {
        CondicaoElegibilidade verdadeira = c -> true;
        CondicaoElegibilidade falsa = c -> false;

        assertThat(new CondicaoNao(new CondicaoE(List.of(verdadeira, falsa))).avaliar(contexto())).isTrue();
        assertThat(new CondicaoNao(new CondicaoOu(List.of(falsa, falsa))).avaliar(contexto())).isTrue();
        assertThat(new CondicaoNao(new CondicaoNao(verdadeira)).avaliar(contexto())).isTrue();
        assertThat(new CondicaoNao(new CondicaoNao(falsa)).avaliar(contexto())).isFalse();
    }

    @Test
    void leisDeDeMorganValemParaAsFolhasDeVerdade() {
        var ctx = contexto();
        CondicaoElegibilidade a = new CategoriaDoProduto(1L);
        CondicaoElegibilidade b = new ItemPorSku("OUTRO");

        var naoDoE = new CondicaoNao(new CondicaoE(List.of(a, b)));
        var ouDosNao = new CondicaoOu(List.of(new CondicaoNao(a), new CondicaoNao(b)));
        var naoDoOu = new CondicaoNao(new CondicaoOu(List.of(a, b)));
        var eDosNao = new CondicaoE(List.of(new CondicaoNao(a), new CondicaoNao(b)));

        assertThat(naoDoE.avaliar(ctx)).isEqualTo(ouDosNao.avaliar(ctx));
        assertThat(naoDoOu.avaliar(ctx)).isEqualTo(eDosNao.avaliar(ctx));
    }

    // --- Registro / compilacao ---

    @Test
    void configuracaoNulaEquivaleAElegibilidadeIrrestrita() {
        assertThat(registro.criar(null).avaliar(contexto())).isTrue();
        assertThat(registro.criar(null).avaliar(contexto(null, false, "0", Set.of()))).isTrue();
    }

    @Test
    void exemploComercialDoSpecValorMinimoEUfEmSpOuRjENaoPrimeiraCompra() {
        var condicao = composta("E",
                folha("VALOR_MINIMO", Map.of("minimo", 300)),
                composta("OU", folha("REGIAO", Map.of("ufs", List.of("SP"))), folha("REGIAO", Map.of("ufs", List.of("RJ")))),
                composta("NAO", verdadeira()));

        assertThat(avalia(condicao, contexto("SP", false, "300", Set.of()))).isTrue();
        assertThat(avalia(condicao, contexto("RJ", false, "500", Set.of()))).isTrue();
        assertThat(avalia(condicao, contexto("MG", false, "500", Set.of()))).as("UF fora").isFalse();
        assertThat(avalia(condicao, contexto("SP", false, "299.99", Set.of()))).as("abaixo do minimo").isFalse();
        assertThat(avalia(condicao, contexto("SP", true, "500", Set.of()))).as("primeira compra nega").isFalse();
        assertThat(avalia(condicao, contexto(null, false, "500", Set.of()))).as("sem endereco").isFalse();
    }

    @Test
    void ufsDaCondicaoSaoNormalizadasParaMaiusculasEAparadas() {
        var condicao = folha("REGIAO", Map.of("ufs", List.of(" sp ", "rj")));

        assertThat(avalia(condicao, contexto("SP", true, "0", Set.of()))).isTrue();
        assertThat(avalia(condicao, contexto("RJ", true, "0", Set.of()))).isTrue();
    }

    @Test
    void ufsRepetidasNaCondicaoSaoToleradas() {
        var condicao = folha("REGIAO", Map.of("ufs", List.of("SP", "sp", "SP")));

        assertThat(avalia(condicao, contexto("SP", true, "0", Set.of()))).isTrue();
    }

    @Test
    void parametrosInvalidosDasFolhasSaoRejeitadosComoDadoInvalido() {
        List<CondicaoConfig> invalidas = List.of(
                folha("VALOR_MINIMO", Map.of()),
                folha("VALOR_MINIMO", Map.of("minimo", -1)),
                folha("VALOR_MINIMO", Map.of("minimo", "abc")),
                folha("VALOR_MINIMO", Map.of("minimo", "10.001")),
                folha("CATEGORIA", Map.of()),
                folha("CATEGORIA", Map.of("categoriaId", 0)),
                folha("CATEGORIA", Map.of("categoriaId", -4)),
                folha("CATEGORIA", Map.of("categoriaId", "x")),
                folha("SKU", Map.of()),
                folha("SKU", Map.of("sku", " ")),
                folha("SKU", Map.of("sku", 123)),
                folha("REGIAO", Map.of()),
                folha("REGIAO", Map.of("ufs", List.of())),
                folha("REGIAO", Map.of("ufs", "SP")),
                folha("REGIAO", Map.of("ufs", List.of("XX"))),
                folha("REGIAO", Map.of("ufs", List.of("SP", "ZZ"))),
                folha("REGIAO", Map.of("ufs", List.of(12))),
                folha("PERFIL", Map.of()),
                folha("PERFIL", Map.of("perfil", "")),
                folha("PERIODO", Map.of()),
                folha("PERIODO", Map.of("inicio", "2026-01-01T00:00:00Z")),
                folha("PERIODO", Map.of("inicio", "ontem", "fim", "amanha")),
                folha("PERIODO", Map.of("inicio", "2026-01-02T00:00:00Z", "fim", "2026-01-01T00:00:00Z")),
                folha("PERIODO", Map.of("inicio", "2026-01-01T00:00:00Z", "fim", "2026-01-01T00:00:00Z")),
                folha("PERIODO", Map.of("inicio", "2026-01-01", "fim", "2026-01-02")));
        for (CondicaoConfig invalida : invalidas) {
            assertThatThrownBy(() -> registro.criar(invalida)).as("%s %s", invalida.tipo(), invalida.parametros())
                    .isInstanceOf(DadoInvalidoException.class);
        }
    }

    @Test
    void parametrosValidosDasFolhasSaoAceitosEmCaseDeBorda() {
        for (CondicaoConfig valida : List.of(
                folha("VALOR_MINIMO", Map.of("minimo", 0)),
                folha("VALOR_MINIMO", Map.of("minimo", "0.01")),
                folha("CATEGORIA", Map.of("categoriaId", "5")),
                folha("SKU", Map.of("sku", "  A  ")),
                folha("REGIAO", Map.of("ufs", List.of("DF"))),
                folha("PERFIL", Map.of("perfil", "X")),
                folha("PERIODO", Map.of("inicio", "2026-01-01T00:00:00Z", "fim", "2026-01-01T00:00:01Z")))) {
            assertThat(registro.criar(valida)).as(valida.tipo()).isNotNull();
        }
    }

    @Test
    void parametroDeSkuEhAparadoNaCriacaoDaFolha() {
        var condicao = registro.criar(folha("SKU", Map.of("sku", "  LIVRO  ")));

        assertThat(condicao.avaliar(contexto())).isTrue();
    }

    @Test
    void folhaComFilhosEhRejeitadaECompostoSemFilhosTambem() {
        assertThatThrownBy(() -> registro.criar(composta("PRIMEIRA_COMPRA", verdadeira())))
                .isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> registro.criar(composta("SKU", folha("SKU", Map.of("sku", "A")))))
                .isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> registro.criar(composta("E"))).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> registro.criar(composta("E", verdadeira()))).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> registro.criar(composta("OU", verdadeira()))).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> registro.criar(composta("OU"))).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> registro.criar(composta("NAO"))).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> registro.criar(composta("NAO", verdadeira(), verdadeira())))
                .isInstanceOf(DadoInvalidoException.class);
    }

    @Test
    void condicaoSemTipoDeveSerDadoInvalidoENaoNullPointer() {
        assertThatThrownBy(() -> registro.criar(folha(null, Map.of()))).isInstanceOf(DadoInvalidoException.class);
    }

    @Test
    void tipoDesconhecidoOuVazioOuComCaixaDiferenteEhRejeitado() {
        assertThatThrownBy(() -> registro.criar(folha("", Map.of()))).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> registro.criar(folha("primeira_compra", Map.of())))
                .isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> registro.criar(composta("NAO", folha("INEXISTENTE", Map.of()))))
                .isInstanceOf(DadoInvalidoException.class);
    }

    // --- Limites da arvore ---

    private static CondicaoConfig cadeiaDeNao(int niveis) {
        CondicaoConfig atual = verdadeira();
        for (int i = 1; i < niveis; i++) {
            atual = composta("NAO", atual);
        }
        return atual;
    }

    @Test
    void profundidadeDeDezesseisNiveisEhAceitaEDezesseteNao() {
        assertThat(registro.criar(cadeiaDeNao(16))).isNotNull();
        assertThatThrownBy(() -> registro.criar(cadeiaDeNao(17))).isInstanceOf(DadoInvalidoException.class);
    }

    @Test
    void cadeiaDeNegacaoAlternaOValorAoLongoDosNiveis() {
        // Folha verdadeira; cada NAO inverte: com 16 niveis ha 15 negacoes => falso.
        assertThat(avalia(cadeiaDeNao(16), contexto())).isFalse();
        assertThat(avalia(cadeiaDeNao(15), contexto())).isTrue();
        assertThat(avalia(cadeiaDeNao(1), contexto())).isTrue();
    }

    @Test
    void tamanhoDe128NosEhAceitoE129Nao() {
        var folhas127 = Collections.nCopies(127, verdadeira());
        var folhas128 = Collections.nCopies(128, verdadeira());

        assertThat(registro.criar(new CondicaoConfig("E", null, folhas127)).avaliar(contexto())).isTrue();
        assertThatThrownBy(() -> registro.criar(new CondicaoConfig("E", null, folhas128)))
                .isInstanceOf(DadoInvalidoException.class);
    }

    @Test
    void ocontadorDeNosEPorChamadaEntaoCompilarDuasVezesNaoAcumula() {
        var arvore = new CondicaoConfig("OU", null, Collections.nCopies(100, verdadeira()));

        assertThat(registro.criar(arvore)).isNotNull();
        assertThat(registro.criar(arvore)).isNotNull();
    }

    @Test
    void arvoreLargaEProfundaQueExcedeNosDispararAntesDeConstruirOsObjetos() {
        var criados = new AtomicInteger();
        var contador = new CriadoresDeCondicao.Criador("CONTA", List.of(), (p, f) -> {
            criados.incrementAndGet();
            return c -> true;
        });
        var lista = new ArrayList<CriadorCondicao>(CriadoresDeCondicao.padrao());
        lista.add(contador);
        var comContador = new CondicaoRegistry(lista);
        var excesso = new CondicaoConfig("E", null, Collections.nCopies(200, folha("CONTA", Map.of())));

        assertThatThrownBy(() -> comContador.criar(excesso)).isInstanceOf(DadoInvalidoException.class);
        assertThat(criados.get()).as("so ate o 128o no e visitado").isLessThanOrEqualTo(128);
    }

    // --- Extensao ---

    @Test
    void novaFolhaRegistradaFuncionaDentroDeCompostosExistentesSemAlteralos() {
        var lista = new ArrayList<CriadorCondicao>(CriadoresDeCondicao.padrao());
        lista.add(new CriadoresDeCondicao.Criador("PAR", List.of(),
                (p, f) -> c -> c.subtotal().valor().intValue() % 2 == 0));
        var estendido = new CondicaoRegistry(lista);
        var condicao = composta("NAO", composta("OU", folha("PAR", Map.of()), folha("PRIMEIRA_COMPRA", Map.of())));

        assertThat(estendido.criar(condicao).avaliar(contexto("SP", false, "401", Set.of()))).isTrue();
        assertThat(estendido.criar(condicao).avaliar(contexto("SP", false, "400", Set.of()))).isFalse();
        assertThat(estendido.tipos()).containsKey("PAR");
        assertThat(registro.tipos()).doesNotContainKey("PAR");
    }

    @Test
    void tipoDeCondicaoDuplicadoEhRejeitadoNaConstrucao() {
        var duplicada = new ArrayList<CriadorCondicao>(CriadoresDeCondicao.padrao());
        duplicada.add(new CriadoresDeCondicao.Criador("SKU", List.of(), (p, f) -> c -> true));

        assertThatThrownBy(() -> new CondicaoRegistry(duplicada)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SKU");
    }

    @Test
    void registroListaOsDezTiposPadraoComSeusParametros() {
        Map<String, List<String>> tipos = registro.tipos();

        assertThat(tipos).containsOnlyKeys("PRIMEIRA_COMPRA", "VALOR_MINIMO", "CATEGORIA", "SKU", "REGIAO", "PERFIL",
                "PERIODO", "E", "OU", "NAO");
        assertThat(tipos.get("VALOR_MINIMO")).containsExactly("minimo");
        assertThat(tipos.get("PERIODO")).containsExactly("inicio", "fim");
        assertThat(tipos.get("E")).isEmpty();
        assertThatThrownBy(() -> tipos.put("X", List.of())).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void parametrosMutaveisDeEntradaNaoAlteramACondicaoJaCompilada() {
        Map<String, Object> parametros = new HashMap<>(Map.of("sku", "LIVRO"));
        var condicao = registro.criar(new CondicaoConfig("SKU", parametros, List.of()));
        parametros.put("sku", "OUTRO");

        assertThat(condicao.avaliar(contexto())).isTrue();
    }
}
