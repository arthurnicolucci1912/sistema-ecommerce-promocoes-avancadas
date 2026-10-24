package br.com.fatec.ecommerce.checkout.aplicacao;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.fatec.ecommerce.TesteDeIntegracao;
import br.com.fatec.ecommerce.carrinho.dominio.Carrinho;
import br.com.fatec.ecommerce.carrinho.dominio.CarrinhoRepository;
import br.com.fatec.ecommerce.catalogo.CatalogoPersistidoParaTeste;
import br.com.fatec.ecommerce.catalogo.dominio.VariacaoProduto;
import br.com.fatec.ecommerce.cliente.ClientesParaTeste;
import br.com.fatec.ecommerce.cliente.ClientesParaTeste.ClienteDeTeste;
import br.com.fatec.ecommerce.cliente.dominio.Endereco;
import br.com.fatec.ecommerce.cliente.dominio.EnderecoRepository;
import br.com.fatec.ecommerce.compartilhado.dominio.ConflitoException;
import br.com.fatec.ecommerce.estoque.aplicacao.MovimentarReservas;
import br.com.fatec.ecommerce.pedido.aplicacao.ConfirmarReservaDoPedido;
import br.com.fatec.ecommerce.pedido.dominio.evento.PedidoCriado;
import br.com.fatec.ecommerce.seguranca.UsuariosParaTeste;
import br.com.fatec.ecommerce.seguranca.dominio.Perfil;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** PostgreSQL real: fluxos HTTP, rollback apos flush e disputas de transacoes independentes. */
@TesteDeIntegracao
@Import({CatalogoPersistidoParaTeste.class, CheckoutIntegracaoTests.Observacao.class})
class CheckoutIntegracaoTests {
    static final String URL = "/api/clientes/me/checkout";
    static final String CARRINHO = "/api/clientes/me/carrinho";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UsuariosParaTeste usuarios;
    @Autowired ClientesParaTeste clientes;
    @Autowired CatalogoPersistidoParaTeste catalogo;
    @Autowired EnderecoRepository enderecos;
    @Autowired JdbcTemplate jdbc;
    @Autowired CheckoutFacade fachada;
    @Autowired MovimentarReservas movimentos;
    @Autowired NovosPedidos observador;
    @MockitoSpyBean RevalidarCheckout revalidar;
    @MockitoSpyBean ConfirmarReservaDoPedido confirmar;
    @MockitoSpyBean CarrinhoRepository carrinhos;
    ClienteDeTeste cliente;
    ClienteDeTeste outro;
    Endereco endereco;
    Endereco enderecoOutro;
    VariacaoProduto cafe;
    VariacaoProduto caneca;
    String admin;

    @TestConfiguration
    static class Observacao {
        @Bean NovosPedidos novosPedidos() { return new NovosPedidos(); }
    }
    static class NovosPedidos {
        final List<Long> pedidos = new CopyOnWriteArrayList<>();
        @EventListener public void ao(PedidoCriado evento) { pedidos.add(evento.pedidoId()); }
    }

    @BeforeEach
    void preparar() {
        usuarios.limpar();
        observador.pedidos.clear();
        cliente = clientes.criar("checkout@teste.local", 801);
        outro = clientes.criar("outrocheckout@teste.local", 802);
        endereco = endereco(cliente);
        enderecoOutro = endereco(outro);
        admin = usuarios.bearer(usuarios.criar("admincheckout@teste.local", Perfil.ADMIN));
        var categoria = catalogo.categoria("Casa", null);
        cafe = catalogo.variacao(categoria, "CAFE", "10.00");
        caneca = catalogo.variacao(categoria, "CANECA", "20.00");
        var kit = catalogo.combo("KIT", "35.00", cafe, 2, caneca, 1);
        catalogo.combo("KIT-DUPLO", "65.00", kit, 2);
        jdbc.update("UPDATE saldo_estoque SET disponivel = 10");
    }

    Endereco endereco(ClienteDeTeste dono) {
        return enderecos.salvar(new Endereco(dono.cliente().getId(), "Casa", "01001000", "Rua de teste", "10",
                null, "Centro", "São Paulo", "SP"));
    }

    void confirmarPagamento(Long id, boolean aprovado) throws Exception {
        var resposta = mvc.perform(post("/api/clientes/me/pedidos/" + id + "/pagamento")
                .header("Authorization", cliente.bearer()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"forma\":\"PIX\"}")).andExpect(status().isCreated()).andReturn();
        String tx = json.readTree(resposta.getResponse().getContentAsString()).get("idTransacaoExterna").asText();
        mvc.perform(post("/api/admin/pagamentos/confirmacoes").header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                        "idTransacaoExterna", tx, "situacao", aprovado ? "APROVADO" : "RECUSADO"))))
                .andExpect(status().isOk());
    }

    void incluir(ClienteDeTeste dono, String sku, int quantidade) throws Exception {
        mvc.perform(post(CARRINHO + "/itens").header("Authorization", dono.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("sku", sku, "quantidade", quantidade))))
                .andExpect(status().isOk());
    }

    JsonNode previa(ClienteDeTeste dono, Endereco destino) throws Exception {
        return json.readTree(mvc.perform(post(URL + "/previa").header("Authorization", dono.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{\"opcaoEntrega\":\"economica-simulada:ECONOMICA\",\"enderecoId\":" + destino.getId() + "}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    ResultActions concluir(ClienteDeTeste dono, Endereco destino, JsonNode previa) throws Exception {
        return mvc.perform(post(URL).header("Authorization", dono.bearer()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("opcaoEntrega","economica-simulada:ECONOMICA","enderecoId", destino.getId(), "confirmacao", previa.get("confirmacao")))));
    }

    int contar(String tabela) { return jdbc.queryForObject("SELECT count(*) FROM " + tabela, Integer.class); }
    int saldo(String campo, VariacaoProduto item) {
        return jdbc.queryForObject("SELECT " + campo + " FROM saldo_estoque WHERE variacao_id = ?", Integer.class, item.getId());
    }
    void semPedidoNemReserva() {
        for (String tabela : List.of("pedido", "pedido_item", "pedido_historico", "reserva_estoque")) {
            assertThat(contar(tabela)).as(tabela).isZero();
        }
        assertThat(saldo("reservado", cafe)).isZero();
        assertThat(saldo("reservado", caneca)).isZero();
    }

    @Test
    void fluxoKitPrecoAlteradoReconfirmacaoSnapshotECancelamento() throws Exception {
        incluir(cliente, "KIT", 1);
        incluir(cliente, "CAFE", 1);
        var antes = previa(cliente, endereco);
        assertThat(antes.at("/confirmacao/total").decimalValue()).isEqualByComparingTo("70");
        semPedidoNemReserva();
        jdbc.update("UPDATE item_vendavel SET preco = 12.50 WHERE id = ?", cafe.getId());
        concluir(cliente, endereco, antes).andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.divergencias").isNotEmpty())
                .andExpect(jsonPath("$.previaAtualizada.confirmacao.total").value(72.50));
        semPedidoNemReserva();
        var atual = previa(cliente, endereco);
        var resposta = concluir(cliente, endereco, atual).andExpect(status().isCreated())
                .andExpect(jsonPath("$.situacao").value("AGUARDANDO_PAGAMENTO"))
                .andExpect(jsonPath("$.clienteId").value(cliente.cliente().getId()))
                .andExpect(jsonPath("$.total").value(72.50)).andExpect(jsonPath("$.historico", hasSize(2)))
                .andExpect(header().exists("Location")).andReturn().getResponse();
        var pedido = json.readTree(resposta.getContentAsString());
        long id = pedido.get("id").asLong();
        assertThat(resposta.getHeader("Location")).isEqualTo("/api/clientes/me/pedidos/" + id);
        assertThat(observador.pedidos).containsExactly(id);
        assertThat(saldo("disponivel", cafe)).isEqualTo(7);
        assertThat(saldo("reservado", cafe)).isEqualTo(3);
        assertThat(saldo("reservado", caneca)).isEqualTo(1);
        assertThat(contar("item_carrinho")).isZero();
        assertThat(pedido.get("itens").toString()).contains("tamanho: CAFE");
        jdbc.update("UPDATE item_vendavel SET preco = 99 WHERE id = ?", cafe.getId());
        jdbc.update("UPDATE endereco SET numero = '999' WHERE id = ?", endereco.getId());
        mvc.perform(get(resposta.getHeader("Location")).header("Authorization", cliente.bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(72.50))
                .andExpect(jsonPath("$.enderecoEntrega.numero").value("10"));
        mvc.perform(post(resposta.getHeader("Location") + "/cancelamento").header("Authorization", cliente.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Desisti\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.situacao").value("CANCELADO"));
        movimentos.liberar(id);
        assertThat(saldo("disponivel", cafe)).isEqualTo(10);
        assertThat(saldo("reservado", cafe)).isZero();
        assertThat(saldo("disponivel", caneca)).isEqualTo(10);
        assertThat(saldo("reservado", caneca)).isZero();
    }

    @Test
    void kitAninhadoESaldoAusenteNaPreviaSemEscrita() throws Exception {
        incluir(cliente, "KIT-DUPLO", 2);
        incluir(cliente, "CAFE", 1);
        jdbc.update("DELETE FROM saldo_estoque WHERE variacao_id = ?", cafe.getId());
        var versao = jdbc.queryForObject("SELECT versao FROM carrinho", Long.class);
        var p = previa(cliente, endereco);
        assertThat(p.at("/confirmacao/total").decimalValue()).isEqualByComparingTo("165");
        assertThat(p.at("/insuficiencias/0/solicitada").asInt()).isEqualTo(9);
        assertThat(p.at("/insuficiencias/0/disponivel").asInt()).isZero();
        assertThat(jdbc.queryForObject("SELECT versao FROM carrinho", Long.class)).isEqualTo(versao);
        assertThat(contar("item_carrinho")).isEqualTo(2);
        semPedidoNemReservaSemSaldoCafe();
        concluir(cliente, endereco, p).andExpect(status().isConflict())
                .andExpect(jsonPath("$.variacaoId").value(cafe.getId()))
                .andExpect(jsonPath("$.disponivel").value(0));
        semPedidoNemReservaSemSaldoCafe();
    }

    void semPedidoNemReservaSemSaldoCafe() {
        assertThat(contar("pedido")).isZero();
        assertThat(contar("reserva_estoque")).isZero();
        assertThat(saldo("reservado", caneca)).isZero();
    }

    @Test
    void insuficienciaPosteriorDesfazReservaAnteriorEPedidoInteiro() throws Exception {
        incluir(cliente, "CAFE", 1);
        incluir(cliente, "CANECA", 4);
        jdbc.update("UPDATE saldo_estoque SET disponivel = 3 WHERE variacao_id = ?", caneca.getId());
        var p = previa(cliente, endereco);
        concluir(cliente, endereco, p).andExpect(status().isConflict())
                .andExpect(jsonPath("$.variacaoId").value(caneca.getId()));
        semPedidoNemReserva();
        assertThat(saldo("disponivel", cafe)).isEqualTo(10);
        assertThat(saldo("disponivel", caneca)).isEqualTo(3);
        assertThat(contar("item_carrinho")).isEqualTo(2);
    }

    @Test
    void falhaDepoisDaConfirmacaoPersistidaDesfazTudo() throws Exception {
        incluir(cliente, "CAFE", 2);
        var p = previa(cliente, endereco);
        doAnswer(invocacao -> {
            invocacao.callRealMethod();
            throw new ConflitoException("Falha de teste após confirmar");
        }).when(confirmar).executar(anyLong());
        concluir(cliente, endereco, p).andExpect(status().isConflict());
        semPedidoNemReserva();
        assertThat(saldo("disponivel", cafe)).isEqualTo(10);
        assertThat(contar("item_carrinho")).isEqualTo(1);
    }

    @Test
    void falhaDepoisDoFlushDaLimpezaDesfazPedidoReservaESelecao() throws Exception {
        incluir(cliente, "CAFE", 2);
        var p = previa(cliente, endereco);
        doAnswer(invocacao -> {
            var salvo = invocacao.callRealMethod();
            if (((Carrinho) invocacao.getArgument(0)).getItens().isEmpty()) {
                throw new ConflitoException("Falha de teste após limpar");
            }
            return salvo;
        }).when(carrinhos).salvar(any());
        concluir(cliente, endereco, p).andExpect(status().isConflict());
        semPedidoNemReserva();
        assertThat(saldo("disponivel", cafe)).isEqualTo(10);
        assertThat(contar("item_carrinho")).isEqualTo(1);
        assertThat(previa(cliente, endereco).get("confirmacao")).isEqualTo(p.get("confirmacao"));
    }

    @Test
    void precoManipuladoQuantidadeEEnderecoAlteradosNaoConcluem() throws Exception {
        incluir(cliente, "CAFE", 2);
        var p = previa(cliente, endereco);
        var adulterada = json.readTree(p.toString().replace("12.50", "1.00").replace("20", "1"));
        concluir(cliente, endereco, adulterada).andExpect(status().isConflict());
        jdbc.update("UPDATE endereco SET numero = '11' WHERE id = ?", endereco.getId());
        concluir(cliente, endereco, p).andExpect(status().isConflict())
                .andExpect(jsonPath("$.previaAtualizada.confirmacao.endereco.numero").value("11"));
        incluir(cliente, "CAFE", 1);
        concluir(cliente, endereco, p).andExpect(status().isConflict())
                .andExpect(jsonPath("$.previaAtualizada.confirmacao.itens[0].quantidade").value(3));
        semPedidoNemReserva();
    }

    @Test
    void inativoIncluindoComponenteDeKitBloqueiaCompraComSkus() throws Exception {
        incluir(cliente, "KIT", 1);
        var p = previa(cliente, endereco);
        jdbc.update("UPDATE item_vendavel SET ativo = false WHERE id = ?", cafe.getId());
        concluir(cliente, endereco, p).andExpect(status().isConflict())
                .andExpect(jsonPath("$.skus[0]").value("KIT"));
        semPedidoNemReserva();
    }

    @Test
    void autenticaPerfisEIsolaEnderecoECarrinho() throws Exception {
        mvc.perform(post(URL + "/previa").contentType(MediaType.APPLICATION_JSON).content("{\"opcaoEntrega\":\"economica-simulada:ECONOMICA\",\"enderecoId\":1}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        for (Perfil perfil : List.of(Perfil.ADMIN, Perfil.ESTOQUE, Perfil.OPERADOR_COMERCIAL, Perfil.PROMOCOES)) {
            String token = usuarios.bearer(usuarios.criar(perfil + "@checkout.local", perfil));
            for (String rota : List.of(URL, URL + "/previa")) {
                mvc.perform(post(rota).header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content("{}"))
                        .andExpect(status().isForbidden());
            }
        }
        incluir(cliente, "CAFE", 1);
        var p = previa(cliente, endereco);
        concluir(outro, endereco, p).andExpect(status().isNotFound());
        mvc.perform(post(URL + "/previa").header("Authorization", cliente.bearer()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"opcaoEntrega\":\"economica-simulada:ECONOMICA\",\"enderecoId\":" + enderecoOutro.getId() + "}"))
                .andExpect(status().isNotFound());
        mvc.perform(post(URL + "/previa").header("Authorization", outro.bearer()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"opcaoEntrega\":\"economica-simulada:ECONOMICA\",\"enderecoId\":" + enderecoOutro.getId() + ",\"clienteId\":" + cliente.cliente().getId() + "}"))
                .andExpect(status().isConflict()); // usa o carrinho vazio do outro, nao o indicado no corpo
        endereco.inativar();
        enderecos.salvar(endereco);
        concluir(cliente, endereco, p).andExpect(status().isNotFound());
        semPedidoNemReserva();
        assertThat(contar("item_carrinho")).isEqualTo(1);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void fachadaProtegeAplicacaoTambem() {
        assertThatThrownBy(() -> fachada.previa(1L)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> fachada.concluir(1L, null)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void corposInvalidosECarrinhoVazioSaoErrosPadronizados() throws Exception {
        for (String corpo : List.of("{}", "{\"opcaoEntrega\":\"economica-simulada:ECONOMICA\",\"enderecoId\":null}", "{\"opcaoEntrega\":\"economica-simulada:ECONOMICA\",\"enderecoId\":0}", "{\"opcaoEntrega\":\"economica-simulada:ECONOMICA\",\"enderecoId\":1.5}")) {
            mvc.perform(post(URL + "/previa").header("Authorization", cliente.bearer())
                    .contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isBadRequest());
        }
        mvc.perform(post(URL + "/previa").header("Authorization", cliente.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{\"opcaoEntrega\":\"economica-simulada:ECONOMICA\",\"enderecoId\":" + endereco.getId() + "}"))
                .andExpect(status().isConflict());
        mvc.perform(post(URL).header("Authorization", cliente.bearer()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"opcaoEntrega\":\"economica-simulada:ECONOMICA\",\"enderecoId\":" + endereco.getId() + "}"))
                .andExpect(status().isBadRequest());
        incluir(cliente, "CAFE", 2);
        var p = previa(cliente, endereco);
        var texto = json.writeValueAsString(Map.of("opcaoEntrega","economica-simulada:ECONOMICA","enderecoId", endereco.getId(), "confirmacao", p.get("confirmacao")));
        for (String invalido : List.of(texto.replace("\"quantidade\":2", "\"quantidade\":1.5"),
                texto.replace("\"quantidade\":2", "\"quantidade\":0"))) {
            mvc.perform(post(URL).header("Authorization", cliente.bearer()).contentType(MediaType.APPLICATION_JSON)
                    .content(invalido)).andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
        }
        semPedidoNemReserva();
    }

    @Test
    void recusaEAprovacaoSimuladasReagemSobreReservaReal() throws Exception {
        incluir(cliente, "CAFE", 2);
        var recusado = json.readTree(concluir(cliente, endereco, previa(cliente, endereco))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();
        confirmarPagamento(recusado, false);
        mvc.perform(get("/api/clientes/me/pedidos/" + recusado).header("Authorization", cliente.bearer()))
                .andExpect(jsonPath("$.situacao").value("PAGAMENTO_RECUSADO"));
        movimentos.liberar(recusado);
        assertThat(saldo("disponivel", cafe)).isEqualTo(10);
        assertThat(saldo("reservado", cafe)).isZero();
        incluir(cliente, "CAFE", 3);
        var pago = json.readTree(concluir(cliente, endereco, previa(cliente, endereco))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();
        confirmarPagamento(pago, true);
        mvc.perform(get("/api/clientes/me/pedidos/" + pago).header("Authorization", cliente.bearer()))
                .andExpect(jsonPath("$.situacao").value("PAGO"));
        movimentos.baixar(pago);
        assertThat(saldo("disponivel", cafe)).isEqualTo(7);
        assertThat(saldo("reservado", cafe)).isZero();
    }

    @Test
    void documentaContratoDePreviaConfirmacaoPedidoEErros() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['" + URL + "/previa'].post").exists())
                .andExpect(jsonPath("$.paths['" + URL + "'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['" + URL + "'].post.responses['409']").exists())
                .andExpect(jsonPath("$.components.schemas.ConfirmacaoCheckout.properties.versaoCarrinho").exists())
                .andExpect(jsonPath("$.components.schemas.PreviaCheckout.properties.confirmacao").exists())
                .andExpect(jsonPath("$.components.schemas.PedidoResponse").exists());
    }

    void sincronizarRevalidacoes(CyclicBarrier barreira) {
        doAnswer(invocacao -> {
            var compra = invocacao.callRealMethod();
            barreira.await(15, TimeUnit.SECONDS);
            return compra;
        }).when(revalidar).executar(anyLong(), anyLong(), anyString());
    }

    @Test
    void doisClientesPelaUltimaUnidadeSoConfirmamUmPedido() throws Exception {
        incluir(cliente, "CAFE", 1);
        incluir(outro, "CAFE", 1);
        var p1 = previa(cliente, endereco);
        var p2 = previa(outro, enderecoOutro);
        jdbc.update("UPDATE saldo_estoque SET disponivel = 1 WHERE variacao_id = ?", cafe.getId());
        sincronizarRevalidacoes(new CyclicBarrier(2));
        try (var executor = Executors.newFixedThreadPool(2)) {
            var a = executor.submit(() -> concluir(cliente, endereco, p1).andReturn().getResponse().getStatus());
            var b = executor.submit(() -> concluir(outro, enderecoOutro, p2).andReturn().getResponse().getStatus());
            assertThat(List.of(a.get(30, TimeUnit.SECONDS), b.get(30, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201, 409);
        }
        assertThat(contar("pedido")).isEqualTo(1);
        assertThat(contar("pedido_historico")).isEqualTo(2);
        assertThat(contar("item_carrinho")).isEqualTo(1);
        assertThat(saldo("disponivel", cafe)).isZero();
        assertThat(saldo("reservado", cafe)).isEqualTo(1);
        assertThat(contar("reserva_estoque")).isEqualTo(1);
    }

    @Test
    void duasConclusoesDaMesmaVersaoNaoDuplicamCompraMesmoComEstoqueSuficiente() throws Exception {
        incluir(cliente, "CAFE", 2);
        var p = previa(cliente, endereco);
        sincronizarRevalidacoes(new CyclicBarrier(2));
        try (var executor = Executors.newFixedThreadPool(2)) {
            var a = executor.submit(() -> concluir(cliente, endereco, p).andReturn().getResponse().getStatus());
            var b = executor.submit(() -> concluir(cliente, endereco, p).andReturn().getResponse().getStatus());
            assertThat(List.of(a.get(30, TimeUnit.SECONDS), b.get(30, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201, 409);
        }
        assertThat(contar("pedido")).isEqualTo(1);
        assertThat(contar("pedido_historico")).isEqualTo(2);
        assertThat(contar("item_carrinho")).isZero();
        assertThat(saldo("disponivel", cafe)).isEqualTo(8);
        assertThat(saldo("reservado", cafe)).isEqualTo(2);
        assertThat(contar("reserva_estoque")).isEqualTo(1);
    }

    @Test
    void checkoutVencedorFazInclusaoDaVersaoAntigaResponder409SemPerderNovaCompra() throws Exception {
        incluir(cliente, "CAFE", 2);
        var p = previa(cliente, endereco);
        var lida = new CountDownLatch(1);
        var concluida = new CountDownLatch(1);
        doAnswer(invocacao -> {
            var resultado = invocacao.callRealMethod();
            if (Thread.currentThread().getName().equals("crud-checkout-teste")) {
                ((Optional<Carrinho>) resultado).orElseThrow().getItens().size();
                lida.countDown();
                assertThat(concluida.await(15, TimeUnit.SECONDS)).isTrue();
            }
            return resultado;
        }).when(carrinhos).buscarPorClienteId(anyLong());
        try (var executor = Executors.newSingleThreadExecutor()) {
            var tentativa = executor.submit(() -> {
                Thread.currentThread().setName("crud-checkout-teste");
                return mvc.perform(post(CARRINHO + "/itens").header("Authorization", cliente.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"sku\":\"CANECA\",\"quantidade\":1}"))
                        .andReturn().getResponse().getStatus();
            });
            assertThat(lida.await(15, TimeUnit.SECONDS)).isTrue();
            try { concluir(cliente, endereco, p).andExpect(status().isCreated()); }
            finally { concluida.countDown(); }
            assertThat(tentativa.get(30, TimeUnit.SECONDS)).isEqualTo(409);
        }
        assertThat(contar("pedido")).isEqualTo(1);
        assertThat(contar("item_carrinho")).isZero();
        assertThat(saldo("reservado", cafe)).isEqualTo(2);
        assertThat(saldo("reservado", caneca)).isZero();
    }

    @Test
    void inclusaoConcorrenteVencedoraNaoEhApagadaPeloCheckout() throws Exception {
        incluir(cliente, "CAFE", 2);
        var p = previa(cliente, endereco);
        var lida = new CountDownLatch(1);
        var alterada = new CountDownLatch(1);
        doAnswer(invocacao -> {
            var compra = invocacao.callRealMethod();
            lida.countDown();
            assertThat(alterada.await(15, TimeUnit.SECONDS)).isTrue();
            return compra;
        }).when(revalidar).executar(anyLong(), anyLong(), anyString());
        try (var executor = Executors.newFixedThreadPool(2)) {
            var tentativa = executor.submit(() -> concluir(cliente, endereco, p).andReturn().getResponse().getStatus());
            assertThat(lida.await(15, TimeUnit.SECONDS)).isTrue();
            try { incluir(cliente, "CANECA", 1); } finally { alterada.countDown(); }
            assertThat(tentativa.get(30, TimeUnit.SECONDS)).isEqualTo(409);
        }
        semPedidoNemReserva();
        assertThat(contar("item_carrinho")).isEqualTo(2);
        assertThat(saldo("disponivel", cafe)).isEqualTo(10);
    }
}
