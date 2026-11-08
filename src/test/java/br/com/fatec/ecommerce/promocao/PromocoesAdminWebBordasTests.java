package br.com.fatec.ecommerce.promocao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.fatec.ecommerce.TesteDeIntegracao;
import br.com.fatec.ecommerce.catalogo.CatalogoPersistidoParaTeste;
import br.com.fatec.ecommerce.cliente.ClientesParaTeste;
import br.com.fatec.ecommerce.cliente.ClientesParaTeste.ClienteDeTeste;
import br.com.fatec.ecommerce.seguranca.UsuariosParaTeste;
import br.com.fatec.ecommerce.seguranca.dominio.Perfil;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Bordas da administracao de campanhas e cupons e do efeito delas no carrinho: autorizacao por rota, validacao
 * (400 sem gravacao parcial), 404, 409 de versao, vigencia, conflitos entre campanhas e arredondamento.
 */
@TesteDeIntegracao
@Import(CatalogoPersistidoParaTeste.class)
class PromocoesAdminWebBordasTests {

    static final String CAR = "/api/clientes/me/carrinho";

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;
    @Autowired
    UsuariosParaTeste usuarios;
    @Autowired
    ClientesParaTeste clientes;
    @Autowired
    CatalogoPersistidoParaTeste catalogo;
    @Autowired
    JdbcTemplate jdbc;

    String admin;
    String promotor;
    ClienteDeTeste cliente;
    Long livrosId;
    Long literaturaId;

    @BeforeEach
    void preparar() {
        usuarios.limpar();
        cliente = clientes.criar("comprador@bordas.local", 801);
        admin = usuarios.bearer(usuarios.criar("admin@bordas.local", Perfil.ADMIN));
        promotor = usuarios.bearer(usuarios.criar("promo@bordas.local", Perfil.PROMOCOES));
        var literatura = catalogo.categoria("Literatura", null);
        var livros = catalogo.categoria("Livros", literatura);
        var tecnologia = catalogo.categoria("Tecnologia", null);
        literaturaId = literatura.getId();
        livrosId = livros.getId();
        catalogo.variacao(livros, "LIVRO", "100");
        catalogo.variacao(tecnologia, "TECLADO", "200");
        jdbc.update("UPDATE saldo_estoque SET disponivel=1000");
    }

    // --- Apoio ---

    MockHttpServletRequestBuilder requisicao(String metodo, String url, String token, Object corpo) throws Exception {
        var builder = switch (metodo) {
            case "POST" -> post(url);
            case "PUT" -> put(url);
            default -> get(url);
        };
        if (token != null) {
            builder.header("Authorization", token);
        }
        if (corpo != null) {
            builder.contentType(MediaType.APPLICATION_JSON)
                    .content(corpo instanceof String texto ? texto : json.writeValueAsString(corpo));
        }
        return builder;
    }

    int codigo(String metodo, String url, String token, Object corpo) throws Exception {
        return mvc.perform(requisicao(metodo, url, token, corpo)).andReturn().getResponse().getStatus();
    }

    ResultActions enviar(String metodo, String url, String token, Object corpo) throws Exception {
        return mvc.perform(requisicao(metodo, url, token, corpo));
    }

    JsonNode node(ResultActions resultado) throws Exception {
        return json.readTree(resultado.andReturn().getResponse().getContentAsString());
    }

    Map<String, Object> campanha(String tipo, Map<String, Object> parametros) {
        var mapa = new HashMap<String, Object>(Map.of("nome", "Campanha " + tipo, "tipo", tipo, "inicio",
                Instant.now().minusSeconds(60).toString(), "fim", Instant.now().plusSeconds(3600).toString(),
                "prioridade", 10, "modo", "ACUMULAVEL"));
        if (parametros != null) {
            mapa.put("parametros", parametros);
        }
        return mapa;
    }

    JsonNode criarCampanha(Map<String, Object> dados) throws Exception {
        return node(enviar("POST", "/api/admin/promocoes", promotor, dados).andExpect(status().isCreated()));
    }

    JsonNode criarCampanha(String tipo, Map<String, Object> parametros) throws Exception {
        return criarCampanha(campanha(tipo, parametros));
    }

    JsonNode fixa(int valor, int prioridade, String modo) throws Exception {
        var dados = campanha("VALOR_FIXO_ACIMA_DE", Map.of("minimo", 0, "valor", valor));
        dados.put("prioridade", prioridade);
        dados.put("modo", modo);
        return criarCampanha(dados);
    }

    Map<String, Object> cupomDados(String codigo, String tipo, Map<String, Object> parametros) {
        return new HashMap<>(Map.of("codigo", codigo, "nome", "Cupom " + codigo, "tipo", tipo, "inicio",
                Instant.now().minusSeconds(60).toString(), "fim", Instant.now().plusSeconds(3600).toString(),
                "acumulavel", true, "limiteTotal", 10, "parametros", parametros));
    }

    JsonNode criarCupom(String codigo, String tipo, Map<String, Object> parametros) throws Exception {
        return node(enviar("POST", "/api/admin/cupons", promotor, cupomDados(codigo, tipo, parametros))
                .andExpect(status().isCreated()));
    }

    void incluir(String sku, int quantidade) throws Exception {
        enviar("POST", CAR + "/itens", cliente.bearer(), Map.of("sku", sku, "quantidade", quantidade))
                .andExpect(status().isOk());
    }

    JsonNode carrinho() throws Exception {
        return node(mvc.perform(get(CAR).header("Authorization", cliente.bearer())).andExpect(status().isOk()));
    }

    int contar(String tabela) {
        return jdbc.queryForObject("SELECT count(*) FROM " + tabela, Integer.class);
    }

    // --- Autorizacao ---

    static final List<String[]> ROTAS = List.of(
            new String[] {"GET", "", "-"}, new String[] {"GET", "/1", "-"}, new String[] {"GET", "/tipos", "-"},
            new String[] {"GET", "/condicoes", "-"}, new String[] {"POST", "", "{}"},
            new String[] {"PUT", "/1", "{}"}, new String[] {"POST", "/1/ativacao", "{\"versao\":0}"},
            new String[] {"POST", "/1/inativacao", "{\"versao\":0}"});

    @Test
    void semTokenTodaRotaAdministrativaDePromocoesECuponsRecebe401() throws Exception {
        for (String area : List.of("promocoes", "cupons")) {
            for (String[] rota : ROTAS) {
                Object corpo = rota[2].equals("-") ? null : rota[2];
                assertThat(codigo(rota[0], "/api/admin/" + area + rota[1], null, corpo))
                        .as("%s /api/admin/%s%s", rota[0], area, rota[1]).isEqualTo(401);
                assertThat(codigo(rota[0], "/api/admin/" + area + rota[1], "Bearer invalido", corpo))
                        .as("token invalido %s /api/admin/%s%s", rota[0], area, rota[1]).isEqualTo(401);
            }
        }
    }

    @ParameterizedTest
    @EnumSource(value = Perfil.class, names = {"CLIENTE", "ESTOQUE", "OPERADOR_COMERCIAL"})
    void perfisSemPermissaoRecebem403EmTodasAsRotas(Perfil perfil) throws Exception {
        String token = usuarios.bearer(usuarios.criar("x-" + perfil + "@bordas.local", perfil));

        for (String area : List.of("promocoes", "cupons")) {
            for (String[] rota : ROTAS) {
                Object corpo = rota[2].equals("-") ? null : rota[2];
                assertThat(codigo(rota[0], "/api/admin/" + area + rota[1], token, corpo))
                        .as("%s /api/admin/%s%s com %s", rota[0], area, rota[1], perfil).isEqualTo(403);
            }
        }
    }

    @Test
    void adminEPromocoesPodemListarETemOMesmoAcesso() throws Exception {
        for (String token : List.of(admin, promotor)) {
            assertThat(codigo("GET", "/api/admin/promocoes", token, null)).isEqualTo(200);
            assertThat(codigo("GET", "/api/admin/cupons", token, null)).isEqualTo(200);
            assertThat(codigo("GET", "/api/admin/promocoes/tipos", token, null)).isEqualTo(200);
            assertThat(codigo("GET", "/api/admin/cupons/condicoes", token, null)).isEqualTo(200);
        }
    }

    @Test
    void autorizacaoVemAntesDaValidacaoEDaExistenciaDoRecurso() throws Exception {
        String estoque = usuarios.bearer(usuarios.criar("estoque@bordas.local", Perfil.ESTOQUE));

        assertThat(codigo("PUT", "/api/admin/promocoes/999999", estoque, "{}")).isEqualTo(403);
        assertThat(codigo("PUT", "/api/admin/promocoes/999999", null, "{}")).isEqualTo(401);
        assertThat(codigo("PUT", "/api/admin/promocoes/999999", promotor, campanha("FRETE_GRATIS", null)))
                .isEqualTo(404);
    }

    // --- Tipos e condicoes publicados ---

    @Test
    void tiposECondicoesDisponiveisSaoListadosComSeusParametros() throws Exception {
        enviar("GET", "/api/admin/promocoes/tipos", promotor, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.LEVE_X_PAGUE_Y[0]").value("sku"))
                .andExpect(jsonPath("$.PERCENTUAL_CATEGORIA[0]").value("categoriaId"))
                .andExpect(jsonPath("$.FRETE_GRATIS", hasSize(0)));
        enviar("GET", "/api/admin/promocoes/condicoes", promotor, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.PRIMEIRA_COMPRA").exists()).andExpect(jsonPath("$.REGIAO[0]").value("ufs"))
                .andExpect(jsonPath("$.E").exists()).andExpect(jsonPath("$.OU").exists())
                .andExpect(jsonPath("$.NAO").exists());
        enviar("GET", "/api/admin/cupons/tipos", promotor, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.PERCENTUAL[0]").value("percentual"))
                .andExpect(jsonPath("$.VALOR_FIXO[0]").value("valor"))
                .andExpect(jsonPath("$.FRETE_GRATIS", hasSize(0)))
                .andExpect(jsonPath("$.REDUCAO_FRETE[0]").value("valor"));
    }

    // --- Validacao de campanhas ---

    @Test
    void campanhaComCamposObrigatoriosAusentesOuInvalidosEh400SemGravacao() throws Exception {
        List<Map<String, Object>> invalidas = new ArrayList<>();
        for (String campo : List.of("nome", "tipo", "inicio", "fim", "modo")) {
            var semCampo = campanha("FRETE_GRATIS", null);
            semCampo.remove(campo);
            invalidas.add(semCampo);
        }
        var nomeVazio = campanha("FRETE_GRATIS", null);
        nomeVazio.put("nome", "  ");
        invalidas.add(nomeVazio);
        var nomeLongo = campanha("FRETE_GRATIS", null);
        nomeLongo.put("nome", "n".repeat(256));
        invalidas.add(nomeLongo);
        var periodoInvertido = campanha("FRETE_GRATIS", null);
        periodoInvertido.put("inicio", Instant.now().plusSeconds(100).toString());
        periodoInvertido.put("fim", Instant.now().minusSeconds(100).toString());
        invalidas.add(periodoInvertido);
        var periodoNulo = campanha("FRETE_GRATIS", null);
        var instante = Instant.parse("2026-12-01T10:00:00Z").toString();
        periodoNulo.put("inicio", instante);
        periodoNulo.put("fim", instante);
        invalidas.add(periodoNulo);
        var tipoVazio = campanha("FRETE_GRATIS", null);
        tipoVazio.put("tipo", "");
        invalidas.add(tipoVazio);
        invalidas.add(campanha("TIPO_DESCONHECIDO", Map.of()));
        invalidas.add(campanha("frete_gratis", Map.of()));
        var datasTexto = campanha("FRETE_GRATIS", null);
        datasTexto.put("inicio", "amanha");
        invalidas.add(datasTexto);

        for (var dados : invalidas) {
            assertThat(codigo("POST", "/api/admin/promocoes", promotor, dados)).as("%s", dados).isEqualTo(400);
        }

        assertThat(contar("promocao_config")).isZero();
    }

    @Test
    void campanhaComModoOuPrioridadeDeTipoErradoEh400ComMensagemUtil() throws Exception {
        var modoInvalido = campanha("FRETE_GRATIS", null);
        modoInvalido.put("modo", "TALVEZ");
        enviar("POST", "/api/admin/promocoes", promotor, modoInvalido).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("ACUMULAVEL")));
        var prioridadeInvalida = campanha("FRETE_GRATIS", null);
        prioridadeInvalida.put("prioridade", "alta");
        assertThat(codigo("POST", "/api/admin/promocoes", promotor, prioridadeInvalida)).isEqualTo(400);
        var modoMinusculo = campanha("FRETE_GRATIS", null);
        modoMinusculo.put("modo", "acumulavel");
        assertThat(codigo("POST", "/api/admin/promocoes", promotor, modoMinusculo)).isEqualTo(400);
        assertThat(contar("promocao_config")).isZero();
    }

    @Test
    void parametrosInvalidosDeCadaTipoDeCampanhaSao400SemGravacao() throws Exception {
        List<Object[]> casos = List.of(
                new Object[] {"PERCENTUAL_PRIMEIRA_COMPRA", Map.of("percentual", 0)},
                new Object[] {"PERCENTUAL_PRIMEIRA_COMPRA", Map.of("percentual", -5)},
                new Object[] {"PERCENTUAL_PRIMEIRA_COMPRA", Map.of("percentual", 100.01)},
                new Object[] {"PERCENTUAL_PRIMEIRA_COMPRA", Map.of("percentual", "abc")},
                new Object[] {"PERCENTUAL_PRIMEIRA_COMPRA", Map.of()},
                new Object[] {"PERCENTUAL_CATEGORIA", Map.of("percentual", 10)},
                new Object[] {"PERCENTUAL_CATEGORIA", Map.of("categoriaId", livrosId)},
                new Object[] {"PERCENTUAL_CATEGORIA", Map.of("categoriaId", 0, "percentual", 10)},
                new Object[] {"PERCENTUAL_CATEGORIA", Map.of("categoriaId", 999999, "percentual", 10)},
                new Object[] {"LEVE_X_PAGUE_Y", Map.of("sku", "LIVRO", "leve", 2, "pague", 3)},
                new Object[] {"LEVE_X_PAGUE_Y", Map.of("sku", "LIVRO", "leve", 3, "pague", 3)},
                new Object[] {"LEVE_X_PAGUE_Y", Map.of("sku", "LIVRO", "leve", 3, "pague", 0)},
                new Object[] {"LEVE_X_PAGUE_Y", Map.of("sku", "LIVRO", "leve", 1.5, "pague", 1)},
                new Object[] {"LEVE_X_PAGUE_Y", Map.of("sku", "LIVRO", "leve", 3)},
                new Object[] {"LEVE_X_PAGUE_Y", Map.of("leve", 3, "pague", 2)},
                new Object[] {"LEVE_X_PAGUE_Y", Map.of("sku", "NAO-EXISTE", "leve", 3, "pague", 2)},
                new Object[] {"VALOR_FIXO_ACIMA_DE", Map.of("minimo", 0, "valor", 0)},
                new Object[] {"VALOR_FIXO_ACIMA_DE", Map.of("minimo", 0, "valor", -1)},
                new Object[] {"VALOR_FIXO_ACIMA_DE", Map.of("minimo", 0, "valor", 1.234)},
                new Object[] {"VALOR_FIXO_ACIMA_DE", Map.of("minimo", -1, "valor", 5)},
                new Object[] {"VALOR_FIXO_ACIMA_DE", Map.of("valor", 5)},
                new Object[] {"VALOR_FIXO_ACIMA_DE", Map.of("minimo", 5)});

        for (Object[] caso : casos) {
            @SuppressWarnings("unchecked")
            var parametros = (Map<String, Object>) caso[1];
            assertThat(codigo("POST", "/api/admin/promocoes", promotor, campanha((String) caso[0], parametros)))
                    .as("%s %s", caso[0], parametros).isEqualTo(400);
        }

        assertThat(contar("promocao_config")).isZero();
    }

    @Test
    void parametrosNoLimiteDeValidacaoSaoAceitos() throws Exception {
        criarCampanha("PERCENTUAL_PRIMEIRA_COMPRA", Map.of("percentual", 100));
        criarCampanha("PERCENTUAL_PRIMEIRA_COMPRA", Map.of("percentual", 0.01));
        criarCampanha("LEVE_X_PAGUE_Y", Map.of("sku", "LIVRO", "leve", 2, "pague", 1));
        criarCampanha("VALOR_FIXO_ACIMA_DE", Map.of("minimo", 0, "valor", 0.01));
        criarCampanha("PERCENTUAL_CATEGORIA", Map.of("categoriaId", livrosId, "percentual", "12.5"));
        var nomeMaximo = campanha("FRETE_GRATIS", null);
        nomeMaximo.put("nome", "n".repeat(255));
        criarCampanha(nomeMaximo);

        assertThat(contar("promocao_config")).isEqualTo(6);
    }

    @Test
    void campanhaSemParametrosOuComParametrosVaziosParaTipoSemParametrosEhAceita() throws Exception {
        var semParametros = criarCampanha("FRETE_GRATIS", null);
        var comVazio = criarCampanha("FRETE_GRATIS", Map.of());

        assertThat(semParametros.get("parametros").size()).isZero();
        assertThat(comVazio.get("versao").asLong()).isZero();
        assertThat(semParametros.get("ativo").asBoolean()).isTrue();
    }

    @Test
    void campanhaCriadaDevolve201LocationVersaoZeroEDadosNormalizados() throws Exception {
        var dados = campanha("PERCENTUAL_PRIMEIRA_COMPRA", Map.of("percentual", 10));
        dados.put("nome", "  Black Friday  ");

        enviar("POST", "/api/admin/promocoes", promotor, dados).andExpect(status().isCreated())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("Location", org.hamcrest.Matchers.startsWith("/api/admin/promocoes/")))
                .andExpect(jsonPath("$.nome").value("Black Friday")).andExpect(jsonPath("$.versao").value(0))
                .andExpect(jsonPath("$.ativo").value(true)).andExpect(jsonPath("$.modo").value("ACUMULAVEL"))
                .andExpect(jsonPath("$.criadoEm").exists());
    }

    @Test
    void condicoesInvalidasNaCampanhaSao400SemGravacao() throws Exception {
        CondicaoJson profunda = CondicaoJson.folha("PRIMEIRA_COMPRA");
        for (int i = 0; i < 16; i++) {
            profunda = CondicaoJson.no("NAO", profunda);
        }
        var larga = new CondicaoJson("E", null, Collections.nCopies(129, CondicaoJson.folha("PRIMEIRA_COMPRA")));
        List<Object> invalidas = List.of(
                CondicaoJson.folha("DESCONHECIDA"),
                CondicaoJson.no("E", CondicaoJson.folha("PRIMEIRA_COMPRA")),
                CondicaoJson.no("OU"),
                CondicaoJson.no("NAO", CondicaoJson.folha("PRIMEIRA_COMPRA"), CondicaoJson.folha("PRIMEIRA_COMPRA")),
                CondicaoJson.no("NAO"),
                new CondicaoJson("REGIAO", Map.of("ufs", List.of("XX")), null),
                new CondicaoJson("REGIAO", Map.of("ufs", List.of()), null),
                new CondicaoJson("VALOR_MINIMO", Map.of("minimo", -10), null),
                new CondicaoJson("CATEGORIA", Map.of("categoriaId", 999999), null),
                new CondicaoJson("SKU", Map.of("sku", "NAO-EXISTE"), null),
                new CondicaoJson("PERIODO", Map.of("inicio", "2026-02-01T00:00:00Z", "fim", "2026-01-01T00:00:00Z"), null),
                CondicaoJson.no("E", CondicaoJson.folha("PRIMEIRA_COMPRA"),
                        new CondicaoJson("CATEGORIA", Map.of("categoriaId", 999999), null)),
                profunda, larga);

        for (Object condicao : invalidas) {
            var dados = campanha("FRETE_GRATIS", null);
            dados.put("condicao", condicao);
            assertThat(codigo("POST", "/api/admin/promocoes", promotor, dados)).as("%s", condicao).isEqualTo(400);
            var cupom = cupomDados("COND", "PERCENTUAL", Map.of("percentual", 10));
            cupom.put("condicao", condicao);
            assertThat(codigo("POST", "/api/admin/cupons", promotor, cupom)).as("cupom %s", condicao).isEqualTo(400);
        }

        assertThat(contar("promocao_config")).isZero();
        assertThat(contar("cupom")).isZero();
    }

    record CondicaoJson(String tipo, Map<String, Object> parametros, List<CondicaoJson> filhos) {
        static CondicaoJson folha(String tipo) {
            return new CondicaoJson(tipo, null, null);
        }

        static CondicaoJson no(String tipo, CondicaoJson... filhos) {
            return new CondicaoJson(tipo, null, List.of(filhos));
        }
    }

    @Test
    void condicaoValidaComSkuECategoriaExistentesEhAceita() throws Exception {
        var dados = campanha("FRETE_GRATIS", null);
        dados.put("condicao", CondicaoJson.no("E",
                new CondicaoJson("CATEGORIA", Map.of("categoriaId", livrosId), null),
                CondicaoJson.no("NAO", new CondicaoJson("SKU", Map.of("sku", "TECLADO"), null))));

        criarCampanha(dados);

        assertThat(contar("promocao_config")).isEqualTo(1);
    }

    @Test
    void condicaoComArvoreNoLimiteDeProfundidadeEAceita() throws Exception {
        CondicaoJson arvore = CondicaoJson.folha("PRIMEIRA_COMPRA");
        for (int i = 0; i < 15; i++) {
            arvore = CondicaoJson.no("NAO", arvore);
        }
        var dados = campanha("FRETE_GRATIS", null);
        dados.put("condicao", arvore);

        criarCampanha(dados);
    }

    @Test
    void parametroComValorNuloDeveSer400() throws Exception {
        var parametros = new HashMap<String, Object>();
        parametros.put("sku", null);

        assertThat(codigo("POST", "/api/admin/promocoes", promotor, campanha("LEVE_X_PAGUE_Y", parametros)))
                .isEqualTo(400);
        assertThat(codigo("POST", "/api/admin/cupons", promotor, cupomDados("NULO", "PERCENTUAL", parametros)))
                .isEqualTo(400);
    }

    @Test
    void condicaoSemTipoDeveSer400() throws Exception {
        var dados = campanha("FRETE_GRATIS", null);
        dados.put("condicao", Map.of("parametros", Map.of()));

        assertThat(codigo("POST", "/api/admin/promocoes", promotor, dados)).isEqualTo(400);
    }

    // --- Consulta, alteracao, ativacao e versao de campanhas ---

    @Test
    void campanhaInexistenteEh404EmConsultaAlteracaoEAtivacao() throws Exception {
        assertThat(codigo("GET", "/api/admin/promocoes/999999", promotor, null)).isEqualTo(404);
        assertThat(codigo("PUT", "/api/admin/promocoes/999999", promotor, campanha("FRETE_GRATIS", null)))
                .isEqualTo(404);
        assertThat(codigo("POST", "/api/admin/promocoes/999999/ativacao", promotor, Map.of("versao", 0)))
                .isEqualTo(404);
        assertThat(codigo("POST", "/api/admin/promocoes/999999/inativacao", promotor, Map.of("versao", 0)))
                .isEqualTo(404);
        assertThat(codigo("GET", "/api/admin/promocoes/abc", promotor, null)).isEqualTo(400);
    }

    @Test
    void ativacaoExigeVersaoValidaEVersaoObsoletaEh409() throws Exception {
        var criada = criarCampanha("FRETE_GRATIS", null);
        String url = "/api/admin/promocoes/" + criada.get("id").asLong();

        assertThat(codigo("POST", url + "/inativacao", promotor, "{}")).isEqualTo(400);
        assertThat(codigo("POST", url + "/inativacao", promotor, Map.of("versao", -1))).isEqualTo(400);
        assertThat(codigo("POST", url + "/inativacao", promotor, "{\"versao\":null}")).isEqualTo(400);
        assertThat(codigo("POST", url + "/inativacao", promotor, "{\"versao\":\"x\"}")).isEqualTo(400);
        assertThat(codigo("POST", url + "/inativacao", promotor, Map.of("versao", 7))).isEqualTo(409);
        enviar("GET", url, promotor, null).andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    void inativarEReativarIncrementamAVersaoEExigemAVersaoCorrente() throws Exception {
        var criada = criarCampanha("FRETE_GRATIS", null);
        String url = "/api/admin/promocoes/" + criada.get("id").asLong();

        var inativa = node(enviar("POST", url + "/inativacao", promotor, Map.of("versao", 0))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ativo").value(false)));
        assertThat(inativa.get("versao").asLong()).isEqualTo(1);
        assertThat(codigo("POST", url + "/ativacao", promotor, Map.of("versao", 0))).as("versao antiga")
                .isEqualTo(409);
        enviar("POST", url + "/ativacao", promotor, Map.of("versao", 1)).andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(true)).andExpect(jsonPath("$.versao").value(2));
    }

    @Test
    void alterarCampanhaExigeAVersaoCorrenteEAtualizaTodosOsCampos() throws Exception {
        var criada = criarCampanha("PERCENTUAL_PRIMEIRA_COMPRA", Map.of("percentual", 10));
        String url = "/api/admin/promocoes/" + criada.get("id").asLong();
        var novos = campanha("VALOR_FIXO_ACIMA_DE", Map.of("minimo", 50, "valor", 5));
        novos.put("nome", "Renomeada");
        novos.put("prioridade", 99);
        novos.put("modo", "EXCLUSIVA");

        assertThat(codigo("PUT", url, promotor, novos)).as("sem versao").isEqualTo(409);
        novos.put("versao", 5);
        assertThat(codigo("PUT", url, promotor, novos)).as("versao obsoleta").isEqualTo(409);
        novos.put("versao", 0);
        enviar("PUT", url, promotor, novos).andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Renomeada"))
                .andExpect(jsonPath("$.tipo").value("VALOR_FIXO_ACIMA_DE")).andExpect(jsonPath("$.prioridade").value(99))
                .andExpect(jsonPath("$.modo").value("EXCLUSIVA")).andExpect(jsonPath("$.versao").value(1));
        assertThat(codigo("PUT", url, promotor, novos)).as("reuso da versao antiga").isEqualTo(409);
    }

    @Test
    void alteracaoInvalidaDaCampanhaNaoMudaNadaNemAVersao() throws Exception {
        var criada = criarCampanha("PERCENTUAL_PRIMEIRA_COMPRA", Map.of("percentual", 10));
        String url = "/api/admin/promocoes/" + criada.get("id").asLong();
        var invalida = campanha("PERCENTUAL_PRIMEIRA_COMPRA", Map.of("percentual", 500));
        invalida.put("versao", 0);
        invalida.put("nome", "Nome que nao deve ficar");

        assertThat(codigo("PUT", url, promotor, invalida)).isEqualTo(400);
        var tipoDesconhecido = campanha("MAGICA", Map.of());
        tipoDesconhecido.put("versao", 0);
        assertThat(codigo("PUT", url, promotor, tipoDesconhecido)).isEqualTo(400);

        enviar("GET", url, promotor, null).andExpect(jsonPath("$.nome").value("Campanha PERCENTUAL_PRIMEIRA_COMPRA"))
                .andExpect(jsonPath("$.versao").value(0)).andExpect(jsonPath("$.parametros.percentual").value(10));
    }

    @Test
    void listagemDeCampanhasValidaPaginacaoETotal() throws Exception {
        for (int i = 0; i < 3; i++) {
            criarCampanha("FRETE_GRATIS", null);
        }

        enviar("GET", "/api/admin/promocoes?size=2", promotor, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo", hasSize(2))).andExpect(jsonPath("$.totalElementos").value(3));
        enviar("GET", "/api/admin/promocoes?size=2&page=1", promotor, null)
                .andExpect(jsonPath("$.conteudo", hasSize(1)));
        enviar("GET", "/api/admin/promocoes?page=9", promotor, null).andExpect(jsonPath("$.conteudo", hasSize(0)));
        for (String invalida : List.of("size=0", "size=101", "size=-1", "page=-1", "size=abc")) {
            assertThat(codigo("GET", "/api/admin/promocoes?" + invalida, promotor, null)).as(invalida).isEqualTo(400);
            assertThat(codigo("GET", "/api/admin/cupons?" + invalida, promotor, null)).as(invalida).isEqualTo(400);
        }
        assertThat(codigo("GET", "/api/admin/promocoes?size=100", promotor, null)).isEqualTo(200);
    }

    // --- Cupons: validacao e ciclo de vida ---

    @Test
    void codigosDeCupomInvalidosSao400SemGravacao() throws Exception {
        for (String codigo : List.of("AB", "A", "", "   ", "A B C", "ABC!", "AB.C", "CÓDIGO", "ABC/DEF",
                "A".repeat(41))) {
            assertThat(codigo("POST", "/api/admin/cupons", promotor,
                    cupomDados(codigo, "PERCENTUAL", Map.of("percentual", 10)))).as("'%s'", codigo).isEqualTo(400);
        }
        var semCodigo = cupomDados("X", "PERCENTUAL", Map.of("percentual", 10));
        semCodigo.remove("codigo");
        assertThat(codigo("POST", "/api/admin/cupons", promotor, semCodigo)).isEqualTo(400);

        assertThat(contar("cupom")).isZero();
    }

    @Test
    void codigosValidosNosLimitesSaoAceitosENormalizados() throws Exception {
        assertThat(criarCupom("abc", "PERCENTUAL", Map.of("percentual", 10)).get("codigo").asString())
                .isEqualTo("ABC");
        assertThat(criarCupom("a-b_c-9", "PERCENTUAL", Map.of("percentual", 10)).get("codigo").asString())
                .isEqualTo("A-B_C-9");
        assertThat(criarCupom("  x".repeat(1) + "y".repeat(39) + " ", "PERCENTUAL", Map.of("percentual", 10))
                .get("codigo").asString()).hasSize(40);
    }

    @Test
    void codigoEquivalenteEmOutraCaixaOuComEspacosEh409() throws Exception {
        criarCupom("BEMVINDO", "PERCENTUAL", Map.of("percentual", 10));

        for (String igual : List.of("bemvindo", " BemVindo ", "BEMVINDO")) {
            assertThat(codigo("POST", "/api/admin/cupons", promotor,
                    cupomDados(igual, "PERCENTUAL", Map.of("percentual", 5)))).as(igual).isEqualTo(409);
        }
        assertThat(contar("cupom")).isEqualTo(1);
    }

    @Test
    void camposInvalidosDoCupomSao400() throws Exception {
        List<Map<String, Object>> invalidos = new ArrayList<>();
        for (String campo : List.of("nome", "tipo", "inicio", "fim")) {
            var dados = cupomDados("CUPOM", "PERCENTUAL", Map.of("percentual", 10));
            dados.remove(campo);
            invalidos.add(dados);
        }
        for (int limite : new int[] {0, -1}) {
            var dados = cupomDados("CUPOM", "PERCENTUAL", Map.of("percentual", 10));
            dados.put("limiteTotal", limite);
            invalidos.add(dados);
        }
        for (int limite : new int[] {0, -3}) {
            var dados = cupomDados("CUPOM", "PERCENTUAL", Map.of("percentual", 10));
            dados.put("limiteCliente", limite);
            invalidos.add(dados);
        }
        var invertido = cupomDados("CUPOM", "PERCENTUAL", Map.of("percentual", 10));
        invertido.put("inicio", Instant.now().plusSeconds(100).toString());
        invertido.put("fim", Instant.now().minusSeconds(100).toString());
        invalidos.add(invertido);
        invalidos.add(cupomDados("CUPOM", "PERCENTUAL", Map.of("percentual", 0)));
        invalidos.add(cupomDados("CUPOM", "PERCENTUAL", Map.of("percentual", 100.5)));
        invalidos.add(cupomDados("CUPOM", "PERCENTUAL", Map.of()));
        invalidos.add(cupomDados("CUPOM", "VALOR_FIXO", Map.of("valor", 0)));
        invalidos.add(cupomDados("CUPOM", "VALOR_FIXO", Map.of("valor", -5)));
        invalidos.add(cupomDados("CUPOM", "VALOR_FIXO", Map.of("valor", 1.005)));
        invalidos.add(cupomDados("CUPOM", "REDUCAO_FRETE", Map.of("valor", 0)));
        invalidos.add(cupomDados("CUPOM", "REDUCAO_FRETE", Map.of()));
        invalidos.add(cupomDados("CUPOM", "TIPO_MAGICO", Map.of()));
        invalidos.add(cupomDados("CUPOM", "percentual", Map.of("percentual", 10)));

        for (var dados : invalidos) {
            assertThat(codigo("POST", "/api/admin/cupons", promotor, dados)).as("%s", dados).isEqualTo(400);
        }

        assertThat(contar("cupom")).isZero();
    }

    @Test
    void cupomNosLimitesDeValidacaoEAceito() throws Exception {
        criarCupom("CEM", "PERCENTUAL", Map.of("percentual", 100));
        criarCupom("CENTAVO", "VALOR_FIXO", Map.of("valor", 0.01));
        criarCupom("FRETEGRATIS", "FRETE_GRATIS", Map.of());
        criarCupom("REDUZ", "REDUCAO_FRETE", Map.of("valor", 5));
        var comLimites = cupomDados("LIMITES", "PERCENTUAL", Map.of("percentual", 10));
        comLimites.put("limiteTotal", 1);
        comLimites.put("limiteCliente", 5);
        enviar("POST", "/api/admin/cupons", promotor, comLimites).andExpect(status().isCreated())
                .andExpect(jsonPath("$.limiteCliente").value(5)).andExpect(jsonPath("$.limiteTotal").value(1))
                .andExpect(jsonPath("$.usosAtivos").value(0)).andExpect(jsonPath("$.versao").value(0));

        assertThat(contar("cupom")).isEqualTo(5);
    }

    @Test
    void cupomInexistenteEh404EmConsultaAlteracaoEAtivacao() throws Exception {
        assertThat(codigo("GET", "/api/admin/cupons/999999", promotor, null)).isEqualTo(404);
        assertThat(codigo("PUT", "/api/admin/cupons/999999", promotor,
                cupomDados("CUPOM", "PERCENTUAL", Map.of("percentual", 10)))).isEqualTo(404);
        assertThat(codigo("POST", "/api/admin/cupons/999999/ativacao", promotor, Map.of("versao", 0))).isEqualTo(404);
        assertThat(codigo("POST", "/api/admin/cupons/999999/inativacao", promotor, Map.of("versao", 0)))
                .isEqualTo(404);
    }

    @Test
    void codigoDoCupomEImutavelMasEquivalenteNormalizadoEhAceito() throws Exception {
        var criado = criarCupom("FIXO", "PERCENTUAL", Map.of("percentual", 10));
        String url = "/api/admin/cupons/" + criado.get("id").asLong();
        var alteracao = cupomDados("OUTRO", "PERCENTUAL", Map.of("percentual", 15));
        alteracao.put("versao", 0);

        assertThat(codigo("PUT", url, promotor, alteracao)).isEqualTo(400);

        alteracao.put("codigo", " fixo ");
        enviar("PUT", url, promotor, alteracao).andExpect(status().isOk()).andExpect(jsonPath("$.codigo").value("FIXO"))
                .andExpect(jsonPath("$.parametros.percentual").value(15)).andExpect(jsonPath("$.versao").value(1));
    }

    @Test
    void alterarCupomComVersaoObsoletaOuSemVersaoEh409() throws Exception {
        var criado = criarCupom("FIXO", "PERCENTUAL", Map.of("percentual", 10));
        String url = "/api/admin/cupons/" + criado.get("id").asLong();
        var alteracao = cupomDados("FIXO", "PERCENTUAL", Map.of("percentual", 15));

        assertThat(codigo("PUT", url, promotor, alteracao)).as("sem versao").isEqualTo(409);
        alteracao.put("versao", 3);
        assertThat(codigo("PUT", url, promotor, alteracao)).as("versao futura").isEqualTo(409);
        enviar("GET", url, promotor, null).andExpect(jsonPath("$.parametros.percentual").value(10));
    }

    @Test
    void alteracaoInvalidaDoCupomNaoGravaNada() throws Exception {
        var criado = criarCupom("FIXO", "PERCENTUAL", Map.of("percentual", 10));
        String url = "/api/admin/cupons/" + criado.get("id").asLong();
        var invalida = cupomDados("FIXO", "PERCENTUAL", Map.of("percentual", 1000));
        invalida.put("versao", 0);
        invalida.put("nome", "Nao deve gravar");

        assertThat(codigo("PUT", url, promotor, invalida)).isEqualTo(400);

        enviar("GET", url, promotor, null).andExpect(jsonPath("$.nome").value("Cupom FIXO"))
                .andExpect(jsonPath("$.versao").value(0));
    }

    @Test
    void ativarEInativarCupomExigemVersaoCorrente() throws Exception {
        var criado = criarCupom("FIXO", "PERCENTUAL", Map.of("percentual", 10));
        String url = "/api/admin/cupons/" + criado.get("id").asLong();

        assertThat(codigo("POST", url + "/inativacao", promotor, "{}")).isEqualTo(400);
        assertThat(codigo("POST", url + "/inativacao", promotor, Map.of("versao", 4))).isEqualTo(409);
        enviar("POST", url + "/inativacao", promotor, Map.of("versao", 0)).andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false)).andExpect(jsonPath("$.versao").value(1));
        enviar("POST", url + "/ativacao", promotor, Map.of("versao", 1)).andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    void listagemDeCuponsComTotalEPagina() throws Exception {
        criarCupom("UMA", "PERCENTUAL", Map.of("percentual", 10));
        criarCupom("DOIS", "PERCENTUAL", Map.of("percentual", 10));
        criarCupom("TRES", "PERCENTUAL", Map.of("percentual", 10));

        enviar("GET", "/api/admin/cupons?size=2", promotor, null).andExpect(jsonPath("$.conteudo", hasSize(2)))
                .andExpect(jsonPath("$.totalElementos").value(3));
    }

    // --- Carrinho: aplicacao das campanhas ---

    @Test
    void campanhaFuturaOuExpiradaOuInativaNaoAfetaOCarrinhoEReativarVolta() throws Exception {
        incluir("LIVRO", 1);
        var futura = campanha("VALOR_FIXO_ACIMA_DE", Map.of("minimo", 0, "valor", 10));
        futura.put("inicio", Instant.now().plusSeconds(600).toString());
        futura.put("fim", Instant.now().plusSeconds(1200).toString());
        criarCampanha(futura);
        var expirada = campanha("VALOR_FIXO_ACIMA_DE", Map.of("minimo", 0, "valor", 10));
        expirada.put("inicio", Instant.now().minusSeconds(1200).toString());
        expirada.put("fim", Instant.now().minusSeconds(600).toString());
        criarCampanha(expirada);
        var vigente = criarCampanha("VALOR_FIXO_ACIMA_DE", Map.of("minimo", 0, "valor", 10));
        String url = "/api/admin/promocoes/" + vigente.get("id").asLong();

        assertThat(carrinho().get("total").decimalValue()).isEqualByComparingTo("90");
        enviar("POST", url + "/inativacao", promotor, Map.of("versao", 0)).andExpect(status().isOk());
        assertThat(carrinho().get("total").decimalValue()).isEqualByComparingTo("100");
        enviar("POST", url + "/ativacao", promotor, Map.of("versao", 1)).andExpect(status().isOk());
        assertThat(carrinho().get("total").decimalValue()).isEqualByComparingTo("90");
    }

    @Test
    void campanhaAlteradaPassaAValerComOsNovosParametros() throws Exception {
        incluir("LIVRO", 1);
        var criada = criarCampanha("VALOR_FIXO_ACIMA_DE", Map.of("minimo", 0, "valor", 10));
        var novos = campanha("VALOR_FIXO_ACIMA_DE", Map.of("minimo", 0, "valor", 25));
        novos.put("versao", 0);

        enviar("PUT", "/api/admin/promocoes/" + criada.get("id").asLong(), promotor, novos).andExpect(status().isOk());

        assertThat(carrinho().get("total").decimalValue()).isEqualByComparingTo("75");
    }

    @Test
    void leveTresPagueDoisCom7UnidadesDescontaDuasUnidades() throws Exception {
        criarCampanha("LEVE_X_PAGUE_Y", Map.of("sku", "LIVRO", "leve", 3, "pague", 2));
        incluir("LIVRO", 7);
        incluir("TECLADO", 1);

        var resumo = carrinho();

        assertThat(resumo.get("subtotal").decimalValue()).isEqualByComparingTo("900");
        assertThat(resumo.get("total").decimalValue()).isEqualByComparingTo("700");
        assertThat(resumo.get("descontos")).hasSize(1);
    }

    @Test
    void leveXPagueYAbaixoDaQuantidadeMinimaNaoConcedeNada() throws Exception {
        criarCampanha("LEVE_X_PAGUE_Y", Map.of("sku", "LIVRO", "leve", 3, "pague", 2));
        incluir("LIVRO", 2);

        var resumo = carrinho();

        assertThat(resumo.get("total").decimalValue()).isEqualByComparingTo("200");
        assertThat(resumo.get("descontos")).isEmpty();
    }

    @Test
    void percentualNaCategoriaAncestralAlcancaSubcategoriaEIgnoraOutrasCategorias() throws Exception {
        criarCampanha("PERCENTUAL_CATEGORIA", Map.of("categoriaId", literaturaId, "percentual", 20));
        incluir("LIVRO", 1);
        incluir("TECLADO", 1);

        var resumo = carrinho();

        assertThat(resumo.get("total").decimalValue()).isEqualByComparingTo("280");
        assertThat(resumo.get("descontos").get(0).get("valor").decimalValue()).isEqualByComparingTo("20");
    }

    @Test
    void percentualNaSubcategoriaNaoAlcancaProdutosDaCategoriaPai() throws Exception {
        var literatura = catalogo.categoria("Clássicos", null);
        catalogo.variacao(literatura, "CLASSICO", "100");
        jdbc.update("UPDATE saldo_estoque SET disponivel=1000");
        criarCampanha("PERCENTUAL_CATEGORIA", Map.of("categoriaId", livrosId, "percentual", 50));
        incluir("CLASSICO", 1);

        assertThat(carrinho().get("total").decimalValue()).isEqualByComparingTo("100");
    }

    @Test
    void descontosAcumulaveisESomamEApenasAMelhorNaoAcumulavelEntra() throws Exception {
        incluir("LIVRO", 4);
        fixa(20, 10, "ACUMULAVEL");
        fixa(50, 20, "ACUMULAVEL");
        fixa(40, 5, "NAO_ACUMULAVEL");
        fixa(60, 5, "NAO_ACUMULAVEL");

        var resumo = carrinho();

        // 400 - (20 + 50 + 60) = 270; a nao acumulavel de 40 nao aparece.
        assertThat(resumo.get("total").decimalValue()).isEqualByComparingTo("270");
        assertThat(resumo.get("descontos")).hasSize(3);
    }

    @Test
    void exclusivaDeMaiorValorAnulaAsDemaisECupomFicaIncompativel() throws Exception {
        incluir("LIVRO", 4);
        fixa(20, 10, "ACUMULAVEL");
        fixa(50, 20, "ACUMULAVEL");
        fixa(100, 1, "EXCLUSIVA");
        criarCupom("DEZ", "PERCENTUAL", Map.of("percentual", 10));

        enviar("PUT", CAR + "/cupom", cliente.bearer(), Map.of("codigo", "DEZ")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.motivo").value("INCOMPATIVEL"));
        assertThat(carrinho().get("total").decimalValue()).isEqualByComparingTo("300");
    }

    @Test
    void tetoDosItensReduzPrimeiroOBeneficioDeMenorPrioridade() throws Exception {
        incluir("LIVRO", 1);
        fixa(50, 20, "ACUMULAVEL");
        fixa(80, 10, "ACUMULAVEL");

        var resumo = carrinho();

        assertThat(resumo.get("total").decimalValue()).isEqualByComparingTo("0");
        assertThat(resumo.get("descontos").get(0).get("valor").decimalValue()).isEqualByComparingTo("50");
        assertThat(resumo.get("descontos").get(1).get("valor").decimalValue()).isEqualByComparingTo("50");
    }

    @Test
    void empateDePrioridadeEValorEntreNaoAcumulaveisResolvePelaCriacaoMaisAntiga() throws Exception {
        incluir("LIVRO", 4);
        var primeira = fixa(30, 7, "NAO_ACUMULAVEL");
        fixa(30, 7, "NAO_ACUMULAVEL");

        var resumo = carrinho();

        assertThat(resumo.get("descontos")).hasSize(1);
        assertThat(resumo.get("descontos").get(0).get("referenciaId").asLong()).isEqualTo(primeira.get("id").asLong());
    }

    @Test
    void kitUsaPrecoEComSkuComercialENaoRecebePercentualDaCategoriaDosComponentes() throws Exception {
        var livros = catalogo.variacao(catalogo.categoria("Extra", null), "EXTRA", "10");
        catalogo.combo("KIT", "79.90", livros, 1);
        jdbc.update("UPDATE saldo_estoque SET disponivel=1000");
        criarCampanha("PERCENTUAL_CATEGORIA", Map.of("categoriaId", livrosId, "percentual", 50));
        criarCampanha("LEVE_X_PAGUE_Y", Map.of("sku", "KIT", "leve", 3, "pague", 2));
        incluir("KIT", 3);

        var resumo = carrinho();

        assertThat(resumo.get("subtotal").decimalValue()).isEqualByComparingTo("239.70");
        assertThat(resumo.get("total").decimalValue()).isEqualByComparingTo("159.80");
    }

    // --- Carrinho: cupons ---

    @Test
    void cupomAposPromocoesDoSpecDa261() throws Exception {
        incluir("LIVRO", 4);
        fixa(110, 10, "ACUMULAVEL");
        criarCupom("DEZ", "PERCENTUAL", Map.of("percentual", 10));

        enviar("PUT", CAR + "/cupom", cliente.bearer(), Map.of("codigo", "DEZ")).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(261));
    }

    @Test
    void cupomDeValorFixoMaiorQueOSubtotalZeraSemFicarNegativo() throws Exception {
        incluir("LIVRO", 1);
        criarCupom("GRANDE", "VALOR_FIXO", Map.of("valor", 500));

        var resumo = node(enviar("PUT", CAR + "/cupom", cliente.bearer(), Map.of("codigo", "GRANDE"))
                .andExpect(status().isOk()));

        assertThat(resumo.get("total").decimalValue()).isEqualByComparingTo("0");
        assertThat(resumo.get("descontos").get(0).get("valor").decimalValue()).isEqualByComparingTo("100");
        assertThat(resumo.get("descontos").get(0).get("origem").asString()).isEqualTo("CUPOM");
        assertThat(resumo.get("descontos").get(0).get("codigo").asString()).isEqualTo("GRANDE");
    }

    @Test
    void cupomPercentualArredondaHalfEvenSobreOSaldoDosItens() throws Exception {
        incluir("LIVRO", 3);
        criarCupom("TERCO", "PERCENTUAL", Map.of("percentual", 33.33));

        var resumo = node(enviar("PUT", CAR + "/cupom", cliente.bearer(), Map.of("codigo", "TERCO"))
                .andExpect(status().isOk()));

        assertThat(resumo.get("descontos").get(0).get("valor").decimalValue()).isEqualByComparingTo("99.99");
        assertThat(resumo.get("total").decimalValue()).isEqualByComparingTo("200.01");
    }

    @Test
    void cupomDeCemPorCentoZeraOCarrinho() throws Exception {
        incluir("LIVRO", 2);
        criarCupom("TUDO", "PERCENTUAL", Map.of("percentual", 100));

        enviar("PUT", CAR + "/cupom", cliente.bearer(), Map.of("codigo", "TUDO")).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void cupomDeFreteSemCotacaoNoCarrinhoNaoGeraDescontoEMantemOTotal() throws Exception {
        incluir("LIVRO", 1);
        criarCupom("FRETE", "FRETE_GRATIS", Map.of());

        var resumo = node(enviar("PUT", CAR + "/cupom", cliente.bearer(), Map.of("codigo", "FRETE"))
                .andExpect(status().isOk()));

        assertThat(resumo.get("total").decimalValue()).isEqualByComparingTo("100");
        assertThat(resumo.get("descontos")).isEmpty();
        assertThat(resumo.get("cupom").get("codigo").asString()).isEqualTo("FRETE");
    }

    @Test
    void codigoMalformadoNoCarrinhoEh400EOCupomAnteriorEPreservado() throws Exception {
        incluir("LIVRO", 1);
        criarCupom("DEZ", "PERCENTUAL", Map.of("percentual", 10));
        enviar("PUT", CAR + "/cupom", cliente.bearer(), Map.of("codigo", "DEZ")).andExpect(status().isOk());

        for (String malformado : List.of("AB", "A B C", "ABC!", "A".repeat(41), "   ")) {
            assertThat(codigo("PUT", CAR + "/cupom", cliente.bearer(), Map.of("codigo", malformado)))
                    .as("'%s'", malformado).isEqualTo(400);
        }
        assertThat(codigo("PUT", CAR + "/cupom", cliente.bearer(), "{}")).isEqualTo(400);
        assertThat(codigo("PUT", CAR + "/cupom", cliente.bearer(), "{\"codigo\":null}")).isEqualTo(400);

        assertThat(carrinho().get("cupom").get("codigo").asString()).isEqualTo("DEZ");
        assertThat(carrinho().get("total").decimalValue()).isEqualByComparingTo("90");
    }

    @Test
    void aplicarCupomExigeAutenticacaoDeCliente() throws Exception {
        assertThat(codigo("PUT", CAR + "/cupom", null, Map.of("codigo", "ABC"))).isEqualTo(401);
        assertThat(codigo("PUT", CAR + "/cupom", promotor, Map.of("codigo", "ABC"))).isIn(403, 404, 409);
    }

    @Test
    void cupomEsgotadoPelaContagemAtivaEhRejeitadoComMotivoEsgotado() throws Exception {
        incluir("LIVRO", 1);
        var cupom = criarCupom("RARO", "PERCENTUAL", Map.of("percentual", 10));
        jdbc.update("UPDATE cupom SET limite_total = 1 WHERE id = ?", cupom.get("id").asLong());
        jdbc.update("UPDATE saldo_cupom SET usos_ativos = 1 WHERE cupom_id = ?", cupom.get("id").asLong());

        enviar("PUT", CAR + "/cupom", cliente.bearer(), Map.of("codigo", "RARO")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.motivo").value("ESGOTADO"));
        enviar("GET", "/api/admin/cupons/" + cupom.get("id").asLong(), promotor, null)
                .andExpect(jsonPath("$.usosAtivos").value(1));
    }

    @Test
    void cupomDeOutroClienteNaoApareceNoCarrinhoDeQuemNaoOAplicou() throws Exception {
        incluir("LIVRO", 1);
        criarCupom("DEZ", "PERCENTUAL", Map.of("percentual", 10));
        enviar("PUT", CAR + "/cupom", cliente.bearer(), Map.of("codigo", "DEZ")).andExpect(status().isOk());
        var outro = clientes.criar("outro@bordas.local", 802);

        enviar("GET", CAR, outro.bearer(), null).andExpect(status().isOk()).andExpect(jsonPath("$.cupom").isEmpty())
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void removerCupomVoltaAoTotalSemDesconto() throws Exception {
        incluir("LIVRO", 1);
        criarCupom("DEZ", "PERCENTUAL", Map.of("percentual", 10));
        enviar("PUT", CAR + "/cupom", cliente.bearer(), Map.of("codigo", "DEZ")).andExpect(status().isOk());

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(CAR + "/cupom")
                .header("Authorization", cliente.bearer())).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(100)).andExpect(jsonPath("$.cupom").isEmpty());
    }

    @Test
    void usoDoCupomNaoEConsumidoAoAplicarNemAoConsultarOCarrinho() throws Exception {
        incluir("LIVRO", 1);
        var cupom = criarCupom("DEZ", "PERCENTUAL", Map.of("percentual", 10));
        enviar("PUT", CAR + "/cupom", cliente.bearer(), Map.of("codigo", "DEZ")).andExpect(status().isOk());
        carrinho();
        carrinho();

        assertThat(contar("uso_cupom")).isZero();
        enviar("GET", "/api/admin/cupons/" + cupom.get("id").asLong(), promotor, null)
                .andExpect(jsonPath("$.usosAtivos").value(0));
    }

    @Test
    void respostaDeErroDeValidacaoListaOsCamposInvalidosDaCampanha() throws Exception {
        var invalida = campanha("FRETE_GRATIS", null);
        invalida.remove("nome");
        invalida.remove("inicio");

        MvcResult resultado = enviar("POST", "/api/admin/promocoes", promotor, invalida)
                .andExpect(status().isBadRequest()).andReturn();

        assertThat(resultado.getResponse().getContentAsString()).contains("nome").contains("inicio");
    }
}
