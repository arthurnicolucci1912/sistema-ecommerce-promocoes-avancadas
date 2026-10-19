package br.com.fatec.ecommerce.estoque.web;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.fatec.ecommerce.TesteDeIntegracao;
import br.com.fatec.ecommerce.catalogo.CatalogoPersistidoParaTeste;
import br.com.fatec.ecommerce.catalogo.dominio.VariacaoProduto;
import br.com.fatec.ecommerce.estoque.EstoqueParaTeste;
import br.com.fatec.ecommerce.seguranca.UsuariosParaTeste;
import br.com.fatec.ecommerce.seguranca.dominio.Perfil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** API administrativa de estoque: perfis por rota, consulta de saldo, estoque baixo e ajuste com motivo. */
@TesteDeIntegracao
@Import({CatalogoPersistidoParaTeste.class, EstoqueParaTeste.class})
class EstoqueWebTests {

    @Autowired
    MockMvc mvc;
    @Autowired
    UsuariosParaTeste helper;
    @Autowired
    CatalogoPersistidoParaTeste catalogo;
    @Autowired
    EstoqueParaTeste saldos;

    String admin;
    String comercial;
    String estoque;
    VariacaoProduto baixa;
    VariacaoProduto alta;

    @BeforeEach
    void preparar() {
        helper.limpar();
        admin = helper.bearer(helper.criar("admin@demo.local", Perfil.ADMIN));
        comercial = helper.bearer(helper.criar("comercial@demo.local", Perfil.OPERADOR_COMERCIAL));
        estoque = helper.bearer(helper.criar("estoque@demo.local", Perfil.ESTOQUE));
        var categoria = catalogo.categoria("Eletrônicos", null);
        baixa = catalogo.variacao(categoria, "TECLADO", "300.00");
        alta = catalogo.variacao(categoria, "MOUSE", "100.00");
        saldos.definirSaldo(baixa.getId(), 3, 2);
        saldos.definirSaldo(alta.getId(), 50, 0);
    }

    private static String ajuste(int quantidade, String motivo) {
        return "{\"quantidade\":" + quantidade + ",\"motivo\":\"" + motivo + "\"}";
    }

    @Test
    void estoqueBaixoListaSomenteVariacoesAbaixoDoLimite() throws Exception {
        mvc.perform(get("/api/admin/estoque").param("abaixoDe", "5").header("Authorization", estoque))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].variacaoId").value(baixa.getId()))
                .andExpect(jsonPath("$[0].disponivel").value(3))
                .andExpect(jsonPath("$[0].reservado").value(2));
        // O limite e exclusivo: 3 nao esta abaixo de 3.
        mvc.perform(get("/api/admin/estoque").param("abaixoDe", "3").header("Authorization", admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/admin/estoque").param("abaixoDe", "100").header("Authorization", admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void estoqueBaixoSemLimiteRespondeComErroDeValidacao() throws Exception {
        mvc.perform(get("/api/admin/estoque").header("Authorization", estoque))
                .andExpect(status().isBadRequest());
    }

    @Test
    void consultaDoSaldoDeUmaVariacao() throws Exception {
        mvc.perform(get("/api/admin/estoque/{id}", baixa.getId()).header("Authorization", estoque))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.variacaoId").value(baixa.getId()))
                .andExpect(jsonPath("$.disponivel").value(3))
                .andExpect(jsonPath("$.reservado").value(2));
        mvc.perform(get("/api/admin/estoque/999999").header("Authorization", estoque))
                .andExpect(status().isNotFound());
    }

    @Test
    void ajustePositivoENegativoAtualizamODisponivel() throws Exception {
        mvc.perform(post("/api/admin/estoque/{id}/ajustes", baixa.getId()).header("Authorization", estoque)
                        .contentType(MediaType.APPLICATION_JSON).content(ajuste(10, "compra do fornecedor")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.disponivel").value(13));
        mvc.perform(post("/api/admin/estoque/{id}/ajustes", baixa.getId()).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(ajuste(-4, "perda")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.disponivel").value(9));
    }

    @Test
    void ajusteQueDeixariaNegativoRespondeConflitoESemMotivoOuZeroRespondem400() throws Exception {
        mvc.perform(post("/api/admin/estoque/{id}/ajustes", baixa.getId()).header("Authorization", estoque)
                        .contentType(MediaType.APPLICATION_JSON).content(ajuste(-5, "inventário")))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/admin/estoque/{id}/ajustes", baixa.getId()).header("Authorization", estoque)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantidade\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[?(@.campo == 'motivo')]").exists());
        mvc.perform(post("/api/admin/estoque/{id}/ajustes", baixa.getId()).header("Authorization", estoque)
                        .contentType(MediaType.APPLICATION_JSON).content(ajuste(0, "nada")))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/admin/estoque/{id}", baixa.getId()).header("Authorization", estoque))
                .andExpect(jsonPath("$.disponivel").value(3));
        mvc.perform(post("/api/admin/estoque/999999/ajustes").header("Authorization", estoque)
                        .contentType(MediaType.APPLICATION_JSON).content(ajuste(1, "x")))
                .andExpect(status().isNotFound());
    }

    @Test
    void perfilComercialAjustaEstoqueRecebe403() throws Exception {
        mvc.perform(post("/api/admin/estoque/{id}/ajustes", baixa.getId()).header("Authorization", comercial)
                        .contentType(MediaType.APPLICATION_JSON).content(ajuste(10, "compra")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/estoque").param("abaixoDe", "5").header("Authorization", comercial))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/estoque/{id}", baixa.getId()).header("Authorization", comercial))
                .andExpect(status().isForbidden());
    }

    @Test
    void visitanteNaoAcessaEstoque() throws Exception {
        mvc.perform(post("/api/admin/estoque/{id}/ajustes", baixa.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(ajuste(10, "compra")))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/estoque").param("abaixoDe", "5")).andExpect(status().isUnauthorized());
    }
}
