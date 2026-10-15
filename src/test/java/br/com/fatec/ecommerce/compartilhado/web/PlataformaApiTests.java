package br.com.fatec.ecommerce.compartilhado.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.fatec.ecommerce.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Documentacao OpenAPI e verificacao de saude, ambas acessiveis sem token. */
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class PlataformaApiTests {

    @Autowired
    MockMvc mvc;

    @Test
    void especificacaoOpenApiDisponivelSemToken() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.info.title").value("E-commerce FATEC"))
                .andExpect(jsonPath("$.components.securitySchemes.bearer-jwt.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearer-jwt.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.bearer-jwt.bearerFormat").value("JWT"))
                .andExpect(jsonPath("$.security[0].bearer-jwt").exists());
    }

    @Test
    void healthRetornaUpSemDetalhes() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(content().string(not(containsString("components"))))
                .andExpect(content().string(not(containsString("details"))));
    }
}
