package br.com.fatec.ecommerce.carrinho.web;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.hasSize;
import br.com.fatec.ecommerce.TesteDeIntegracao;
import br.com.fatec.ecommerce.carrinho.aplicacao.GerenciarCarrinho;
import br.com.fatec.ecommerce.catalogo.CatalogoPersistidoParaTeste;
import br.com.fatec.ecommerce.catalogo.dominio.VariacaoProduto;
import br.com.fatec.ecommerce.cliente.ClientesParaTeste;
import br.com.fatec.ecommerce.cliente.ClientesParaTeste.ClienteDeTeste;
import br.com.fatec.ecommerce.seguranca.UsuariosParaTeste;
import br.com.fatec.ecommerce.seguranca.dominio.Perfil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@TesteDeIntegracao
@Import(CatalogoPersistidoParaTeste.class)
class CarrinhoWebTests {
    private static final String URL = "/api/clientes/me/carrinho";
    @Autowired MockMvc mvc;
    @Autowired UsuariosParaTeste usuarios;
    @Autowired ClientesParaTeste clientes;
    @Autowired CatalogoPersistidoParaTeste catalogo;
    @Autowired JdbcTemplate jdbc;
    @Autowired GerenciarCarrinho aplicacao;
    ClienteDeTeste cliente;
    ClienteDeTeste outro;
    VariacaoProduto cafe;
    String admin;

    @BeforeEach
    void preparar() {
        usuarios.limpar();
        cliente = clientes.criar("cliente@teste.local", 501);
        outro = clientes.criar("outro@teste.local", 502);
        admin = usuarios.bearer(usuarios.criar("admin@teste.local", Perfil.ADMIN));
        var categoria = catalogo.categoria("Casa", null);
        cafe = catalogo.variacao(categoria, "CAFE", "10.00");
        var caneca = catalogo.variacao(categoria, "CANECA", "20.00");
        var kit = catalogo.combo("KIT", "35.00", cafe, 2, caneca, 1);
        catalogo.combo("KIT-DUPLO", "65.00", kit, 2);
        jdbc.update("UPDATE saldo_estoque SET disponivel = 3 WHERE variacao_id = ?", cafe.getId());
        jdbc.update("UPDATE saldo_estoque SET disponivel = 10 WHERE variacao_id = ?", caneca.getId());
    }

    private ResultActions incluir(String token, String sku, String quantidade) throws Exception {
        return mvc.perform(post(URL + "/itens").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sku\":\"" + sku + "\",\"quantidade\":" + quantidade + "}"));
    }

    private ResultActions consultar(String token) throws Exception {
        return mvc.perform(get(URL).header("Authorization", token));
    }

    @Test
    void fluxoCompletoRecalculaEConsultaNaoCriaCarrinhoNemReservaEstoque() throws Exception {
        consultar(cliente.bearer()).andExpect(status().isOk()).andExpect(jsonPath("$.itens", hasSize(0)))
                .andExpect(jsonPath("$.total").value(0));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM carrinho", Integer.class)).isZero();
        incluir(cliente.bearer(), "CAFE", "2").andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotal").value(20)).andExpect(jsonPath("$.total").value(20))
                .andExpect(jsonPath("$.frete").value(0)).andExpect(jsonPath("$.descontos", hasSize(0)));
        incluir(cliente.bearer(), "CAFE", "1").andExpect(status().isOk())
                .andExpect(jsonPath("$.itens", hasSize(1))).andExpect(jsonPath("$.itens[0].quantidade").value(3));
        mvc.perform(put(URL + "/itens/CAFE").header("Authorization", cliente.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{\"quantidade\":2}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(20));
        // Mudanca de preco entre duas consultas e outro pedido consumindo saldo.
        jdbc.update("UPDATE item_vendavel SET preco = 12.50 WHERE id = ?", cafe.getId());
        jdbc.update("UPDATE saldo_estoque SET disponivel = 0 WHERE variacao_id = ?", cafe.getId());
        consultar(cliente.bearer()).andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[0].precoUnitario").value(12.50))
                .andExpect(jsonPath("$.total").value(25))
                .andExpect(jsonPath("$.insuficiencias[0].solicitada").value(2))
                .andExpect(jsonPath("$.insuficiencias[0].disponivel").value(0));
        mvc.perform(delete(URL + "/itens/ausente").header("Authorization", cliente.bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(25));
        mvc.perform(delete(URL + "/itens/CAFE").header("Authorization", cliente.bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.itens", hasSize(0)))
                .andExpect(jsonPath("$.total").value(0));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reserva_estoque", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT reservado FROM saldo_estoque WHERE variacao_id = ?",
                Integer.class, cafe.getId())).isZero();
        assertThat(jdbc.queryForObject("SELECT disponivel FROM saldo_estoque WHERE variacao_id = ?",
                Integer.class, cafe.getId())).isZero();
    }

    @Test
    void kitsAninhadosTemPrecoProprioEAvisoSomaComponentesCompartilhados() throws Exception {
        incluir(cliente.bearer(), "KIT-DUPLO", "1").andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(65))
                .andExpect(jsonPath("$.insuficiencias[0].variacaoId").value(cafe.getId()))
                .andExpect(jsonPath("$.insuficiencias[0].solicitada").value(4))
                .andExpect(jsonPath("$.insuficiencias[0].disponivel").value(3));
        incluir(cliente.bearer(), "CAFE", "2").andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(85))
                .andExpect(jsonPath("$.insuficiencias[0].solicitada").value(6));
        assertThat(jdbc.queryForObject("SELECT disponivel FROM saldo_estoque WHERE variacao_id = ?",
                Integer.class, cafe.getId())).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reserva_estoque", Integer.class)).isZero();
    }

    @Test
    void rejeitaDadosInvalidosSemAlterarSelecaoPersistida() throws Exception {
        incluir(cliente.bearer(), "CAFE", "2").andExpect(status().isOk());
        for (String quantidade : new String[]{"0", "-1", "1.5", "2147483648", "null"}) {
            incluir(cliente.bearer(), "CAFE", quantidade).andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
        }
        incluir(cliente.bearer(), " ", "1").andExpect(status().isBadRequest());
        incluir(cliente.bearer(), "inexistente", "1").andExpect(status().isNotFound());
        mvc.perform(put(URL + "/itens/ausente").header("Authorization", cliente.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{\"quantidade\":1}"))
                .andExpect(status().isNotFound());
        consultar(cliente.bearer()).andExpect(jsonPath("$.itens[0].quantidade").value(2));
        mvc.perform(put(URL + "/itens/CAFE").header("Authorization", cliente.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{\"quantidade\":2147483647}"))
                .andExpect(status().isOk());
        incluir(cliente.bearer(), "CAFE", "1").andExpect(status().isBadRequest());
        consultar(cliente.bearer()).andExpect(jsonPath("$.itens[0].quantidade").value(2147483647));
    }

    @Test
    void itemInativadoContinuaVisivelPodeSerReduzidoERemovidoMasNaoAumentado() throws Exception {
        incluir(cliente.bearer(), "CAFE", "2").andExpect(status().isOk());
        jdbc.update("UPDATE item_vendavel SET ativo = false WHERE id = ?", cafe.getId());
        consultar(cliente.bearer()).andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[0].disponivelParaVenda").value(false));
        incluir(cliente.bearer(), "CAFE", "1").andExpect(status().isConflict());
        incluir(cliente.bearer(), "KIT", "1").andExpect(status().isConflict());
        mvc.perform(put(URL + "/itens/CAFE").header("Authorization", cliente.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{\"quantidade\":3}"))
                .andExpect(status().isConflict());
        consultar(cliente.bearer()).andExpect(jsonPath("$.itens[0].quantidade").value(2));
        mvc.perform(put(URL + "/itens/CAFE").header("Authorization", cliente.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{\"quantidade\":1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.itens[0].quantidade").value(1));
        mvc.perform(delete(URL + "/itens/CAFE").header("Authorization", cliente.bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.itens", hasSize(0)));
    }

    @Test
    void autenticaRestringePerfisEIsolaClientesIgnorandoIdentidadeNoCorpo() throws Exception {
        mvc.perform(get(URL)).andExpect(status().isUnauthorized());
        mvc.perform(post(URL + "/itens").contentType(MediaType.APPLICATION_JSON)
                .content("{\"sku\":\"CAFE\",\"quantidade\":1}")).andExpect(status().isUnauthorized());
        consultar(admin).andExpect(status().isForbidden());
        incluir(admin, "CAFE", "1").andExpect(status().isForbidden());
        for (Perfil perfil : new Perfil[]{Perfil.ESTOQUE, Perfil.OPERADOR_COMERCIAL, Perfil.PROMOCOES}) {
            String token = usuarios.bearer(usuarios.criar(perfil + "@teste.local", perfil));
            consultar(token).andExpect(status().isForbidden());
            incluir(token, "CAFE", "1").andExpect(status().isForbidden());
        }
        incluir(cliente.bearer(), "CAFE", "2").andExpect(status().isOk());
        consultar(outro.bearer()).andExpect(jsonPath("$.itens", hasSize(0)));
        mvc.perform(post(URL + "/itens").header("Authorization", outro.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sku\":\"CANECA\",\"quantidade\":1,\"clienteId\":" + cliente.cliente().getId() + "}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.itens[0].sku").value("CANECA"));
        mvc.perform(delete(URL + "/itens/CAFE").header("Authorization", outro.bearer()))
                .andExpect(status().isOk());
        consultar(cliente.bearer()).andExpect(jsonPath("$.itens", hasSize(1)))
                .andExpect(jsonPath("$.itens[0].sku").value("CAFE"))
                .andExpect(jsonPath("$.itens[0].quantidade").value(2));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void aplicacaoTambemExigePerfilClienteSemDependerDaRota() {
        assertThatThrownBy(() -> aplicacao.consultar()).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> aplicacao.incluir("CAFE", 1)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> aplicacao.alterarQuantidade("CAFE", 1)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> aplicacao.remover("CAFE")).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void openApiDocumentaRotasEResumoEQuantidadeInteira() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['" + URL + "'].get").exists())
                .andExpect(jsonPath("$.paths['" + URL + "/itens'].post").exists())
                .andExpect(jsonPath("$.paths['" + URL + "/itens/{sku}'].put").exists())
                .andExpect(jsonPath("$.components.schemas.IncluirItem.properties.quantidade.type").value("integer"));
    }

    @Test
    void saldoAusenteEquivaleAZeroEConsultaNaoAlteraSelecao() throws Exception {
        jdbc.update("DELETE FROM saldo_estoque WHERE variacao_id = ?", cafe.getId());
        incluir(cliente.bearer(), "CAFE", "1").andExpect(status().isOk())
                .andExpect(jsonPath("$.insuficiencias[0].disponivel").value(0));
        consultar(cliente.bearer()).andExpect(jsonPath("$.itens[0].quantidade").value(1));
    }

    @Test
    void falhaNoRecalculoDepoisDeAlterarAgregadoFazRollbackCompleto() throws Exception {
        for (String sku : new String[]{"GRANDE-A", "GRANDE-B", "GRANDE-C"}) {
            catalogo.combo(sku, "1.00", cafe, Integer.MAX_VALUE);
        }
        incluir(cliente.bearer(), "GRANDE-A", "2147483647").andExpect(status().isOk());
        incluir(cliente.bearer(), "GRANDE-B", "2147483647").andExpect(status().isOk());
        // A terceira demanda estoura long durante o recalculo, apos a inclusao no agregado gerenciado.
        incluir(cliente.bearer(), "GRANDE-C", "2147483647").andExpect(status().isBadRequest());
        consultar(cliente.bearer()).andExpect(status().isOk()).andExpect(jsonPath("$.itens", hasSize(2)));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM item_carrinho", Integer.class)).isEqualTo(2);
    }
}
