package br.com.fatec.ecommerce.catalogo.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.fatec.ecommerce.TesteDeIntegracao;
import br.com.fatec.ecommerce.catalogo.CatalogoPersistidoParaTeste;
import br.com.fatec.ecommerce.seguranca.UsuariosParaTeste;
import br.com.fatec.ecommerce.seguranca.dominio.Perfil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.json.JsonMapper;

/** API do catalogo: vitrine publica sem token, perfis da administracao e fluxo HTTP completo com disponibilidade. */
@TesteDeIntegracao
@Import(CatalogoPersistidoParaTeste.class)
class CatalogoWebTests {

    @Autowired
    MockMvc mvc;
    @Autowired
    UsuariosParaTeste helper;
    @Autowired
    CatalogoPersistidoParaTeste catalogo;

    String admin;
    String comercial;
    String estoque;

    @BeforeEach
    void preparar() {
        helper.limpar();
        admin = helper.bearer(helper.criar("admin@demo.local", Perfil.ADMIN));
        comercial = helper.bearer(helper.criar("comercial@demo.local", Perfil.OPERADOR_COMERCIAL));
        estoque = helper.bearer(helper.criar("estoque@demo.local", Perfil.ESTOQUE));
    }

    private MvcResult enviar(String metodo, String url, String token, String corpo) throws Exception {
        var requisicao = switch (metodo) {
            case "POST" -> post(url);
            case "PUT" -> put(url);
            default -> delete(url);
        };
        if (token != null) {
            requisicao.header("Authorization", token);
        }
        if (corpo != null) {
            requisicao.contentType(MediaType.APPLICATION_JSON).content(corpo);
        }
        return mvc.perform(requisicao).andReturn();
    }

    private long id(MvcResult resultado) throws Exception {
        return JsonMapper.builder().build().readTree(resultado.getResponse().getContentAsString()).get("id").asLong();
    }

    private long idDaVariacao(MvcResult produto, int indice) throws Exception {
        return JsonMapper.builder().build().readTree(produto.getResponse().getContentAsString()).get("variacoes")
                .get(indice).get("id").asLong();
    }

    // --- Vitrine publica ---

    @Test
    void consultaPublicaNaoExigeToken() throws Exception {
        var raiz = catalogo.categoria("Casa", null);
        var variacao = catalogo.variacao(raiz, "CANECA", "25.00");

        mvc.perform(get("/api/catalogo/categorias")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Casa"));
        mvc.perform(get("/api/catalogo/produtos")).andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo", hasSize(1)))
                .andExpect(jsonPath("$.conteudo[0].precoMinimo").value(25.00));
        mvc.perform(get("/api/catalogo/produtos").param("nome", "inexistente")).andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo", hasSize(0)));
        mvc.perform(get("/api/catalogo/produtos/{id}", variacao.getProduto().getId())).andExpect(status().isOk())
                .andExpect(jsonPath("$.variacoes[0].sku").value("CANECA"));
        mvc.perform(get("/api/catalogo/combos")).andExpect(status().isOk());
    }

    @Test
    void produtoEComboInexistentesRespondem404() throws Exception {
        mvc.perform(get("/api/catalogo/produtos/999999")).andExpect(status().isNotFound());
        mvc.perform(get("/api/catalogo/combos/999999")).andExpect(status().isNotFound());
    }

    // --- Perfis e autenticacao ---

    @Test
    void perfilDeEstoqueAlteraCatalogoRecebe403() throws Exception {
        String categoria = "{\"nome\":\"Roupas\"}";
        String produto = "{\"nome\":\"Camiseta\",\"categoriaId\":1,\"variacoes\":[{\"sku\":\"C-1\",\"preco\":10.00}]}";
        String combo = "{\"sku\":\"K-1\",\"nome\":\"Kit\",\"preco\":10.00,\"componentes\":[{\"sku\":\"C-1\",\"quantidade\":1}]}";

        assertThat(enviar("POST", "/api/admin/categorias", estoque, categoria).getResponse().getStatus()).isEqualTo(403);
        assertThat(enviar("PUT", "/api/admin/categorias/1", estoque, categoria).getResponse().getStatus()).isEqualTo(403);
        assertThat(enviar("DELETE", "/api/admin/categorias/1", estoque, null).getResponse().getStatus()).isEqualTo(403);
        assertThat(enviar("POST", "/api/admin/produtos", estoque, produto).getResponse().getStatus()).isEqualTo(403);
        assertThat(enviar("POST", "/api/admin/produtos/1/inativacao", estoque, null).getResponse().getStatus())
                .isEqualTo(403);
        assertThat(enviar("POST", "/api/admin/combos", estoque, combo).getResponse().getStatus()).isEqualTo(403);
        assertThat(enviar("POST", "/api/admin/combos/1/inativacao", estoque, null).getResponse().getStatus())
                .isEqualTo(403);
    }

    @Test
    void visitanteAlteraCatalogoRecebe401() throws Exception {
        assertThat(enviar("POST", "/api/admin/categorias", null, "{\"nome\":\"Roupas\"}").getResponse().getStatus())
                .isEqualTo(401);
        assertThat(enviar("POST", "/api/admin/produtos", null, "{}").getResponse().getStatus()).isEqualTo(401);
        assertThat(enviar("POST", "/api/admin/combos", null, "{}").getResponse().getStatus()).isEqualTo(401);
        assertThat(enviar("POST", "/api/catalogo/categorias", null, "{}").getResponse().getStatus()).isEqualTo(401);
        mvc.perform(get("/api/admin/categorias/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void usuariosAdministrativosContinuamSomenteDoAdmin() throws Exception {
        mvc.perform(get("/api/admin/usuarios").header("Authorization", comercial)).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/usuarios").header("Authorization", estoque)).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/usuarios").header("Authorization", admin)).andExpect(status().isOk());
    }

    // --- Validacao e regras de negocio ---

    @Test
    void corpoInvalidoRespondeComOsCamposComErro() throws Exception {
        mvc.perform(post("/api/admin/produtos").header("Authorization", comercial)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\" \",\"variacoes\":[{\"sku\":\"\",\"preco\":0}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[?(@.campo == 'nome')]").exists())
                .andExpect(jsonPath("$.campos[?(@.campo == 'categoriaId')]").exists())
                .andExpect(jsonPath("$.campos[?(@.campo == 'variacoes[0].sku')]").exists())
                .andExpect(jsonPath("$.campos[?(@.campo == 'variacoes[0].preco')]").exists());
    }

    @Test
    void categoriaCriadaAlteradaERemovida() throws Exception {
        MvcResult raiz = mvc.perform(post("/api/admin/categorias").header("Authorization", comercial)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Casa\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/api/admin/categorias/")))
                .andReturn();
        long casaId = id(raiz);
        long cozinhaId = id(enviar("POST", "/api/admin/categorias", comercial, "{\"nome\":\"Cozinha\"}"));

        mvc.perform(put("/api/admin/categorias/{id}", cozinhaId).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Cozinha e Mesa\",\"paiId\":" + casaId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Cozinha e Mesa"))
                .andExpect(jsonPath("$.paiId").value(casaId));
        // Ciclo: a raiz nao pode ir para baixo da propria filha (409) e nada e alterado.
        mvc.perform(put("/api/admin/categorias/{id}", casaId).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Outro nome\",\"paiId\":" + cozinhaId + "}"))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/catalogo/categorias")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Casa"))
                .andExpect(jsonPath("$[0].filhas[0].nome").value("Cozinha e Mesa"));

        mvc.perform(delete("/api/admin/categorias/{id}", casaId).header("Authorization", admin))
                .andExpect(status().isConflict());
        mvc.perform(delete("/api/admin/categorias/{id}", cozinhaId).header("Authorization", admin))
                .andExpect(status().isNoContent());
    }

    @Test
    void produtoPrecoVariacaoEInativacaoRefletemNaVitrine() throws Exception {
        long categoriaId = id(enviar("POST", "/api/admin/categorias", comercial, "{\"nome\":\"Roupas\"}"));
        MvcResult criado = mvc.perform(post("/api/admin/produtos").header("Authorization", comercial)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Camiseta\",\"categoriaId\":" + categoriaId
                                + ",\"variacoes\":[{\"sku\":\"CAM-P\",\"preco\":59.90,\"atributos\":{\"tamanho\":\"P\"}}]}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn();
        long produtoId = id(criado);
        long variacaoP = idDaVariacao(criado, 0);

        // SKU repetido: 409.
        mvc.perform(post("/api/admin/produtos/{id}/variacoes", produtoId).header("Authorization", comercial)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"sku\":\"CAM-P\",\"preco\":10.00}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/admin/produtos/{id}/variacoes", produtoId).header("Authorization", comercial)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"CAM-M\",\"preco\":64.90,\"atributos\":{\"tamanho\":\"M\"}}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.variacoes", hasSize(2)));
        mvc.perform(put("/api/admin/produtos/variacoes/{id}", variacaoP).header("Authorization", comercial)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"preco\":54.90}"))
                .andExpect(status().isOk());
        mvc.perform(put("/api/admin/produtos/{id}", produtoId).header("Authorization", comercial)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Camiseta Básica\",\"marca\":\"Fatec\",\"categoriaId\":" + categoriaId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Camiseta Básica"));

        mvc.perform(get("/api/catalogo/produtos/{id}", produtoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Camiseta Básica"))
                .andExpect(jsonPath("$.variacoes[?(@.sku == 'CAM-P')].preco", contains(54.90)));

        mvc.perform(post("/api/admin/produtos/variacoes/{id}/inativacao", variacaoP)
                        .header("Authorization", comercial))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/catalogo/produtos/{id}", produtoId))
                .andExpect(jsonPath("$.variacoes", hasSize(1)));
        mvc.perform(post("/api/admin/produtos/{id}/inativacao", produtoId).header("Authorization", comercial))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/catalogo/produtos/{id}", produtoId)).andExpect(status().isNotFound());
        mvc.perform(post("/api/admin/produtos/999999/inativacao").header("Authorization", comercial))
                .andExpect(status().isNotFound());
    }

    @Test
    void comboAlteraPrecoEComponentesEInativa() throws Exception {
        var categoria = catalogo.categoria("Casa", null);
        catalogo.variacao(categoria, "CANECA", "25.00");
        catalogo.variacao(categoria, "CAFE", "30.00");

        long comboId = id(mvc.perform(post("/api/admin/combos").header("Authorization", comercial)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"KIT\",\"nome\":\"Kit Café\",\"preco\":80.00,"
                                + "\"componentes\":[{\"sku\":\"CANECA\",\"quantidade\":1}]}"))
                .andExpect(status().isCreated()).andExpect(header().exists("Location")).andReturn());

        mvc.perform(put("/api/admin/combos/{id}", comboId).header("Authorization", comercial)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/admin/combos/{id}", comboId).header("Authorization", comercial)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"preco\":70.00,\"componentes\":[{\"sku\":\"NAO-EXISTE\",\"quantidade\":1}]}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/admin/combos/{id}", comboId).header("Authorization", comercial)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"preco\":70.00,\"componentes\":[{\"sku\":\"CAFE\",\"quantidade\":2}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preco").value(70.00));

        mvc.perform(put("/api/admin/combos/{id}", comboId).header("Authorization", comercial)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"preco\":60.00,\"componentes\":[{\"sku\":\"NAO-EXISTE\",\"quantidade\":1}]}"))
                .andExpect(status().isBadRequest());
        // A recusa acima e atomica: o preco 60.00 nao ficou gravado; vale so o ultimo PUT valido.
        mvc.perform(get("/api/catalogo/combos/{id}", comboId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.preco").value(70.00))
                .andExpect(jsonPath("$.componentes[0].sku").value("CAFE"));
        mvc.perform(post("/api/admin/combos/{id}/inativacao", comboId).header("Authorization", comercial))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/catalogo/combos/{id}", comboId)).andExpect(status().isNotFound());
    }

    // --- Fluxo completo ---

    @Test
    void fluxoCategoriaProdutoComVariacoesComboEConsultaPublicaComDisponibilidade() throws Exception {
        long roupasId = id(enviar("POST", "/api/admin/categorias", comercial, "{\"nome\":\"Roupas\"}"));
        long camisetasId = id(enviar("POST", "/api/admin/categorias", comercial,
                "{\"nome\":\"Camisetas\",\"paiId\":" + roupasId + "}"));
        MvcResult produto = mvc.perform(post("/api/admin/produtos").header("Authorization", comercial)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Camiseta Básica\",\"categoriaId\":" + camisetasId + ",\"variacoes\":["
                                + "{\"sku\":\"CAM-P\",\"preco\":59.90,\"atributos\":{\"tamanho\":\"P\",\"cor\":\"azul\"}},"
                                + "{\"sku\":\"CAM-M\",\"preco\":59.90,\"atributos\":{\"tamanho\":\"M\",\"cor\":\"azul\"}}]}"))
                .andExpect(status().isCreated())
                .andReturn();
        assertThat(produto.getResponse().getContentAsString()).contains("\"preco\":59.90");
        long produtoId = id(produto);
        long variacaoP = idDaVariacao(produto, 0);
        long variacaoM = idDaVariacao(produto, 1);
        long comboId = id(mvc.perform(post("/api/admin/combos").header("Authorization", comercial)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"KIT-CAM\",\"nome\":\"Kit Camisetas\",\"preco\":99.90,\"componentes\":["
                                + "{\"sku\":\"CAM-P\",\"quantidade\":1},{\"sku\":\"CAM-M\",\"quantidade\":2}]}"))
                .andExpect(status().isCreated()).andReturn());

        // Sem estoque, o combo nao pode ser montado: disponibilidade 0.
        mvc.perform(get("/api/catalogo/combos/{id}", comboId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.disponibilidade").value(0));

        mvc.perform(post("/api/admin/estoque/{id}/ajustes", variacaoP).header("Authorization", estoque)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantidade\":5,\"motivo\":\"compra do fornecedor\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.disponivel").value(5));
        mvc.perform(post("/api/admin/estoque/{id}/ajustes", variacaoM).header("Authorization", estoque)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantidade\":10,\"motivo\":\"compra do fornecedor\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.disponivel").value(10));

        mvc.perform(get("/api/catalogo/produtos/{id}", produtoId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.variacoes[?(@.sku == 'CAM-P')].disponivel", contains(5)))
                .andExpect(jsonPath("$.variacoes[?(@.sku == 'CAM-M')].disponivel", contains(10)));
        // min(5 / 1, 10 / 2) = 5 combos montaveis.
        mvc.perform(get("/api/catalogo/combos/{id}", comboId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.disponibilidade").value(5))
                .andExpect(jsonPath("$.componentes", hasSize(2)));
        mvc.perform(get("/api/catalogo/combos")).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].disponibilidade").value(5));
        mvc.perform(get("/api/catalogo/categorias")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].filhas[0].nome").value("Camisetas"));
        // O filtro por categoria inclui as subcategorias: a raiz "Roupas" encontra a camiseta de "Camisetas".
        mvc.perform(get("/api/catalogo/produtos").param("categoriaId", String.valueOf(roupasId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo", hasSize(1)))
                .andExpect(jsonPath("$.conteudo[0].id").value(produtoId));
    }
}
