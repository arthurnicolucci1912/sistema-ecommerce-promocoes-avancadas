package br.com.fatec.ecommerce.seguranca.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.fatec.ecommerce.TesteDeIntegracao;
import br.com.fatec.ecommerce.seguranca.UsuariosParaTeste;
import br.com.fatec.ecommerce.seguranca.dominio.Perfil;
import br.com.fatec.ecommerce.seguranca.dominio.Usuario;
import br.com.fatec.ecommerce.seguranca.dominio.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@TesteDeIntegracao
class UsuariosAdministrativosTests {

    private static final String URL = "/api/admin/usuarios";

    @Autowired
    MockMvc mvc;
    @Autowired
    UsuariosParaTeste helper;
    @Autowired
    UsuarioRepository usuarios;

    Usuario admin;
    String authAdmin;

    @BeforeEach
    void preparar() {
        helper.limpar();
        admin = helper.criar("admin@demo.local", Perfil.ADMIN);
        authAdmin = helper.bearer(admin);
    }

    private String corpoCriar(String email, String senha, String perfis) {
        return "{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\",\"perfis\":" + perfis + "}";
    }

    private void criarViaApi(String email, String perfis) throws Exception {
        mvc.perform(post(URL).header("Authorization", authAdmin).contentType(MediaType.APPLICATION_JSON)
                .content(corpoCriar(email, "Demo1234", perfis))).andExpect(status().isCreated());
    }

    @Test
    void criarUsuarioAdministrativoEleFazLoginComOPerfil() throws Exception {
        mvc.perform(post(URL).header("Authorization", authAdmin).contentType(MediaType.APPLICATION_JSON)
                        .content(corpoCriar("Promo@Demo.local", "Demo1234", "[\"PROMOCOES\"]")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value("promo@demo.local"))
                .andExpect(jsonPath("$.perfis[0]").value("PROMOCOES"))
                .andExpect(jsonPath("$.ativo").value(true))
                .andExpect(content().string(not(containsString("senha"))))
                .andExpect(content().string(not(containsString("{bcrypt}"))));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"promo@demo.local\",\"senha\":\"Demo1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        Usuario criado = usuarios.buscarPorEmail("promo@demo.local").orElseThrow();
        assertThat(criado.getPerfis()).containsExactly(Perfil.PROMOCOES);
        assertThat(criado.getSenhaHash()).isNotEqualTo("Demo1234");
    }

    @Test
    void perfilInexistenteRetorna400IndicandoOPerfil() throws Exception {
        mvc.perform(post(URL).header("Authorization", authAdmin).contentType(MediaType.APPLICATION_JSON)
                        .content(corpoCriar("x@demo.local", "Demo1234", "[\"SUPERVISOR\"]")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(containsString("SUPERVISOR")))
                .andExpect(jsonPath("$.detail").value(containsString("perfis")))
                .andExpect(jsonPath("$.detail").value(containsString("ADMIN")));
        assertThat(usuarios.existePorEmail("x@demo.local")).isFalse();
    }

    @Test
    void atribuirClienteRetorna400() throws Exception {
        mvc.perform(post(URL).header("Authorization", authAdmin).contentType(MediaType.APPLICATION_JSON)
                        .content(corpoCriar("c@demo.local", "Demo1234", "[\"CLIENTE\"]")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        assertThat(usuarios.existePorEmail("c@demo.local")).isFalse();

        criarViaApi("op@demo.local", "[\"ESTOQUE\"]");
        Long id = usuarios.buscarPorEmail("op@demo.local").orElseThrow().getId();
        mvc.perform(put(URL + "/" + id + "/perfis").header("Authorization", authAdmin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"perfis\":[\"ESTOQUE\",\"CLIENTE\"]}"))
                .andExpect(status().isBadRequest());
        assertThat(usuarios.buscarPorId(id).orElseThrow().getPerfis()).containsExactly(Perfil.ESTOQUE);
    }

    @Test
    void emailDuplicadoRetorna409() throws Exception {
        criarViaApi("dup@demo.local", "[\"ESTOQUE\"]");

        mvc.perform(post(URL).header("Authorization", authAdmin).contentType(MediaType.APPLICATION_JSON)
                        .content(corpoCriar("DUP@demo.local", "Demo1234", "[\"ESTOQUE\"]")))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void senhaFracaRetorna400IndicandoOCampoSenha() throws Exception {
        mvc.perform(post(URL).header("Authorization", authAdmin).contentType(MediaType.APPLICATION_JSON)
                        .content(corpoCriar("fraca@demo.local", "abcdefgh", "[\"ESTOQUE\"]")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[?(@.campo == 'senha')]").exists());
    }

    @Test
    void listaApenasUsuariosAdministrativosSemCredenciais() throws Exception {
        helper.criar("cliente@demo.local", Perfil.CLIENTE);
        criarViaApi("estoque@demo.local", "[\"ESTOQUE\"]");

        mvc.perform(get(URL).header("Authorization", authAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].email", hasItem("admin@demo.local")))
                .andExpect(jsonPath("$[*].email", hasItem("estoque@demo.local")))
                .andExpect(content().string(not(containsString("senha"))))
                .andExpect(content().string(not(containsString("{bcrypt}"))));
    }

    @Test
    void alterarPerfisFuncionaQuandoRestaOutroAdmin() throws Exception {
        helper.criar("admin2@demo.local", Perfil.ADMIN);
        mvc.perform(put(URL + "/" + admin.getId() + "/perfis").header("Authorization", authAdmin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"perfis\":[\"ESTOQUE\",\"PROMOCOES\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfis", hasSize(2)));
        assertThat(usuarios.buscarPorId(admin.getId()).orElseThrow().getPerfis())
                .containsExactlyInAnyOrder(Perfil.ESTOQUE, Perfil.PROMOCOES);
    }

    @Test
    void inativarRetorna204EOUsuarioNaoFazMaisLogin() throws Exception {
        criarViaApi("estoque@demo.local", "[\"ESTOQUE\"]");
        Long id = usuarios.buscarPorEmail("estoque@demo.local").orElseThrow().getId();

        mvc.perform(delete(URL + "/" + id).header("Authorization", authAdmin)).andExpect(status().isNoContent());

        assertThat(usuarios.buscarPorId(id).orElseThrow().isAtivo()).isFalse();
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"estoque@demo.local\",\"senha\":\"Demo1234\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usuarioInexistenteRetorna404() throws Exception {
        mvc.perform(delete(URL + "/999999").header("Authorization", authAdmin))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void retirarPerfilAdminDoUnicoAdminRetorna409EMantemOUsuario() throws Exception {
        mvc.perform(put(URL + "/" + admin.getId() + "/perfis").header("Authorization", authAdmin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"perfis\":[\"ESTOQUE\"]}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:ecommerce:erro:conflito"));

        Usuario depois = usuarios.buscarPorId(admin.getId()).orElseThrow();
        assertThat(depois.getPerfis()).containsExactly(Perfil.ADMIN);
        assertThat(depois.isAtivo()).isTrue();
    }

    @Test
    void inativarOUnicoAdminRetorna409EMantemOUsuario() throws Exception {
        mvc.perform(delete(URL + "/" + admin.getId()).header("Authorization", authAdmin))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        Usuario depois = usuarios.buscarPorId(admin.getId()).orElseThrow();
        assertThat(depois.isAtivo()).isTrue();
        assertThat(depois.getPerfis()).containsExactly(Perfil.ADMIN);
    }

    @Test
    void adminInativoNaoContaComoAdminRemanescente() throws Exception {
        helper.criarInativo("admin-inativo@demo.local", Perfil.ADMIN);

        mvc.perform(delete(URL + "/" + admin.getId()).header("Authorization", authAdmin))
                .andExpect(status().isConflict());
    }

    @Test
    void clienteNaGestaoDeUsuariosRetorna403() throws Exception {
        Usuario cliente = helper.criar("cliente@demo.local", Perfil.CLIENTE);

        mvc.perform(get(URL).header("Authorization", helper.bearer(cliente)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mvc.perform(post(URL).header("Authorization", helper.bearer(cliente)).contentType(MediaType.APPLICATION_JSON)
                        .content(corpoCriar("x@demo.local", "Demo1234", "[\"ADMIN\"]")))
                .andExpect(status().isForbidden());
        assertThat(usuarios.existePorEmail("x@demo.local")).isFalse();
    }

    @Test
    void perfilEstoqueNaGestaoDeUsuariosRetorna403() throws Exception {
        Usuario estoque = helper.criar("estoque@demo.local", Perfil.ESTOQUE);

        mvc.perform(get(URL).header("Authorization", helper.bearer(estoque)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mvc.perform(delete(URL + "/" + admin.getId()).header("Authorization", helper.bearer(estoque)))
                .andExpect(status().isForbidden());
    }
}
