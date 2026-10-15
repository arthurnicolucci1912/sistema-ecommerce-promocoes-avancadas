package br.com.fatec.ecommerce.compartilhado.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.fatec.ecommerce.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@Import({TestcontainersConfiguration.class, ErrosDeTesteController.class})
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class TratadorGlobalDeErrosTests {

    @Autowired
    MockMvc mvc;

    @Test
    void erroDeValidacaoListaOsDoisCamposInvalidos() throws Exception {
        mvc.perform(post("/teste/validacao").with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"\",\"email\":\"invalido\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").exists())
                .andExpect(jsonPath("$.instance").value("/teste/validacao"))
                .andExpect(jsonPath("$.campos", hasSize(2)))
                .andExpect(jsonPath("$.campos[?(@.campo == 'nome')].mensagem").value("o nome é obrigatório"))
                .andExpect(jsonPath("$.campos[?(@.campo == 'email')].mensagem").value("e-mail inválido"));
    }

    @Test
    void corpoIlegivelRetorna400() throws Exception {
        mvc.perform(post("/teste/validacao").with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content("{nao e json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.instance").value("/teste/validacao"));
    }

    @Test
    void recursoInexistenteRetorna404() throws Exception {
        mvc.perform(get("/teste/nao-encontrado").with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Pedido 99 não encontrado."))
                .andExpect(jsonPath("$.instance").value("/teste/nao-encontrado"));
    }

    @Test
    void conflitoRetorna409() throws Exception {
        mvc.perform(get("/teste/conflito").with(jwt()))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("E-mail já cadastrado."));
    }

    @Test
    void erroInesperadoRetorna500SemDetalhesInternos() throws Exception {
        mvc.perform(get("/teste/falha").with(jwt()))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").value("Ocorreu um erro inesperado. Tente novamente mais tarde."))
                .andExpect(content().string(not(containsString("br.com.fatec"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))))
                .andExpect(content().string(not(containsString("segredo interno"))));
    }

    @Test
    void acessoNegadoDentroDoControllerRetorna403EmProblemDetail() throws Exception {
        mvc.perform(get("/teste/protegido").with(jwt()))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:ecommerce:erro:acesso-negado"))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.instance").value("/teste/protegido"));
    }

    @Test
    void perfilCorretoPassaPeloPreAuthorize() throws Exception {
        mvc.perform(get("/teste/protegido")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }

    @Test
    void falhaDeAutenticacaoDentroDoControllerRetorna401EmProblemDetail() throws Exception {
        mvc.perform(get("/teste/autenticacao").with(jwt()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:ecommerce:erro:nao-autenticado"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(content().string(not(containsString("detalhe interno"))));
    }
}
