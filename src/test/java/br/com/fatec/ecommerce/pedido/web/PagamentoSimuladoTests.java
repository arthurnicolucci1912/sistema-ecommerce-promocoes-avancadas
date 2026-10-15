package br.com.fatec.ecommerce.pedido.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.fatec.ecommerce.TesteDeIntegracao;
import br.com.fatec.ecommerce.cliente.ClientesParaTeste;
import br.com.fatec.ecommerce.cliente.ClientesParaTeste.ClienteDeTeste;
import br.com.fatec.ecommerce.pedido.PedidosPersistidosParaTeste;
import br.com.fatec.ecommerce.pedido.dominio.SituacaoPedido;
import br.com.fatec.ecommerce.seguranca.UsuariosParaTeste;
import br.com.fatec.ecommerce.seguranca.dominio.Perfil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Simulacao temporaria do resultado do pagamento (removida na etapa 6): so ADMIN, responsavel SISTEMA. */
@TesteDeIntegracao
@Import(PedidosPersistidosParaTeste.class)
class PagamentoSimuladoTests {

    @Autowired
    MockMvc mvc;
    @Autowired
    UsuariosParaTeste helper;
    @Autowired
    ClientesParaTeste clientesHelper;
    @Autowired
    PedidosPersistidosParaTeste pedidos;

    ClienteDeTeste cliente;
    String admin;

    @BeforeEach
    void preparar() {
        helper.limpar();
        cliente = clientesHelper.criar("cliente@demo.local", 60);
        admin = helper.bearer(helper.criar("admin@demo.local", Perfil.ADMIN));
    }

    private String url(Long id) {
        return "/api/admin/pedidos/" + id + "/pagamento-simulado";
    }

    private static String corpo(boolean aprovado) {
        return "{\"aprovado\":" + aprovado + "}";
    }

    @Test
    void aprovacaoSimuladaLevaAPagoEOHistoricoIndicaSimulacao() throws Exception {
        Long id = pedidos.criar(cliente.cliente().getId(), SituacaoPedido.AGUARDANDO_PAGAMENTO);

        mvc.perform(post(url(id)).header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("PAGO"))
                .andExpect(jsonPath("$.historico[2].de").value("AGUARDANDO_PAGAMENTO"))
                .andExpect(jsonPath("$.historico[2].para").value("PAGO"))
                .andExpect(jsonPath("$.historico[2].motivo").value("pagamento simulado"))
                .andExpect(jsonPath("$.historico[2].responsavel.tipo").value("SISTEMA"));
    }

    @Test
    void recusaSimuladaLevaAPagamentoRecusado() throws Exception {
        Long id = pedidos.criar(cliente.cliente().getId(), SituacaoPedido.AGUARDANDO_PAGAMENTO);

        mvc.perform(post(url(id)).header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("PAGAMENTO_RECUSADO"))
                .andExpect(jsonPath("$.historico[2].motivo").value("pagamento simulado"))
                .andExpect(jsonPath("$.historico[2].responsavel.tipo").value("SISTEMA"));
    }

    @Test
    void pagamentoRecusadoNaoReabreOPedido() throws Exception {
        Long id = pedidos.criar(cliente.cliente().getId(), SituacaoPedido.PAGAMENTO_RECUSADO);

        mvc.perform(post(url(id)).header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(true)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.situacaoAtual").value("PAGAMENTO_RECUSADO"))
                .andExpect(jsonPath("$.operacao").value("CONFIRMAR_PAGAMENTO"));

        mvc.perform(get("/api/admin/pedidos/" + id).header("Authorization", admin))
                .andExpect(jsonPath("$.situacao").value("PAGAMENTO_RECUSADO"));
    }

    @Test
    void somenteAdminSimulaECorpoSemAprovadoResponde400() throws Exception {
        Long id = pedidos.criar(cliente.cliente().getId(), SituacaoPedido.AGUARDANDO_PAGAMENTO);
        String comercial = helper.bearer(helper.criar("comercial@demo.local", Perfil.OPERADOR_COMERCIAL));

        mvc.perform(post(url(id)).header("Authorization", comercial).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(true)))
                .andExpect(status().isForbidden());
        mvc.perform(post(url(id)).header("Authorization", cliente.bearer()).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(true)))
                .andExpect(status().isForbidden());
        mvc.perform(post(url(id)).header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/admin/pedidos/" + id).header("Authorization", admin))
                .andExpect(jsonPath("$.situacao").value("AGUARDANDO_PAGAMENTO"));
    }
}
