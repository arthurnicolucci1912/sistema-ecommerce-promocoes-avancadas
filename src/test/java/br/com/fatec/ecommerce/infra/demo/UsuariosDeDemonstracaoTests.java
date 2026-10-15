package br.com.fatec.ecommerce.infra.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.fatec.ecommerce.TesteDeIntegracao;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.json.JsonMapper;

/**
 * Perfis "dev" e "test" ativos (o segredo JWT vem do "test", que vem por ultimo; o banco vem do Testcontainers,
 * que prevalece sobre o datasource do application-dev.yml). O runner ja rodou na subida do contexto.
 */
@TesteDeIntegracao
@ActiveProfiles({"dev", "test"})
class UsuariosDeDemonstracaoTests {

    private static final Map<String, String> PERFIL_POR_EMAIL = Map.of(
            "admin@demo.local", "ADMIN",
            "comercial@demo.local", "OPERADOR_COMERCIAL",
            "estoque@demo.local", "ESTOQUE",
            "promocoes@demo.local", "PROMOCOES",
            "cliente@demo.local", "CLIENTE");

    @Autowired
    MockMvc mvc;
    @Autowired
    UsuariosDeDemonstracao runner;
    @Autowired
    JdbcTemplate jdbc;

    private String login(String email) throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"senha\":\"Demo1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();
        return JsonMapper.builder().build().readTree(r.getResponse().getContentAsString()).get("accessToken").asString();
    }

    private long contar(String tabela) {
        return jdbc.queryForObject("select count(*) from " + tabela, Long.class);
    }

    @Test
    void execucaoEmDesenvolvimentoCriaOsUsuariosEOsFazLogarComSeusPerfis() throws Exception {
        for (Map.Entry<String, String> e : PERFIL_POR_EMAIL.entrySet()) {
            String token = login(e.getKey());
            assertThat(token).isNotBlank();
            List<String> perfis = jdbc.queryForList("select p.perfil from usuario_perfil p join usuario u "
                    + "on u.id = p.usuario_id where u.email = ?", String.class, e.getKey());
            assertThat(perfis).containsExactly(e.getValue());
        }
    }

    @Test
    void clienteDeDemonstracaoTemDadosFicticiosEUmEnderecoPrincipalEPodeConsultarOsProprios() throws Exception {
        String token = login("cliente@demo.local");

        mvc.perform(get("/api/clientes/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("cliente@demo.local"))
                .andExpect(jsonPath("$.cpf").value("52998224725"));
        mvc.perform(get("/api/clientes/me/enderecos").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[*].principal", hasItem(true)));
    }

    @Test
    void clienteDeDemonstracaoTemUmPedidoDeExemploAguardandoPagamentoCriadoUmaSoVez() throws Exception {
        runner.run(null);
        runner.run(null);

        String clienteDemo = "(select c.id from cliente c join usuario u on u.id = c.usuario_id "
                + "where u.email = 'cliente@demo.local')";
        assertThat(jdbc.queryForObject("select count(*) from pedido where cliente_id = " + clienteDemo, Long.class))
                .isEqualTo(1);

        String token = login("cliente@demo.local");
        mvc.perform(get("/api/clientes/me/pedidos").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.conteudo[0].situacao").value("AGUARDANDO_PAGAMENTO"))
                .andExpect(jsonPath("$.conteudo[0].total").value(173.20));
        Long id = jdbc.queryForObject("select id from pedido where cliente_id = " + clienteDemo, Long.class);
        mvc.perform(get("/api/clientes/me/pedidos/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(2))
                .andExpect(jsonPath("$.itens[0].nomeProduto").value("Camiseta Básica"))
                .andExpect(jsonPath("$.itens[0].descricaoVariacao").value("Tamanho M"))
                .andExpect(jsonPath("$.itens[0].quantidade").value(2))
                .andExpect(jsonPath("$.itens[0].precoUnitario").value(59.90))
                .andExpect(jsonPath("$.itens[1].nomeProduto").value("Caneca"))
                .andExpect(jsonPath("$.subtotal").value(154.70))
                .andExpect(jsonPath("$.totalDescontos").value(0.00))
                .andExpect(jsonPath("$.frete").value(18.50))
                .andExpect(jsonPath("$.total").value(173.20))
                .andExpect(jsonPath("$.enderecoEntrega.logradouro").value("Avenida Paulista"))
                .andExpect(jsonPath("$.enderecoEntrega.cep").value("01310100"))
                .andExpect(jsonPath("$.historico[*].para").value(
                        org.hamcrest.Matchers.contains("CRIADO", "AGUARDANDO_PAGAMENTO")));
    }

    @Test
    void reinicioEmDesenvolvimentoNaoDuplicaNada() throws Exception {
        long usuarios = contar("usuario");
        long perfis = contar("usuario_perfil");
        long clientes = contar("cliente");
        long enderecos = contar("endereco");

        runner.run(null);
        runner.run(null);

        assertThat(contar("usuario")).isEqualTo(usuarios).isEqualTo(5);
        assertThat(contar("usuario_perfil")).isEqualTo(perfis);
        assertThat(contar("cliente")).isEqualTo(clientes).isEqualTo(1);
        assertThat(contar("endereco")).isEqualTo(enderecos).isEqualTo(1);
    }
}
