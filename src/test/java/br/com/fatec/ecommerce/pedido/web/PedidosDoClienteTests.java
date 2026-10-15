package br.com.fatec.ecommerce.pedido.web;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
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

/** Endpoints do cliente: listagem paginada, detalhe, isolamento entre clientes e cancelamento. */
@TesteDeIntegracao
@Import(PedidosPersistidosParaTeste.class)
class PedidosDoClienteTests {

    private static final String URL = "/api/clientes/me/pedidos";

    @Autowired
    MockMvc mvc;
    @Autowired
    UsuariosParaTeste helper;
    @Autowired
    ClientesParaTeste clientesHelper;
    @Autowired
    PedidosPersistidosParaTeste pedidos;

    ClienteDeTeste a;
    ClienteDeTeste b;

    @BeforeEach
    void preparar() {
        helper.limpar();
        a = clientesHelper.criar("a@demo.local", 40);
        b = clientesHelper.criar("b@demo.local", 41);
    }

    private static String motivo(String texto) {
        return "{\"motivo\":\"" + texto + "\"}";
    }

    // --- Listar os proprios pedidos ---

    @Test
    void listaOsProprioPedidosDoMaisRecenteParaOMaisAntigoSemPedidosDeOutroCliente() throws Exception {
        Long primeiro = pedidos.criar(a.cliente().getId(), SituacaoPedido.AGUARDANDO_PAGAMENTO);
        Long segundo = pedidos.criar(a.cliente().getId(), SituacaoPedido.PAGO);
        pedidos.criar(b.cliente().getId(), SituacaoPedido.AGUARDANDO_PAGAMENTO);

        mvc.perform(get(URL).header("Authorization", a.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo", hasSize(2)))
                .andExpect(jsonPath("$.conteudo[*].id", contains(segundo.intValue(), primeiro.intValue())))
                .andExpect(jsonPath("$.conteudo[0].situacao").value("PAGO"))
                .andExpect(jsonPath("$.conteudo[0].total").value(415.00))
                .andExpect(jsonPath("$.conteudo[0].criadoEm").isNotEmpty())
                .andExpect(jsonPath("$.conteudo[0].itens").doesNotExist())
                .andExpect(jsonPath("$.totalElementos").value(2))
                .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    void listagemEPaginadaPorPageESize() throws Exception {
        Long um = pedidos.criar(a.cliente().getId(), SituacaoPedido.CRIADO);
        Long dois = pedidos.criar(a.cliente().getId(), SituacaoPedido.CRIADO);
        Long tres = pedidos.criar(a.cliente().getId(), SituacaoPedido.CRIADO);

        mvc.perform(get(URL).param("page", "0").param("size", "2").header("Authorization", a.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[*].id", contains(tres.intValue(), dois.intValue())))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElementos").value(3))
                .andExpect(jsonPath("$.totalPaginas").value(2));
        mvc.perform(get(URL).param("page", "1").param("size", "2").header("Authorization", a.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[*].id", contains(um.intValue())));
    }

    @Test
    void paginaOuTamanhoInvalidosResponde400() throws Exception {
        mvc.perform(get(URL).param("page", "-1").header("Authorization", a.bearer()))
                .andExpect(status().isBadRequest());
        mvc.perform(get(URL).param("size", "0").header("Authorization", a.bearer()))
                .andExpect(status().isBadRequest());
    }

    // --- Detalhe ---

    @Test
    void detalheTrazSnapshotSituacaoEHistoricoCronologicoComResponsavel() throws Exception {
        Long id = pedidos.criar(a.cliente().getId(), SituacaoPedido.EM_PREPARACAO);

        mvc.perform(get(URL + "/" + id).header("Authorization", a.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.situacao").value("EM_PREPARACAO"))
                .andExpect(jsonPath("$.itens", hasSize(2)))
                .andExpect(jsonPath("$.itens[0].nomeProduto").value("Camiseta"))
                .andExpect(jsonPath("$.itens[0].quantidade").value(2))
                .andExpect(jsonPath("$.itens[0].precoUnitario").value(100.00))
                .andExpect(jsonPath("$.itens[0].totalItem").value(200.00))
                .andExpect(jsonPath("$.descontos", hasSize(1)))
                .andExpect(jsonPath("$.descontos[0].descricao").value("Promo de inverno"))
                .andExpect(jsonPath("$.descontos[0].valor").value(20.00))
                .andExpect(jsonPath("$.frete").value(15.00))
                .andExpect(jsonPath("$.subtotal").value(420.00))
                .andExpect(jsonPath("$.totalDescontos").value(20.00))
                .andExpect(jsonPath("$.total").value(415.00))
                .andExpect(jsonPath("$.enderecoEntrega.cep").value("13010000"))
                .andExpect(jsonPath("$.enderecoEntrega.cidade").value("Campinas"))
                .andExpect(jsonPath("$.historico[*].para",
                        contains("CRIADO", "AGUARDANDO_PAGAMENTO", "PAGO", "EM_PREPARACAO")))
                .andExpect(jsonPath("$.historico[0].de").value(nullValue()))
                .andExpect(jsonPath("$.historico[0].responsavel.tipo").value("SISTEMA"))
                .andExpect(jsonPath("$.historico[3].de").value("PAGO"))
                .andExpect(jsonPath("$.historico[3].responsavel.tipo").value("ADMINISTRATIVO"))
                .andExpect(jsonPath("$.historico[3].responsavel.usuarioId").value(1));
    }

    // --- Pedido de outro cliente ---

    @Test
    void pedidoDeOutroClienteResponde404AoConsultarECancelaSemAlterarOPedido() throws Exception {
        Long doB = pedidos.criar(b.cliente().getId(), SituacaoPedido.AGUARDANDO_PAGAMENTO);

        mvc.perform(get(URL + "/" + doB).header("Authorization", a.bearer()))
                .andExpect(status().isNotFound());
        mvc.perform(post(URL + "/" + doB + "/cancelamento").header("Authorization", a.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(motivo("não é meu")))
                .andExpect(status().isNotFound());

        mvc.perform(get(URL + "/" + doB).header("Authorization", b.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("AGUARDANDO_PAGAMENTO"));
    }

    @Test
    void pedidoInexistenteResponde404() throws Exception {
        mvc.perform(get(URL + "/999999").header("Authorization", a.bearer())).andExpect(status().isNotFound());
    }

    // --- Cancelamento pelo cliente ---

    @Test
    void clienteCancelaPedidoAguardandoPagamentoERegistraSeusDadosNoHistorico() throws Exception {
        Long id = pedidos.criar(a.cliente().getId(), SituacaoPedido.AGUARDANDO_PAGAMENTO);

        mvc.perform(post(URL + "/" + id + "/cancelamento").header("Authorization", a.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(motivo("mudei de ideia")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("CANCELADO"));

        mvc.perform(get(URL + "/" + id).header("Authorization", a.bearer()))
                .andExpect(jsonPath("$.situacao").value("CANCELADO"))
                .andExpect(jsonPath("$.historico", hasSize(3)))
                .andExpect(jsonPath("$.historico[2].de").value("AGUARDANDO_PAGAMENTO"))
                .andExpect(jsonPath("$.historico[2].para").value("CANCELADO"))
                .andExpect(jsonPath("$.historico[2].motivo").value("mudei de ideia"))
                .andExpect(jsonPath("$.historico[2].responsavel.tipo").value("CLIENTE"))
                .andExpect(jsonPath("$.historico[2].responsavel.usuarioId").value(a.usuario().getId()));
    }

    @Test
    void clienteNaoCancelaPedidoPagoEORecebe409ComSituacaoEOperacao() throws Exception {
        Long id = pedidos.criar(a.cliente().getId(), SituacaoPedido.PAGO);

        mvc.perform(post(URL + "/" + id + "/cancelamento").header("Authorization", a.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(motivo("desisti")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.situacaoAtual").value("PAGO"))
                .andExpect(jsonPath("$.operacao").value("CANCELAR"));

        mvc.perform(get(URL + "/" + id).header("Authorization", a.bearer()))
                .andExpect(jsonPath("$.situacao").value("PAGO"));
    }

    @Test
    void cancelamentoSemMotivoResponde400EOPedidoNaoMuda() throws Exception {
        Long id = pedidos.criar(a.cliente().getId(), SituacaoPedido.AGUARDANDO_PAGAMENTO);

        mvc.perform(post(URL + "/" + id + "/cancelamento").header("Authorization", a.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post(URL + "/" + id + "/cancelamento").header("Authorization", a.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(motivo("   ")))
                .andExpect(status().isBadRequest());

        mvc.perform(get(URL + "/" + id).header("Authorization", a.bearer()))
                .andExpect(jsonPath("$.situacao").value("AGUARDANDO_PAGAMENTO"))
                .andExpect(jsonPath("$.historico[*].para", not(hasItem("CANCELADO"))));
    }

    // --- Acesso ---

    @Test
    void semTokenResponde401EPerfilAdministrativoResponde403() throws Exception {
        mvc.perform(get(URL)).andExpect(status().isUnauthorized());
        String admin = helper.bearer(helper.criar("admin@demo.local", Perfil.ADMIN));
        mvc.perform(get(URL).header("Authorization", admin)).andExpect(status().isForbidden());
    }
}
