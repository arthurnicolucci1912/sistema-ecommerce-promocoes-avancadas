package br.com.fatec.ecommerce.cliente.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.fatec.ecommerce.TesteDeIntegracao;
import br.com.fatec.ecommerce.cliente.ClientesParaTeste;
import br.com.fatec.ecommerce.cliente.ClientesParaTeste.ClienteDeTeste;
import br.com.fatec.ecommerce.seguranca.UsuariosParaTeste;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.json.JsonMapper;

/** Gestao de enderecos, endereco principal e isolamento entre clientes. */
@TesteDeIntegracao
class EnderecosTests {

    private static final String URL = "/api/clientes/me/enderecos";

    @Autowired
    MockMvc mvc;
    @Autowired
    UsuariosParaTeste helper;
    @Autowired
    ClientesParaTeste clientesHelper;
    @Autowired
    JdbcTemplate jdbc;

    ClienteDeTeste a;
    ClienteDeTeste b;

    @BeforeEach
    void preparar() {
        helper.limpar();
        a = clientesHelper.criar("a@demo.local", 20);
        b = clientesHelper.criar("b@demo.local", 21);
    }

    private static String corpo(String apelido, String cep, String uf) {
        return "{\"apelido\":\"" + apelido + "\",\"cep\":\"" + cep + "\",\"logradouro\":\"Rua das Flores\","
                + "\"numero\":\"10\",\"complemento\":\"Casa 2\",\"bairro\":\"Centro\",\"cidade\":\"Campinas\","
                + "\"uf\":\"" + uf + "\"}";
    }

    private long cadastrar(ClienteDeTeste cliente, String apelido) throws Exception {
        MvcResult r = mvc.perform(post(URL).header("Authorization", cliente.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(corpo(apelido, "13010-000", "SP")))
                .andExpect(status().isCreated()).andReturn();
        return Long.parseLong(String.valueOf(JsonMapper.builder().build().readTree(r.getResponse().getContentAsString())
                .get("id").asLong()));
    }

    private void principalEsperado(ClienteDeTeste cliente, String apelido, int total) throws Exception {
        mvc.perform(get(URL).header("Authorization", cliente.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(total)))
                .andExpect(jsonPath("$[?(@.principal == true)].apelido", contains(apelido)));
    }

    // --- Gestao ---

    @Test
    void cadastrarEnderecoRetorna201ENormalizaCepEUf() throws Exception {
        mvc.perform(post(URL).header("Authorization", a.bearer()).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Casa", "13010-000", "sp")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString(URL + "/")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.apelido").value("Casa"))
                .andExpect(jsonPath("$.cep").value("13010000"))
                .andExpect(jsonPath("$.uf").value("SP"))
                .andExpect(jsonPath("$.complemento").value("Casa 2"));

        mvc.perform(get(URL).header("Authorization", a.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].apelido").value("Casa"));
    }

    @Test
    void variosEnderecosListagemDevolveTodosSomenteDoCliente() throws Exception {
        cadastrar(a, "Casa");
        cadastrar(a, "Trabalho");
        cadastrar(b, "Outro");

        mvc.perform(get(URL).header("Authorization", a.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].apelido", containsInAnyOrder("Casa", "Trabalho")));
    }

    @Test
    void enderecoInvalidoRetorna400IndicandoCepEUf() throws Exception {
        mvc.perform(post(URL).header("Authorization", a.bearer()).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Casa", "123", "XX")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.campos[?(@.campo == 'cep')]").exists())
                .andExpect(jsonPath("$.campos[?(@.campo == 'uf')]").exists());

        mvc.perform(get(URL).header("Authorization", a.bearer())).andExpect(jsonPath("$", empty()));
    }

    @Test
    void consultarEAlterarEndereco() throws Exception {
        long id = cadastrar(a, "Casa");

        mvc.perform(put(URL + "/" + id).header("Authorization", a.bearer()).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Casa Nova", "01310100", "RJ")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apelido").value("Casa Nova"))
                .andExpect(jsonPath("$.uf").value("RJ"))
                .andExpect(jsonPath("$.principal").value(true));

        mvc.perform(get(URL + "/" + id).header("Authorization", a.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cep").value("01310100"));

        mvc.perform(put(URL + "/" + id).header("Authorization", a.bearer()).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Casa", "12", "ZZ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[?(@.campo == 'cep')]").exists())
                .andExpect(jsonPath("$.campos[?(@.campo == 'uf')]").exists());
    }

    @Test
    void removerEnderecoRetorna204InativaESaiDaListagem() throws Exception {
        long id = cadastrar(a, "Casa");
        cadastrar(a, "Trabalho");

        mvc.perform(delete(URL + "/" + id).header("Authorization", a.bearer())).andExpect(status().isNoContent());

        mvc.perform(get(URL).header("Authorization", a.bearer()))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].apelido").value("Trabalho"));
        mvc.perform(get(URL + "/" + id).header("Authorization", a.bearer())).andExpect(status().isNotFound());
        mvc.perform(delete(URL + "/" + id).header("Authorization", a.bearer())).andExpect(status().isNotFound());
        Boolean ativo = jdbc.queryForObject("select ativo from endereco where id = ?", Boolean.class, id);
        assertThat(ativo).isFalse();
    }

    // --- Endereco principal ---

    @Test
    void primeiroEnderecoEPrincipal() throws Exception {
        cadastrar(a, "Casa");
        cadastrar(a, "Trabalho");

        principalEsperado(a, "Casa", 2);
    }

    @Test
    void trocarEnderecoPrincipalDesmarcaOAnterior() throws Exception {
        cadastrar(a, "Casa");
        long segundo = cadastrar(a, "Trabalho");

        mvc.perform(put(URL + "/" + segundo + "/principal").header("Authorization", a.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.principal").value(true));

        principalEsperado(a, "Trabalho", 2);

        mvc.perform(put(URL + "/" + segundo + "/principal").header("Authorization", a.bearer()))
                .andExpect(status().isOk());
        principalEsperado(a, "Trabalho", 2);
    }

    @Test
    void removerOPrincipalPromoveOEnderecoRestante() throws Exception {
        long casa = cadastrar(a, "Casa");
        cadastrar(a, "Trabalho");

        mvc.perform(delete(URL + "/" + casa).header("Authorization", a.bearer())).andExpect(status().isNoContent());

        principalEsperado(a, "Trabalho", 1);
    }

    @Test
    void removerOPrincipalPromoveOAtivoMaisAntigoRestante() throws Exception {
        cadastrar(a, "Primeiro");
        long segundo = cadastrar(a, "Segundo");
        long terceiro = cadastrar(a, "Terceiro");
        mvc.perform(put(URL + "/" + terceiro + "/principal").header("Authorization", a.bearer()))
                .andExpect(status().isOk());

        mvc.perform(delete(URL + "/" + terceiro).header("Authorization", a.bearer())).andExpect(status().isNoContent());

        principalEsperado(a, "Primeiro", 2);

        // Remover um nao principal nao muda o principal.
        mvc.perform(delete(URL + "/" + segundo).header("Authorization", a.bearer())).andExpect(status().isNoContent());
        principalEsperado(a, "Primeiro", 1);
    }

    @Test
    void removerOUnicoEnderecoERecadastrarOTornaPrincipalDeNovo() throws Exception {
        long id = cadastrar(a, "Casa");
        mvc.perform(delete(URL + "/" + id).header("Authorization", a.bearer())).andExpect(status().isNoContent());

        cadastrar(a, "Nova");

        principalEsperado(a, "Nova", 1);
    }

    // --- Isolamento ---

    @Test
    void acessarAlterarOuRemoverEnderecoDeOutroClienteRetorna404ESemAlterarOEnderecoDeB() throws Exception {
        long idB = cadastrar(b, "Casa de B");
        cadastrar(b, "Trabalho de B");
        List<Map<String, Object>> antes = jdbc.queryForList("select * from endereco where id = ?", idB);

        mvc.perform(get(URL + "/" + idB).header("Authorization", a.bearer()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mvc.perform(put(URL + "/" + idB).header("Authorization", a.bearer()).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Invadido", "99999-999", "AM")))
                .andExpect(status().isNotFound());
        mvc.perform(put(URL + "/" + idB + "/principal").header("Authorization", a.bearer()))
                .andExpect(status().isNotFound());
        mvc.perform(delete(URL + "/" + idB).header("Authorization", a.bearer())).andExpect(status().isNotFound());

        assertThat(jdbc.queryForList("select * from endereco where id = ?", idB)).isEqualTo(antes);
        principalEsperado(b, "Casa de B", 2);
        mvc.perform(get(URL).header("Authorization", a.bearer())).andExpect(jsonPath("$", empty()));
    }

    @Test
    void enderecoInexistenteRetorna404() throws Exception {
        mvc.perform(get(URL + "/999999").header("Authorization", a.bearer())).andExpect(status().isNotFound());
    }

    @Test
    void semTokenRetorna401EAdminRetorna403() throws Exception {
        mvc.perform(get(URL)).andExpect(status().isUnauthorized());
        var admin = helper.criar("admin@demo.local", br.com.fatec.ecommerce.seguranca.dominio.Perfil.ADMIN);
        mvc.perform(get(URL).header("Authorization", helper.bearer(admin))).andExpect(status().isForbidden());
    }
}
