package br.com.fatec.ecommerce.seguranca.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.fatec.ecommerce.TesteDeIntegracao;
import br.com.fatec.ecommerce.seguranca.UsuariosParaTeste;
import br.com.fatec.ecommerce.seguranca.dominio.Perfil;
import br.com.fatec.ecommerce.seguranca.dominio.Usuario;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.json.JsonMapper;

/** Login, exigencia de token e formato Problem Details dos erros 401/403. */
@TesteDeIntegracao
class AutenticacaoTests {

    private static final String MENSAGEM_GENERICA = "Credenciais inválidas.";

    @Autowired
    MockMvc mvc;
    @Autowired
    UsuariosParaTeste helper;
    @Autowired
    JwtEncoder encoder;
    @Autowired
    JwtDecoder decoder;

    @BeforeEach
    void limpar() {
        helper.limpar();
    }

    private String login(String email, String senha) {
        return "{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}";
    }

    private String token(Instant emitidoEm, Instant expiraEm, JwtEncoder jwtEncoder, Usuario usuario) {
        JwtClaimsSet claims = JwtClaimsSet.builder().subject(String.valueOf(usuario.getId()))
                .claim("email", usuario.getEmail()).claim("perfis", List.of("ADMIN"))
                .issuedAt(emitidoEm).expiresAt(expiraEm).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    // ---- login ----

    @Test
    void loginBemSucedido() throws Exception {
        Usuario usuario = helper.criar("Admin@Demo.local", Perfil.ADMIN, Perfil.ESTOQUE);

        MvcResult resultado = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login(" ADMIN@demo.local", UsuariosParaTeste.SENHA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(content().string(not(containsString("senha"))))
                .andReturn();

        String corpo = resultado.getResponse().getContentAsString();
        String token = JsonMapper.shared().readTree(corpo).get("accessToken").asString();
        Jwt jwt = decoder.decode(token);
        assertThat(jwt.getHeaders()).containsEntry("alg", "HS256");
        assertThat(jwt.getSubject()).isEqualTo(String.valueOf(usuario.getId()));
        assertThat(jwt.getClaimAsString("email")).isEqualTo("admin@demo.local");
        assertThat(jwt.getClaimAsStringList("perfis")).containsExactlyInAnyOrder("ADMIN", "ESTOQUE");
        assertThat(jwt.getIssuedAt()).isNotNull();
        assertThat(jwt.getExpiresAt()).isAfter(jwt.getIssuedAt());
    }

    @Test
    void senhaIncorretaRetorna401Generico() throws Exception {
        helper.criar("cliente@demo.local", Perfil.CLIENTE);

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login("cliente@demo.local", "Errada1234")))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:ecommerce:erro:nao-autenticado"))
                .andExpect(jsonPath("$.detail").value(MENSAGEM_GENERICA))
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }

    @Test
    void emailInexistenteRetorna401ComAMesmaMensagem() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login("ninguem@demo.local", UsuariosParaTeste.SENHA)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(MENSAGEM_GENERICA));
    }

    @Test
    void usuarioInativoRetorna401SemToken() throws Exception {
        helper.criarInativo("inativo@demo.local", Perfil.CLIENTE);

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login("inativo@demo.local", UsuariosParaTeste.SENHA)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(MENSAGEM_GENERICA))
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }

    @Test
    void loginSemCamposRetorna400() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    // ---- acesso exige token ----

    @Test
    void semTokenRetorna401EmProblemDetail() throws Exception {
        mvc.perform(get("/api/admin/usuarios"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:ecommerce:erro:nao-autenticado"))
                .andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").exists())
                .andExpect(jsonPath("$.instance").value("/api/admin/usuarios"));
    }

    @Test
    void rotaQualquerProtegidaSemTokenRetorna401() throws Exception {
        mvc.perform(get("/api/clientes/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void tokenExpiradoRetorna401() throws Exception {
        Usuario admin = helper.criar("admin@demo.local", Perfil.ADMIN);
        Instant agora = Instant.now();
        String expirado = token(agora.minusSeconds(7200), agora.minusSeconds(3600), encoder, admin);

        mvc.perform(get("/api/admin/usuarios").header("Authorization", "Bearer " + expirado))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:ecommerce:erro:nao-autenticado"));
    }

    @Test
    void tokenComAssinaturaInvalidaRetorna401() throws Exception {
        Usuario admin = helper.criar("admin@demo.local", Perfil.ADMIN);
        JwtEncoder outroSegredo = new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(
                "outro-segredo-qualquer-0123456789-abcdefgh".getBytes(StandardCharsets.UTF_8), "HmacSHA256")));
        Instant agora = Instant.now();
        String forjado = token(agora, agora.plusSeconds(3600), outroSegredo, admin);

        mvc.perform(get("/api/admin/usuarios").header("Authorization", "Bearer " + forjado))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void tokenAdulteradoRetorna401() throws Exception {
        Usuario admin = helper.criar("admin@demo.local", Perfil.ADMIN);
        String bearer = helper.bearer(admin);

        mvc.perform(get("/api/admin/usuarios").header("Authorization", bearer.substring(0, bearer.length() - 4) + "AAAA"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void endpointsPublicosAcessiveisSemToken() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        // cadastro de cliente (grupo 4): sem token nao pode ser barrado por autenticacao
        int statusCadastro = mvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andReturn().getResponse().getStatus();
        assertThat(statusCadastro).isNotIn(401, 403);
    }

    // ---- 403 ----

    @Test
    void perfilSemPermissaoRetorna403EmProblemDetail() throws Exception {
        Usuario cliente = helper.criar("cliente@demo.local", Perfil.CLIENTE);

        mvc.perform(get("/api/admin/usuarios").header("Authorization", helper.bearer(cliente)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:ecommerce:erro:acesso-negado"))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.detail").exists())
                .andExpect(jsonPath("$.instance").value("/api/admin/usuarios"));
    }
}
