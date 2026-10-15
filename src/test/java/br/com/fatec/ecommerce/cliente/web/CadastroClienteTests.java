package br.com.fatec.ecommerce.cliente.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
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
import br.com.fatec.ecommerce.cliente.CpfsParaTeste;
import br.com.fatec.ecommerce.cliente.dominio.ClienteRepository;
import br.com.fatec.ecommerce.cliente.dominio.Cpf;
import br.com.fatec.ecommerce.seguranca.UsuariosParaTeste;
import br.com.fatec.ecommerce.seguranca.dominio.Perfil;
import br.com.fatec.ecommerce.seguranca.dominio.Usuario;
import br.com.fatec.ecommerce.seguranca.dominio.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/** Cadastro de cliente e consulta/alteracao dos proprios dados. */
@TesteDeIntegracao
class CadastroClienteTests {

    private static final String URL = "/api/clientes";

    @Autowired
    MockMvc mvc;
    @Autowired
    UsuariosParaTeste helper;
    @Autowired
    ClientesParaTeste clientesHelper;
    @Autowired
    UsuarioRepository usuarios;
    @Autowired
    ClienteRepository clientes;
    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void limpar() {
        helper.limpar();
    }

    private String corpo(String nome, String email, String senha, String cpf) {
        return "{\"nome\":\"" + nome + "\",\"email\":\"" + email + "\",\"senha\":\"" + senha + "\",\"cpf\":\""
                + cpf + "\",\"telefone\":\"11999990000\"}";
    }

    private org.springframework.test.web.servlet.ResultActions cadastrar(String corpo) throws Exception {
        return mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    // --- Criacao de conta ---

    @Test
    void cadastroValidoRetorna201ComLocationSemSenhaEOClienteFazLogin() throws Exception {
        cadastrar(corpo("Maria Silva", "Maria@Demo.local", "Demo1234", CpfsParaTeste.comMascara(1)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/clientes/me")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nome").value("Maria Silva"))
                .andExpect(jsonPath("$.email").value("maria@demo.local"))
                .andExpect(jsonPath("$.cpf").value(CpfsParaTeste.valido(1)))
                .andExpect(jsonPath("$.telefone").value("11999990000"))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(content().string(not(containsString("senha"))))
                .andExpect(content().string(not(containsString("{bcrypt}"))));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"maria@demo.local\",\"senha\":\"Demo1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        Usuario usuario = usuarios.buscarPorEmail("maria@demo.local").orElseThrow();
        assertThat(usuario.getPerfis()).containsExactly(Perfil.CLIENTE);
        assertThat(usuario.getSenhaHash()).isNotEqualTo("Demo1234");
        assertThat(clientes.buscarPorUsuarioId(usuario.getId())).isPresent();
    }

    @Test
    void emailJaCadastradoRetorna409SemCriarCliente() throws Exception {
        helper.criar("dup@demo.local", Perfil.ESTOQUE);

        cadastrar(corpo("Maria", "DUP@demo.local", "Demo1234", CpfsParaTeste.valido(2)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        assertThat(clientes.existePorCpf(Cpf.de(CpfsParaTeste.valido(2)))).isFalse();
    }

    @Test
    void cpfJaCadastradoRetorna409SemCriarUsuario() throws Exception {
        clientesHelper.criar("existente@demo.local", 3);

        cadastrar(corpo("Maria", "novo@demo.local", "Demo1234", CpfsParaTeste.comMascara(3)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        assertThat(usuarios.existePorEmail("novo@demo.local")).isFalse();
    }

    @Test
    void emailMalFormadoECpfInvalidoRetorna400ListandoOsDoisCampos() throws Exception {
        cadastrar(corpo("Maria", "nao-e-email", "Demo1234", "52998224724"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.campos[?(@.campo == 'email')]").exists())
                .andExpect(jsonPath("$.campos[?(@.campo == 'cpf')]").exists());

        assertThat(jdbc.queryForObject("select count(*) from usuario", Long.class)).isZero();
    }

    @Test
    void senhaFracaRetorna400IndicandoOCampoSenha() throws Exception {
        cadastrar(corpo("Maria", "maria@demo.local", "abcdefgh", CpfsParaTeste.valido(4)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[?(@.campo == 'senha')]").exists());
    }

    @Test
    void cadastroEPublicoEnaoAceitaCpfDeSequenciaRepetida() throws Exception {
        cadastrar(corpo("Maria", "maria@demo.local", "Demo1234", "111.111.111-11"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[?(@.campo == 'cpf')]").exists());
    }

    // --- Consulta e alteracao dos proprios dados ---

    @Test
    void consultarOsProprioDadosRetornaNomeEmailCpfETelefone() throws Exception {
        ClienteDeTeste a = clientesHelper.criar("a@demo.local", 10);
        clientesHelper.criar("b@demo.local", 11);

        mvc.perform(get(URL + "/me").header("Authorization", a.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Cliente 10"))
                .andExpect(jsonPath("$.email").value("a@demo.local"))
                .andExpect(jsonPath("$.cpf").value(CpfsParaTeste.valido(10)))
                .andExpect(content().string(not(containsString("senha"))))
                .andExpect(content().string(not(containsString("{bcrypt}"))));
    }

    @Test
    void alterarTelefoneEConsultaSeguinteMostraOValor() throws Exception {
        ClienteDeTeste a = clientesHelper.criar("a@demo.local", 12);

        mvc.perform(put(URL + "/me").header("Authorization", a.bearer()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Nome Novo\",\"telefone\":\"11988887777\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telefone").value("11988887777"));

        mvc.perform(get(URL + "/me").header("Authorization", a.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Nome Novo"))
                .andExpect(jsonPath("$.telefone").value("11988887777"));
    }

    @Test
    void emailECpfEnviadosNoPutSaoIgnorados() throws Exception {
        ClienteDeTeste a = clientesHelper.criar("a@demo.local", 13);

        mvc.perform(put(URL + "/me").header("Authorization", a.bearer()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Outro\",\"telefone\":\"11977776666\",\"email\":\"hacker@demo.local\","
                                + "\"cpf\":\"" + CpfsParaTeste.valido(99) + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("a@demo.local"))
                .andExpect(jsonPath("$.cpf").value(CpfsParaTeste.valido(13)));

        mvc.perform(get(URL + "/me").header("Authorization", a.bearer()))
                .andExpect(jsonPath("$.email").value("a@demo.local"))
                .andExpect(jsonPath("$.cpf").value(CpfsParaTeste.valido(13)))
                .andExpect(jsonPath("$.nome").value("Outro"));
        assertThat(usuarios.existePorEmail("hacker@demo.local")).isFalse();
    }

    @Test
    void alterarComNomeEmBrancoRetorna400() throws Exception {
        ClienteDeTeste a = clientesHelper.criar("a@demo.local", 14);

        mvc.perform(put(URL + "/me").header("Authorization", a.bearer()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"  \",\"telefone\":\"1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[?(@.campo == 'nome')]").exists());
    }

    @Test
    void semTokenRetorna401() throws Exception {
        mvc.perform(get(URL + "/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void usuarioAdministrativoSemClienteRecebe403() throws Exception {
        Usuario admin = helper.criar("admin@demo.local", Perfil.ADMIN);

        mvc.perform(get(URL + "/me").header("Authorization", helper.bearer(admin)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mvc.perform(put(URL + "/me").header("Authorization", helper.bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"x\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void usuarioComPerfilClienteSemRegistroDeClienteRetorna404() throws Exception {
        Usuario orfao = helper.criar("orfao@demo.local", Perfil.CLIENTE);

        mvc.perform(get(URL + "/me").header("Authorization", helper.bearer(orfao)))
                .andExpect(status().isNotFound());
        mvc.perform(get(URL + "/me/enderecos").header("Authorization", helper.bearer(orfao)))
                .andExpect(status().isNotFound());
    }
}
