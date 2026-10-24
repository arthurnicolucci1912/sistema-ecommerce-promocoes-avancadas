package br.com.fatec.ecommerce.auditoria;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.fatec.ecommerce.TesteDeIntegracao;
import br.com.fatec.ecommerce.auditoria.aplicacao.ConsultarAuditoria;
import br.com.fatec.ecommerce.auditoria.dominio.ConsultarRegistrosAuditoria;
import br.com.fatec.ecommerce.auditoria.dominio.FiltroAuditoria;
import br.com.fatec.ecommerce.compartilhado.dominio.OperacaoAdministrativa;
import br.com.fatec.ecommerce.compartilhado.dominio.PublicadorEventos;
import br.com.fatec.ecommerce.seguranca.UsuariosParaTeste;
import br.com.fatec.ecommerce.seguranca.dominio.Perfil;
import br.com.fatec.ecommerce.seguranca.dominio.Usuario;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@TesteDeIntegracao
class ConsultarAuditoriaTests {
    static final String URL = "/api/admin/auditoria";
    static final Instant T0 = Instant.parse("2026-10-23T12:00:00Z");
    static final Instant T1 = T0.plusSeconds(1);
    static final Instant T2 = T0.plusSeconds(2);
    @Autowired UsuariosParaTeste usuarios;
    @Autowired PublicadorEventos eventos;
    @Autowired PlatformTransactionManager transacoes;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired ConsultarAuditoria consultar;
    @MockitoSpyBean ConsultarRegistrosAuditoria leitura;
    Usuario admin;
    Usuario outro;
    String bearer;
    List<Long> ids;

    @BeforeEach void preparar() {
        SecurityContextHolder.clearContext(); usuarios.limpar();
        admin = usuarios.criar("admin-auditoria@teste.local", Perfil.ADMIN);
        outro = usuarios.criar("outro-auditoria@teste.local", Perfil.OPERADOR_COMERCIAL);
        bearer = usuarios.bearer(admin);
        new TransactionTemplate(transacoes).executeWithoutResult(tx -> {
            registrar(admin, T0, "VARIACAO_PRECO_ALTERADO");
            registrar(admin, T1, "VARIACAO_PRECO_ALTERADO");
            registrar(admin, T1, "VARIACAO_PRECO_ALTERADO");
            registrar(admin, T2, "VARIACAO_PRECO_ALTERADO");
            registrar(outro, T1, "VARIACAO_PRECO_ALTERADO");
            registrar(admin, T1, "CAMPANHA_CRIADA");
        });
        ids = jdbc.queryForList("select id from auditoria_operacao order by id", Long.class);
        clearInvocations(leitura);
    }
    @AfterEach void limpar() { SecurityContextHolder.clearContext(); }
    void registrar(Usuario ator, Instant momento, String operacao) {
        eventos.publicar(new OperacaoAdministrativa(ator.getId(), momento, operacao, "VARIACAO", 900L,
                Map.of("antes", Map.of("preco", 100), "depois", Map.of("preco", 120))));
    }
    JsonNode obter(MockHttpServletRequestBuilder request) throws Exception {
        return mapper.readTree(mvc.perform(request.header("Authorization", bearer)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }
    @Test void paginaPadraoRetornaDetalhesUtcTotalEOrdemDeterministicaSemEscrever() throws Exception {
        var r = obter(get(URL));
        assertThat(r.get("page").asInt()).isZero(); assertThat(r.get("size").asInt()).isEqualTo(20);
        assertThat(r.get("totalElementos").asLong()).isEqualTo(6); assertThat(r.get("totalPaginas").asInt()).isEqualTo(1);
        assertThat(r.get("conteudo").valueStream().map(n -> n.get("id").asLong()).toList())
                .containsExactly(ids.get(3), ids.get(5), ids.get(4), ids.get(2), ids.get(1), ids.get(0));
        var primeiro = r.get("conteudo").get(0);
        assertThat(primeiro.get("usuarioId").asLong()).isEqualTo(admin.getId());
        assertThat(primeiro.get("ocorridoEm").asString()).isEqualTo(T2.toString());
        assertThat(primeiro.get("operacao").asString()).isEqualTo("VARIACAO_PRECO_ALTERADO");
        assertThat(primeiro.get("recursoTipo").asString()).isEqualTo("VARIACAO");
        assertThat(primeiro.get("recursoId").asLong()).isEqualTo(900);
        assertThat(primeiro.at("/detalhes/antes/preco").asInt()).isEqualTo(100);
        assertThat(primeiro.at("/detalhes/depois/preco").asInt()).isEqualTo(120);
        assertThat(jdbc.queryForObject("select count(*) from auditoria_operacao",Integer.class)).isEqualTo(6);
    }
    @Test void filtrosCombinadosSaoInclusivosEPaginamComTotalCompleto() throws Exception {
        for (int page = 0; page <= 2; page++) {
            var r = obter(get(URL).param("usuarioId", admin.getId().toString()).param("operacao", "VARIACAO_PRECO_ALTERADO")
                    .param("inicio", T1.toString()).param("fim", T1.toString()).param("page", ""+page).param("size", "1"));
            assertThat(r.get("totalElementos").asLong()).isEqualTo(2); assertThat(r.get("totalPaginas").asInt()).isEqualTo(2);
            if (page < 2) assertThat(r.get("conteudo").get(0).get("id").asLong()).isEqualTo(ids.get(2-page));
            else assertThat(r.get("conteudo").size()).isZero();
        }
    }
    @Test void cadaFiltroFuncionaIsoladoEInstanteComOffsetRepresentaOMesmoLimite() throws Exception {
        assertThat(obter(get(URL).param("usuarioId",outro.getId().toString())).get("totalElementos").asInt()).isEqualTo(1);
        assertThat(obter(get(URL).param("operacao","VARIACAO_PRECO_ALTERADO")).get("totalElementos").asInt()).isEqualTo(5);
        assertThat(obter(get(URL).param("inicio",T1.toString())).get("totalElementos").asInt()).isEqualTo(5);
        assertThat(obter(get(URL).param("fim",T1.toString())).get("totalElementos").asInt()).isEqualTo(5);
        assertThat(obter(get(URL).param("inicio","2026-10-23T09:00:01-03:00").param("fim",T1.toString()))
                .get("totalElementos").asInt()).isEqualTo(4);
    }
    @Test void filtrosSemCorrespondenciaEOffsetGrandeRetornamPaginaVazia() throws Exception {
        for (var request : List.of(get(URL).param("operacao","OPERACAO_INEXISTENTE"),
                get(URL).param("usuarioId",Long.MAX_VALUE+""), get(URL).param("inicio",T2.plusSeconds(1).toString()))) {
            var r=obter(request); assertThat(r.get("conteudo").size()).isZero(); assertThat(r.get("totalElementos").asInt()).isZero();
        }
        var r=obter(get(URL).param("page",Integer.MAX_VALUE+"").param("size","100"));
        assertThat(r.get("conteudo").size()).isZero(); assertThat(r.get("totalElementos").asInt()).isEqualTo(6);
    }
    @Test void tamanhoMaximoLimitaConteudoNoBancoEMantemTotal() throws Exception {
        new TransactionTemplate(transacoes).executeWithoutResult(tx -> {
            for (int i=0;i<100;i++) registrar(admin,T2.plusSeconds(i),"ESTOQUE_AJUSTADO");
        });
        var r=obter(get(URL).param("size","100"));
        assertThat(r.get("conteudo").size()).isEqualTo(100); assertThat(r.get("totalElementos").asInt()).isEqualTo(106);
        assertThat(r.get("totalPaginas").asInt()).isEqualTo(2);
        assertThat(obter(get(URL).param("size","100").param("page","1")).get("conteudo").size()).isEqualTo(6);
    }
    @Test void filtrosInvalidosRetornam400AntesDeAcessarBanco() throws Exception {
        for (var parametro : List.of(new String[]{"page","-1"},new String[]{"page","abc"},new String[]{"size","0"},
                new String[]{"size","101"},new String[]{"size","-1"},new String[]{"usuarioId","0"},new String[]{"usuarioId","-1"},
                new String[]{"usuarioId","x"},new String[]{"usuarioId",""},new String[]{"usuarioId","9223372036854775808"},
                new String[]{"operacao",""},new String[]{"operacao"," "},new String[]{"operacao","minuscula"},
                new String[]{"operacao","A".repeat(101)},new String[]{"operacao","X' OR 1=1"},new String[]{"inicio",""},
                new String[]{"inicio","ontem"},new String[]{"fim","2026-10-23"},new String[]{"fim","2026-10-23T12:00:00"})) {
            mvc.perform(get(URL).header("Authorization",bearer).param(parametro[0],parametro[1]))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(get(URL).header("Authorization",bearer).param("inicio",T2.toString()).param("fim",T0.toString()))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(leitura);
    }
    @Test void somenteAdminPodeConsultarNaApi() throws Exception {
        mvc.perform(get(URL)).andExpect(status().isUnauthorized());
        for (Perfil perfil : List.of(Perfil.CLIENTE,Perfil.OPERADOR_COMERCIAL,Perfil.ESTOQUE,Perfil.PROMOCOES)) {
            var u=usuarios.criar(perfil.name()+"@teste.local",perfil);
            mvc.perform(get(URL).header("Authorization",usuarios.bearer(u))).andExpect(status().isForbidden());
        }
        verifyNoInteractions(leitura);
    }
    @Test void aplicacaoTambemNegaConsultaSemAdmin() {
        var filtro=new FiltroAuditoria(null,null,null,null,0,20);
        assertThatThrownBy(() -> consultar.consultar(filtro)).isInstanceOf(AuthenticationCredentialsNotFoundException.class);
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
                Jwt.withTokenValue("teste").header("alg","none").subject(outro.getId().toString()).build(),
                List.of(new SimpleGrantedAuthority("ROLE_OPERADOR_COMERCIAL"))));
        assertThatThrownBy(() -> consultar.consultar(filtro)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(leitura);
    }
    @Test void apiNaoOfereceCriacaoEdicaoOuExclusaoMesmoParaAdmin() throws Exception {
        mvc.perform(post(URL).header("Authorization",bearer).contentType("application/json").content("{}"))
                .andExpect(status().isMethodNotAllowed());
        for (var request : List.of(put(URL+"/"+ids.getFirst()),patch(URL+"/"+ids.getFirst()),delete(URL+"/"+ids.getFirst()))) {
            mvc.perform(request.header("Authorization",bearer).contentType("application/json").content("{}"))
                    .andExpect(status().isNotFound());
        }
        assertThat(jdbc.queryForObject("select count(*) from auditoria_operacao",Integer.class)).isEqualTo(6);
        verifyNoInteractions(leitura);
    }
    @Test void openApiDescreveFiltrosRespostasEApenasLeitura() throws Exception {
        var paths=mapper.readTree(mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("paths");
        var rota=paths.get(URL); assertThat(rota.has("get")).isTrue(); assertThat(rota.has("post")).isFalse();
        var get=rota.get("get");
        assertThat(get.get("responses").propertyNames()).contains("200","400","401","403");
        assertThat(get.get("parameters").valueStream().map(n->n.get("name").asString()).toList())
                .containsExactlyInAnyOrder("usuarioId","operacao","inicio","fim","page","size");
        assertThat(paths.has(URL+"/{id}")).isFalse();
    }
}
