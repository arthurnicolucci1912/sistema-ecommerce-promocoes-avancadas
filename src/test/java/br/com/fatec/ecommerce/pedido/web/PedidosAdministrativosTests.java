package br.com.fatec.ecommerce.pedido.web;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
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
import br.com.fatec.ecommerce.seguranca.dominio.Usuario;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Endpoints administrativos de pedidos: perfis por rota, avanco do andamento, cancelamento e 409 de transicao. */
@TesteDeIntegracao
@Import(PedidosPersistidosParaTeste.class)
class PedidosAdministrativosTests {

    private static final String URL = "/api/admin/pedidos";

    @Autowired
    MockMvc mvc;
    @Autowired
    UsuariosParaTeste helper;
    @Autowired
    ClientesParaTeste clientesHelper;
    @Autowired
    PedidosPersistidosParaTeste pedidos;

    ClienteDeTeste cliente;
    Usuario adminUsuario;
    String admin;
    String comercial;
    String estoque;
    String promocoes;

    @BeforeEach
    void preparar() {
        helper.limpar();
        cliente = clientesHelper.criar("cliente@demo.local", 50);
        adminUsuario = helper.criar("admin@demo.local", Perfil.ADMIN);
        admin = helper.bearer(adminUsuario);
        comercial = helper.bearer(helper.criar("comercial@demo.local", Perfil.OPERADOR_COMERCIAL));
        estoque = helper.bearer(helper.criar("estoque@demo.local", Perfil.ESTOQUE));
        promocoes = helper.bearer(helper.criar("promocoes@demo.local", Perfil.PROMOCOES));
    }

    private static String motivo(String texto) {
        return "{\"motivo\":\"" + texto + "\"}";
    }

    private Long pedido(SituacaoPedido situacao) {
        return pedidos.criar(cliente.cliente().getId(), situacao);
    }

    // --- Consulta ---

    @Test
    void filtrarPorEstadoDevolveApenasPedidosNaquelaSituacao() throws Exception {
        Long pago = pedido(SituacaoPedido.PAGO);
        pedido(SituacaoPedido.AGUARDANDO_PAGAMENTO);
        pedido(SituacaoPedido.ENVIADO);

        mvc.perform(get(URL).param("situacao", "PAGO").header("Authorization", comercial))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo", hasSize(1)))
                .andExpect(jsonPath("$.conteudo[0].id").value(pago))
                .andExpect(jsonPath("$.conteudo[*].situacao", everyItem(equalTo("PAGO"))));
    }

    @Test
    void semFiltroListaPedidosDeQualquerClienteComPaginacao() throws Exception {
        ClienteDeTeste outro = clientesHelper.criar("outro@demo.local", 51);
        pedido(SituacaoPedido.PAGO);
        pedidos.criar(outro.cliente().getId(), SituacaoPedido.CRIADO);

        mvc.perform(get(URL).header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo", hasSize(2)))
                .andExpect(jsonPath("$.conteudo[0].clienteId").value(outro.cliente().getId()))
                .andExpect(jsonPath("$.totalElementos").value(2));
        mvc.perform(get(URL).param("size", "1").header("Authorization", admin))
                .andExpect(jsonPath("$.conteudo", hasSize(1)))
                .andExpect(jsonPath("$.totalPaginas").value(2));
    }

    @Test
    void situacaoDesconhecidaNoFiltroResponde400() throws Exception {
        mvc.perform(get(URL).param("situacao", "INEXISTENTE").header("Authorization", admin))
                .andExpect(status().isBadRequest());
    }

    @Test
    void detalheDePedidoDeQualquerClienteParaAdminEComercialE404QuandoNaoExiste() throws Exception {
        Long id = pedido(SituacaoPedido.PAGO);

        for (String perfil : List.of(admin, comercial)) {
            mvc.perform(get(URL + "/" + id).header("Authorization", perfil))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.situacao").value("PAGO"))
                    .andExpect(jsonPath("$.historico", hasSize(3)));
        }
        mvc.perform(get(URL + "/999999").header("Authorization", admin)).andExpect(status().isNotFound());
    }

    // --- Andamento ---

    @Test
    void estoqueAvancaOAndamentoAteAEntrega() throws Exception {
        Long id = pedido(SituacaoPedido.PAGO);

        mvc.perform(post(URL + "/" + id + "/preparacao").header("Authorization", estoque))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("EM_PREPARACAO"));
        mvc.perform(post(URL + "/" + id + "/envio").header("Authorization", estoque))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("ENVIADO"));
        mvc.perform(post(URL + "/" + id + "/entrega").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("ENTREGUE"))
                .andExpect(jsonPath("$.historico[3].responsavel.tipo").value("ADMINISTRATIVO"))
                .andExpect(jsonPath("$.historico[5].responsavel.tipo").value("ADMINISTRATIVO"))
                .andExpect(jsonPath("$.historico[5].responsavel.usuarioId").value(adminUsuario.getId()));
    }

    // --- Perfis ---

    @Test
    void perfilSemPermissaoRecebe403NasRotasDePedido() throws Exception {
        Long pago = pedido(SituacaoPedido.PAGO);
        Long preparacao = pedido(SituacaoPedido.EM_PREPARACAO);

        mvc.perform(post(URL + "/" + preparacao + "/envio").header("Authorization", promocoes))
                .andExpect(status().isForbidden());
        mvc.perform(post(URL + "/" + preparacao + "/envio").header("Authorization", comercial))
                .andExpect(status().isForbidden());
        mvc.perform(post(URL + "/" + pago + "/preparacao").header("Authorization", comercial))
                .andExpect(status().isForbidden());
        mvc.perform(get(URL).header("Authorization", estoque)).andExpect(status().isForbidden());
        mvc.perform(get(URL + "/" + pago).header("Authorization", promocoes)).andExpect(status().isForbidden());
        mvc.perform(post(URL + "/" + pago + "/cancelamento").header("Authorization", estoque)
                        .contentType(MediaType.APPLICATION_JSON).content(motivo("x")))
                .andExpect(status().isForbidden());

        mvc.perform(get(URL + "/" + preparacao).header("Authorization", admin))
                .andExpect(jsonPath("$.situacao").value("EM_PREPARACAO"));
    }

    @Test
    void clienteNaAreaAdministrativaRecebe403ESemTokenRecebe401() throws Exception {
        mvc.perform(get(URL).header("Authorization", cliente.bearer())).andExpect(status().isForbidden());
        mvc.perform(get(URL)).andExpect(status().isUnauthorized());
    }

    @Test
    void liberarAsRotasDePedidoNaoAbreAAreaDeUsuariosAdministrativos() throws Exception {
        for (String perfil : List.of(comercial, estoque, promocoes)) {
            mvc.perform(get("/api/admin/usuarios").header("Authorization", perfil))
                    .andExpect(status().isForbidden());
        }
    }

    // --- Transicoes invalidas ---

    @Test
    void cancelarPedidoEnviadoResponde409InformandoEstadoEOperacao() throws Exception {
        Long id = pedido(SituacaoPedido.ENVIADO);

        mvc.perform(post(URL + "/" + id + "/cancelamento").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(motivo("tarde demais")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.situacaoAtual").value("ENVIADO"))
                .andExpect(jsonPath("$.operacao").value("CANCELAR"))
                .andExpect(jsonPath("$.status").value(409));

        mvc.perform(get(URL + "/" + id).header("Authorization", admin))
                .andExpect(jsonPath("$.situacao").value("ENVIADO"))
                .andExpect(jsonPath("$.historico", hasSize(5)));
    }

    @Test
    void pularEtapaResponde409EOPedidoNaoMuda() throws Exception {
        Long id = pedido(SituacaoPedido.AGUARDANDO_PAGAMENTO);

        mvc.perform(post(URL + "/" + id + "/envio").header("Authorization", estoque))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.situacaoAtual").value("AGUARDANDO_PAGAMENTO"))
                .andExpect(jsonPath("$.operacao").value("ENVIAR"));

        mvc.perform(get(URL + "/" + id).header("Authorization", admin))
                .andExpect(jsonPath("$.situacao").value("AGUARDANDO_PAGAMENTO"));
    }

    @Test
    void estadosFinaisRejeitamQualquerTransicao() throws Exception {
        for (SituacaoPedido final_ : List.of(SituacaoPedido.ENTREGUE, SituacaoPedido.CANCELADO)) {
            Long id = pedido(final_);
            for (String rota : List.of("preparacao", "envio", "entrega")) {
                mvc.perform(post(URL + "/" + id + "/" + rota).header("Authorization", admin))
                        .andExpect(status().isConflict())
                        .andExpect(jsonPath("$.situacaoAtual").value(final_.name()));
            }
            mvc.perform(post(URL + "/" + id + "/cancelamento").header("Authorization", admin)
                            .contentType(MediaType.APPLICATION_JSON).content(motivo("de novo")))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.situacaoAtual").value(final_.name()))
                    .andExpect(jsonPath("$.operacao").value("CANCELAR"));
            mvc.perform(get(URL + "/" + id).header("Authorization", admin))
                    .andExpect(jsonPath("$.situacao").value(final_.name()));
        }
    }

    // --- Cancelamento administrativo ---

    @Test
    void administradorCancelaPedidoPagoERegistraResponsavelEstadoAnteriorEMotivo() throws Exception {
        Long id = pedido(SituacaoPedido.PAGO);

        mvc.perform(post(URL + "/" + id + "/cancelamento").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(motivo("fraude suspeita")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("CANCELADO"))
                .andExpect(jsonPath("$.historico[3].de").value("PAGO"))
                .andExpect(jsonPath("$.historico[3].para").value("CANCELADO"))
                .andExpect(jsonPath("$.historico[3].motivo").value("fraude suspeita"))
                .andExpect(jsonPath("$.historico[3].responsavel.tipo").value("ADMINISTRATIVO"))
                .andExpect(jsonPath("$.historico[3].responsavel.usuarioId").value(adminUsuario.getId()));
    }

    @Test
    void operadorComercialCancelaPedidoEmPreparacaoEMotivoEObrigatorio() throws Exception {
        Long id = pedido(SituacaoPedido.EM_PREPARACAO);

        mvc.perform(post(URL + "/" + id + "/cancelamento").header("Authorization", comercial)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post(URL + "/" + id + "/cancelamento").header("Authorization", comercial)
                        .contentType(MediaType.APPLICATION_JSON).content(motivo("sem estoque")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("CANCELADO"))
                .andExpect(jsonPath("$.historico[*].de", hasItem("EM_PREPARACAO")));
    }
}
