package br.com.fatec.ecommerce.pagamento;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import br.com.fatec.ecommerce.TesteDeIntegracao;
import br.com.fatec.ecommerce.catalogo.CatalogoPersistidoParaTeste;
import br.com.fatec.ecommerce.cliente.ClientesParaTeste;
import br.com.fatec.ecommerce.cliente.ClientesParaTeste.ClienteDeTeste;
import br.com.fatec.ecommerce.cliente.dominio.*;
import br.com.fatec.ecommerce.pagamento.aplicacao.*;
import br.com.fatec.ecommerce.pagamento.dominio.*;
import br.com.fatec.ecommerce.pagamento.infra.gateway.CenariosPagamento;
import br.com.fatec.ecommerce.estoque.aplicacao.MovimentarReservas;
import br.com.fatec.ecommerce.pedido.PedidosPersistidosParaTeste;
import br.com.fatec.ecommerce.pedido.dominio.*;
import br.com.fatec.ecommerce.pedido.dominio.evento.*;
import br.com.fatec.ecommerce.seguranca.UsuariosParaTeste;
import br.com.fatec.ecommerce.seguranca.dominio.Perfil;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.*;
import tools.jackson.databind.*;

@TesteDeIntegracao
@Import({CatalogoPersistidoParaTeste.class,PedidosPersistidosParaTeste.class,PagamentoIntegracaoTests.Observacao.class})
class PagamentoIntegracaoTests {
    static final String CONF="/api/admin/pagamentos/confirmacoes";
    @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired UsuariosParaTeste usuarios;
    @Autowired ClientesParaTeste clientes; @Autowired CatalogoPersistidoParaTeste catalogo;
    @Autowired EnderecoRepository enderecos; @Autowired JdbcTemplate jdbc;
    @Autowired PedidoRepository pedidos; @Autowired PagamentosRepository pagamentos;
    @Autowired PedidosPersistidosParaTeste legados; @Autowired Eventos eventos;
    @Autowired OuvinteDePedidosDoPagamento ouvinte;
    @MockitoSpyBean CenariosPagamento cenarios;
    @MockitoSpyBean MovimentarReservas movimentos;
    @MockitoSpyBean br.com.fatec.ecommerce.notificacao.infra.integracao.CanalNotificacaoSimulado canalNotificacao;
    @MockitoSpyBean br.com.fatec.ecommerce.promocao.aplicacao.ConfirmarUsoCupom usos;
    ClienteDeTeste cliente,outro; String admin; long endereco;
    @TestConfiguration static class Observacao { @Bean Eventos eventosPagamento(){return new Eventos();} }
    static class Eventos {
        final List<Long> pagos=new CopyOnWriteArrayList<>(), cancelados=new CopyOnWriteArrayList<>();
        volatile boolean falhar;
        @EventListener @Order(999) public void pago(PedidoPago e){ if(falhar)throw new IllegalStateException("Falha de observador");pagos.add(e.pedidoId()); }
        @EventListener @Order(999) public void cancelado(PedidoCancelado e){cancelados.add(e.pedidoId());}
    }
    @BeforeEach void preparar(){
        usuarios.limpar(); eventos.pagos.clear();eventos.cancelados.clear();eventos.falhar=false;
        cliente=clientes.criar("pagar@teste.local",1301);outro=clientes.criar("outropagar@teste.local",1302);
        admin=usuarios.bearer(usuarios.criar("adminpagar@teste.local",Perfil.ADMIN));
        endereco=enderecos.salvar(new Endereco(cliente.cliente().getId(),"Casa","01001000","Rua","1",null,"Centro","SP","SP")).getId();
        var cat=catalogo.categoria("Pagamento",null);catalogo.variacao(cat,"PAG-ITEM","10");
        jdbc.update("UPDATE saldo_estoque SET disponivel=10");
    }
    ResultActions postJson(String url,String token,Object corpo)throws Exception{
        var req=post(url).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corpo));
        if(token!=null)req.header("Authorization",token);
        return mvc.perform(req);
    }
    JsonNode node(ResultActions acao)throws Exception{return json.readTree(acao.andReturn().getResponse().getContentAsString());}
    String url(long id){return "/api/clientes/me/pedidos/"+id+"/pagamento";}
    long comprar()throws Exception{
        postJson("/api/clientes/me/carrinho/itens",cliente.bearer(),Map.of("sku","PAG-ITEM","quantidade",2)).andExpect(status().isOk());
        return concluirSelecao();
    }
    long concluirSelecao()throws Exception{
        var p=node(postJson("/api/clientes/me/checkout/previa",cliente.bearer(),Map.of("opcaoEntrega","economica-simulada:ECONOMICA","enderecoId",endereco)).andExpect(status().isOk()));
        return node(postJson("/api/clientes/me/checkout",cliente.bearer(),Map.of("opcaoEntrega","economica-simulada:ECONOMICA","enderecoId",endereco,"confirmacao",p.get("confirmacao"))).andExpect(status().isCreated())).get("id").asLong();
    }
    JsonNode iniciar(long id,String forma)throws Exception{return node(postJson(url(id),cliente.bearer(),Map.of("forma",forma)).andExpect(status().isCreated()));}
    ResultActions confirmar(String tx,String situacao)throws Exception{return postJson(CONF,admin,Map.of("idTransacaoExterna",tx,"situacao",situacao));}
    String pix(long id)throws Exception{return iniciar(id,"PIX").get("idTransacaoExterna").asText();}
    ResultActions cancelar(long id)throws Exception{return postJson("/api/admin/pedidos/"+id+"/cancelamento",admin,Map.of("motivo","Teste"));}
    int saldo(String coluna){return jdbc.queryForObject("SELECT "+coluna+" FROM saldo_estoque",Integer.class);}
    int contar(String tabela){return jdbc.queryForObject("SELECT count(*) FROM "+tabela,Integer.class);}
    void notificou(long id,String acontecimento,int vezes) {
        verify(canalNotificacao,times(vezes)).enviar(argThat(n -> n.assunto().equals("Pedido "+id+": "+acontecimento)
                && n.destinatario().equals("pagar@teste.local")));
    }
    @Test void cartaoSincronoPreservaTotalEHistoricoComResponsavelSistema()throws Exception{
        long id=comprar();var r=iniciar(id,"CARTAO_CREDITO");
        assertThat(r.get("total").decimalValue()).isEqualByComparingTo("45");
        assertThat(r.get("situacao").asText()).isEqualTo("APROVADO");
        var p=pedidos.buscarPorId(id).orElseThrow();assertThat(p.getSituacao()).isEqualTo(SituacaoPedido.PAGO);
        assertThat(p.getHistorico().getLast().getResponsavel().tipo()).isEqualTo(TipoResponsavel.SISTEMA);
        assertThat(p.getHistorico().getLast().getMotivo()).contains(r.get("idTransacaoExterna").asText());
        assertThat(saldo("disponivel")).isEqualTo(8);assertThat(saldo("reservado")).isZero();
        postJson(url(id),cliente.bearer(),Map.of("forma","CARTAO_CREDITO")).andExpect(status().isOk()).andExpect(jsonPath("$.idTransacaoExterna").value(r.get("idTransacaoExterna").asText()));
        assertThat(contar("confirmacao_pagamento")).isEqualTo(1);assertThat(eventos.pagos).containsExactly(id);
        notificou(id,"AGUARDANDO_PAGAMENTO",1);notificou(id,"PAGO",1);
        assertThat(jdbc.queryForObject("select count(*) from auditoria_operacao where recurso_tipo='PEDIDO'",Integer.class)).isZero();
    }
    @Test void recusaSincronaNaoReabrePedidoEFormaNaoPodeMudar()throws Exception{
        doReturn(false).when(cenarios).cartaoAprovado();long id=comprar();iniciar(id,"CARTAO_CREDITO");
        assertThat(pedidos.buscarPorId(id).orElseThrow().getSituacao()).isEqualTo(SituacaoPedido.PAGAMENTO_RECUSADO);
        assertThat(saldo("disponivel")).isEqualTo(10);assertThat(saldo("reservado")).isZero();
        postJson(url(id),cliente.bearer(),Map.of("forma","PIX")).andExpect(status().isConflict());
        confirmar("CARD-"+id,"APROVADO").andExpect(status().isConflict());
        assertThat(contar("cobranca")).isEqualTo(1);
        notificou(id,"PAGAMENTO_RECUSADO",1);
    }
    @Test void boletoPendenteERecusaAssincrona()throws Exception{
        long id=comprar();var c=iniciar(id,"BOLETO");assertThat(c.get("situacao").asText()).isEqualTo("PENDENTE");
        assertThat(saldo("reservado")).isEqualTo(2);
        confirmar(c.get("idTransacaoExterna").asText(),"RECUSADO").andExpect(status().isOk());
        confirmar(c.get("idTransacaoExterna").asText(),"RECUSADO").andExpect(status().isOk());
        assertThat(saldo("disponivel")).isEqualTo(10);assertThat(contar("confirmacao_pagamento")).isEqualTo(1);
        notificou(id,"PAGAMENTO_RECUSADO",1);
    }
    @Test void replayAposCancelamentoContinuaSucessoEEstornoNaoRepete()throws Exception{
        long id=comprar();String tx=pix(id);confirmar(tx,"APROVADO").andExpect(status().isOk());
        cancelar(id).andExpect(status().isOk());confirmar(tx,"APROVADO").andExpect(status().isOk());
        confirmar(tx,"RECUSADO").andExpect(status().isConflict());
        var estorno=pagamentos.buscarEstorno(id).orElseThrow();assertThat(estorno.situacao()).isEqualTo(Estorno.Situacao.CONCLUIDO);
        postJson("/api/admin/pagamentos/"+id+"/estorno/tentativas",admin,Map.of()).andExpect(status().isOk()).andExpect(jsonPath("$.tentativas").value(1));
        ouvinte.aoCancelar(new PedidoCancelado(id,cliente.cliente().getId(),SituacaoPedido.PAGO,SituacaoPedido.CANCELADO,Instant.now(),Responsavel.sistema(),"replay"));
        assertThat(pagamentos.buscarEstorno(id)).contains(estorno);assertThat(eventos.pagos).hasSize(1);assertThat(eventos.cancelados).hasSize(1);
        assertThat(jdbc.queryForList("select operacao from auditoria_operacao where recurso_tipo='PEDIDO'",String.class)).containsExactly("PEDIDO_CANCELADO");
        assertThat(saldo("disponivel")).isEqualTo(10);assertThat(saldo("reservado")).isZero();
        mvc.perform(get(url(id)).header("Authorization",cliente.bearer())).andExpect(jsonPath("$.estorno.situacao").value("CONCLUIDO"));
    }
    @Test void falhaDeEstornoMantemCancelamentoEPermiteRecuperacao()throws Exception{
        long id=comprar();iniciar(id,"CARTAO_CREDITO");doReturn(true).when(cenarios).falharEstorno();
        cancelar(id).andExpect(status().isOk());assertThat(pagamentos.buscarEstorno(id).orElseThrow().situacao()).isEqualTo(Estorno.Situacao.FALHOU);
        assertThat(pedidos.buscarPorId(id).orElseThrow().getSituacao()).isEqualTo(SituacaoPedido.CANCELADO);
        assertThat(saldo("disponivel")).isEqualTo(10);
        doReturn(false).when(cenarios).falharEstorno();
        postJson("/api/admin/pagamentos/"+id+"/estorno/tentativas",cliente.bearer(),Map.of()).andExpect(status().isForbidden());
        var respostas=concorrer(()->postJson("/api/admin/pagamentos/"+id+"/estorno/tentativas",admin,Map.of()).andReturn().getResponse().getStatus(),
                ()->postJson("/api/admin/pagamentos/"+id+"/estorno/tentativas",admin,Map.of()).andReturn().getResponse().getStatus());
        assertThat(respostas).containsExactly(200,200);
        assertThat(pagamentos.buscarEstorno(id).orElseThrow().tentativas()).isEqualTo(2);
        assertThat(saldo("disponivel")).isEqualTo(10);
    }
    @Test void excecaoDoFornecedorNoEstornoViraPendenciaSemDesfazerCancelamento()throws Exception{
        long id=comprar();iniciar(id,"CARTAO_CREDITO");doThrow(new IllegalStateException("indisponivel")).when(cenarios).falharEstorno();
        cancelar(id).andExpect(status().isOk());assertThat(pagamentos.buscarEstorno(id).orElseThrow().ultimoErro()).contains("Falha no fornecedor");
        assertThat(saldo("disponivel")).isEqualTo(10);
    }
    @Test void confirmacaoTardiaRejeitadaSemRegistroECompraCanceladaSemEstorno()throws Exception{
        long id=comprar();String tx=pix(id);cancelar(id).andExpect(status().isOk());
        confirmar(tx,"APROVADO").andExpect(status().isConflict());assertThat(contar("confirmacao_pagamento")).isZero();
        assertThat(contar("estorno")).isZero();assertThat(saldo("disponivel")).isEqualTo(10);
        notificou(id,"CANCELADO",1);notificou(id,"PAGO",0);
    }
    @Test void falhaDeOuvinteDesfazConfirmacaoHistoricoEEstoqueParaReenvio()throws Exception{
        long id=comprar();String tx=pix(id);int historico=pedidos.buscarPorId(id).orElseThrow().getHistorico().size();
        eventos.falhar=true;
        confirmar(tx,"APROVADO").andExpect(status().isInternalServerError());
        assertThat(contar("confirmacao_pagamento")).isZero();assertThat(pedidos.buscarPorId(id).orElseThrow().getHistorico()).hasSize(historico);
        assertThat(pagamentos.buscarPorPedido(id).orElseThrow().situacao()).isEqualTo(ResultadoCobranca.Situacao.PENDENTE);
        assertThat(saldo("reservado")).isEqualTo(2);eventos.falhar=false;
        notificou(id,"PAGO",0);
        confirmar(tx,"APROVADO").andExpect(status().isOk());assertThat(saldo("reservado")).isZero();
        notificou(id,"PAGO",1);
    }
    @Test void falhaNoEstoqueTambemPermiteReenvio()throws Exception{
        long id=comprar();String tx=pix(id);doThrow(new IllegalStateException("Falha de estoque")).when(movimentos).baixar(id);
        confirmar(tx,"APROVADO").andExpect(status().isInternalServerError());
        assertThat(contar("confirmacao_pagamento")).isZero();assertThat(saldo("reservado")).isEqualTo(2);
        doCallRealMethod().when(movimentos).baixar(id);confirmar(tx,"APROVADO").andExpect(status().isOk());
    }
    @Test void duasConfirmacoesSimultaneasSaoSucessoComUmEvento()throws Exception{
        long id=comprar();String tx=pix(id);
        assertThat(concorrer(()->confirmar(tx,"APROVADO").andReturn().getResponse().getStatus(),()->confirmar(tx,"APROVADO").andReturn().getResponse().getStatus())).containsExactly(200,200);
        assertThat(eventos.pagos).containsExactly(id);assertThat(contar("confirmacao_pagamento")).isEqualTo(1);
        assertThat(saldo("disponivel")).isEqualTo(8);assertThat(saldo("reservado")).isZero();
        notificou(id,"PAGO",1);
        assertThat(jdbc.queryForObject("select count(*) from auditoria_operacao where recurso_tipo='PEDIDO'",Integer.class)).isZero();
    }
    @Test void doisIniciosSimultaneosCriamUmaCobranca()throws Exception{
        long id=comprar();
        var r=concorrer(()->postJson(url(id),cliente.bearer(),Map.of("forma","PIX")).andReturn().getResponse().getStatus(),
                ()->postJson(url(id),cliente.bearer(),Map.of("forma","PIX")).andReturn().getResponse().getStatus());
        assertThat(r).containsExactlyInAnyOrder(201,200);assertThat(contar("cobranca")).isEqualTo(1);
    }
    @Test void corridaPagamentoCancelamentoPreservaSequenciaValida()throws Exception{
        long id=comprar();String tx=pix(id);
        var r=concorrer(()->confirmar(tx,"APROVADO").andReturn().getResponse().getStatus(),()->cancelar(id).andReturn().getResponse().getStatus());
        assertThat(r.get(1)).isEqualTo(200);assertThat(r.getFirst()).isIn(200,409);
        assertThat(pedidos.buscarPorId(id).orElseThrow().getSituacao()).isEqualTo(SituacaoPedido.CANCELADO);
        assertThat(saldo("disponivel")).isEqualTo(10);assertThat(saldo("reservado")).isZero();
        assertThat(contar("estorno")).isEqualTo(r.getFirst()==200?1:0);
    }
    @Test void doisCancelamentosConcorrentesNaoRepetemEfeitos()throws Exception{
        long id=comprar();iniciar(id,"CARTAO_CREDITO");
        assertThat(concorrer(()->cancelar(id).andReturn().getResponse().getStatus(),()->cancelar(id).andReturn().getResponse().getStatus())).containsExactlyInAnyOrder(200,409);
        assertThat(pagamentos.buscarEstorno(id).orElseThrow().tentativas()).isEqualTo(1);assertThat(saldo("disponivel")).isEqualTo(10);
    }
    @Test void estornoEmPreparacaoELegadoPagoSemTransacao()throws Exception{
        long id=comprar();iniciar(id,"CARTAO_CREDITO");
        postJson("/api/admin/pedidos/"+id+"/preparacao",admin,Map.of()).andExpect(status().isOk());cancelar(id).andExpect(status().isOk());
        assertThat(pagamentos.buscarEstorno(id).orElseThrow().situacao()).isEqualTo(Estorno.Situacao.CONCLUIDO);
        long legado=legados.criar(cliente.cliente().getId(),SituacaoPedido.PAGO);cancelar(legado).andExpect(status().isOk());
        assertThat(pagamentos.buscarEstorno(legado).orElseThrow().ultimoErro()).contains("legado");
        postJson("/api/admin/pagamentos/"+legado+"/estorno/tentativas",admin,Map.of()).andExpect(status().isConflict());
        assertThat(pagamentos.buscarPorPedido(legado)).isEmpty();
    }
    @Test void permissoesValidacaoEPropriedadeNaoPermitemAlterarPedidoAlheio()throws Exception{
        long id=comprar();
        postJson(url(id),null,Map.of("forma","PIX")).andExpect(status().isUnauthorized());
        postJson(url(id),admin,Map.of("forma","PIX")).andExpect(status().isForbidden());
        postJson(url(id),outro.bearer(),Map.of("forma","PIX")).andExpect(status().isNotFound());
        mvc.perform(get(url(id)).header("Authorization",outro.bearer())).andExpect(status().isNotFound());
        mvc.perform(get(url(id)).header("Authorization",cliente.bearer())).andExpect(status().isNotFound());
        postJson(url(id),cliente.bearer(),Map.of()).andExpect(status().isBadRequest());
        postJson(url(id),cliente.bearer(),Map.of("forma","DINHEIRO")).andExpect(status().isBadRequest());
        postJson(CONF,cliente.bearer(),Map.of("idTransacaoExterna","TX","situacao","APROVADO")).andExpect(status().isForbidden());
        postJson(CONF,null,Map.of("idTransacaoExterna","TX","situacao","APROVADO")).andExpect(status().isUnauthorized());
        postJson(CONF,admin,Map.of()).andExpect(status().isBadRequest());
        confirmar("desconhecida","APROVADO").andExpect(status().isNotFound());
        String tx=pix(id);confirmar(tx,"PENDENTE").andExpect(status().isBadRequest());
        for(Perfil perfil:List.of(Perfil.OPERADOR_COMERCIAL,Perfil.ESTOQUE,Perfil.PROMOCOES)) {
            String token=usuarios.bearer(usuarios.criar(perfil+"@pag.local",perfil));
            postJson(CONF,token,Map.of("idTransacaoExterna",tx,"situacao","APROVADO")).andExpect(status().isForbidden());
        }
        assertThat(contar("confirmacao_pagamento")).isZero();
    }
    @Test void atalhoRemovidoEInexistenteNoOpenApi()throws Exception{
        long id=comprar();
        postJson("/api/admin/pedidos/"+id+"/pagamento-simulado",admin,Map.of("aprovado",true)).andExpect(status().isNotFound());
        var api=mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(api).doesNotContain("pagamento-simulado").contains("/api/admin/pagamentos/confirmacoes");
    }
    @Test void pedidoRecusadoOuCanceladoNaoAceitaPrimeiraCobranca()throws Exception{
        long id=legados.criar(cliente.cliente().getId(),SituacaoPedido.PAGAMENTO_RECUSADO);
        postJson(url(id),cliente.bearer(),Map.of("forma","PIX")).andExpect(status().isConflict());
        long cancelado=legados.criar(cliente.cliente().getId(),SituacaoPedido.CANCELADO);
        postJson(url(cancelado),cliente.bearer(),Map.of("forma","PIX")).andExpect(status().isConflict());
        assertThat(contar("cobranca")).isZero();
    }
    @Test void falhaNaDevolucaoDoCupomDesfazRecusaEPermiteReenvio()throws Exception{
        var cupom=Map.of("codigo","PAG10","nome","Desconto","tipo","PERCENTUAL","inicio",Instant.now().minusSeconds(60).toString(),
                "fim",Instant.now().plusSeconds(3600).toString(),"acumulavel",true,"limiteTotal",10,"parametros",Map.of("percentual",10));
        postJson("/api/admin/cupons",admin,cupom).andExpect(status().isCreated());
        postJson("/api/clientes/me/carrinho/itens",cliente.bearer(),Map.of("sku","PAG-ITEM","quantidade",2)).andExpect(status().isOk());
        mvc.perform(put("/api/clientes/me/carrinho/cupom").header("Authorization",cliente.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{\"codigo\":\"PAG10\"}")).andExpect(status().isOk());
        long id=concluirSelecao();String tx=pix(id);
        doAnswer(inv->{inv.callRealMethod();throw new IllegalStateException("Falha após devolução do cupom");}).when(usos).devolver(id);
        confirmar(tx,"RECUSADO").andExpect(status().isInternalServerError());
        assertThat(contar("confirmacao_pagamento")).isZero();
        assertThat(jdbc.queryForObject("SELECT usos_ativos FROM saldo_cupom",Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT situacao FROM uso_cupom",String.class)).isEqualTo("CONSUMIDO");
        assertThat(saldo("reservado")).isEqualTo(2);assertThat(saldo("disponivel")).isEqualTo(8);
        notificou(id,"PAGAMENTO_RECUSADO",0);
        doCallRealMethod().when(usos).devolver(id);confirmar(tx,"RECUSADO").andExpect(status().isOk());
        confirmar(tx,"RECUSADO").andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT usos_ativos FROM saldo_cupom",Integer.class)).isZero();
        assertThat(saldo("disponivel")).isEqualTo(10);
        notificou(id,"PAGAMENTO_RECUSADO",1);
    }
    @Test void falhaDeCanalNaoDesfazPagamentoNemMudaRespostaOuReplay()throws Exception {
        long id=comprar();String tx=pix(id);
        doThrow(new IllegalStateException("Falha simulada do canal")).when(canalNotificacao).enviar(any());
        confirmar(tx,"APROVADO").andExpect(status().isOk());
        confirmar(tx,"APROVADO").andExpect(status().isOk());
        assertThat(pedidos.buscarPorId(id).orElseThrow().getSituacao()).isEqualTo(SituacaoPedido.PAGO);
        assertThat(contar("confirmacao_pagamento")).isEqualTo(1);
        assertThat(saldo("reservado")).isZero();assertThat(saldo("disponivel")).isEqualTo(8);
        notificou(id,"PAGO",1);
    }
    @Test void cupomDevolvidoUmaVezNoCancelamentoNotificado()throws Exception {
        postJson("/api/admin/cupons",admin,Map.of("codigo","CANCEL10","nome","Desconto","tipo","PERCENTUAL",
                "inicio",Instant.now().minusSeconds(60).toString(),"fim",Instant.now().plusSeconds(3600).toString(),
                "acumulavel",true,"limiteTotal",10,"parametros",Map.of("percentual",10))).andExpect(status().isCreated());
        postJson("/api/clientes/me/carrinho/itens",cliente.bearer(),Map.of("sku","PAG-ITEM","quantidade",2)).andExpect(status().isOk());
        mvc.perform(put("/api/clientes/me/carrinho/cupom").header("Authorization",cliente.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{\"codigo\":\"CANCEL10\"}")).andExpect(status().isOk());
        long id=concluirSelecao();String tx=pix(id);
        confirmar(tx,"APROVADO").andExpect(status().isOk());
        cancelar(id).andExpect(status().isOk());cancelar(id).andExpect(status().isConflict());
        confirmar(tx,"APROVADO").andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT usos_ativos FROM saldo_cupom",Integer.class)).isZero();
        assertThat(saldo("disponivel")).isEqualTo(10);assertThat(saldo("reservado")).isZero();
        assertThat(pagamentos.buscarEstorno(id).orElseThrow().tentativas()).isEqualTo(1);
        notificou(id,"PAGO",1);notificou(id,"CANCELADO",1);
    }
    @Test void roteiroDeCompraNotificadaEConsultaDaTrilha()throws Exception {
        long variacao=jdbc.queryForObject("SELECT id FROM item_vendavel WHERE sku='PAG-ITEM'",Long.class);
        mvc.perform(put("/api/admin/produtos/variacoes/"+variacao).header("Authorization",admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"preco\":12.50}"))
                .andExpect(status().isOk());
        var campanha=node(postJson("/api/admin/promocoes",admin,Map.of("nome","Campanha auditoria","tipo","VALOR_FIXO_ACIMA_DE",
                "inicio",Instant.now().minusSeconds(60).toString(),"fim",Instant.now().plusSeconds(3600).toString(),
                "prioridade",1,"modo","ACUMULAVEL","parametros",Map.of("minimo",100,"valor",10)))
                .andExpect(status().isCreated()));
        long id=comprar();String tx=pix(id);confirmar(tx,"APROVADO").andExpect(status().isOk());
        notificou(id,"AGUARDANDO_PAGAMENTO",1);notificou(id,"PAGO",1);
        assertThat(pedidos.buscarPorId(id).orElseThrow().getTotal().valor()).isEqualByComparingTo("50");
        cancelar(id).andExpect(status().isOk());notificou(id,"CANCELADO",1);
        assertThat(saldo("disponivel")).isEqualTo(10);
        assertThat(pagamentos.buscarEstorno(id).orElseThrow().total().valor()).isEqualByComparingTo("50");
        var preco=node(mvc.perform(get("/api/admin/auditoria").header("Authorization",admin)
                .param("operacao","VARIACAO_PRECO_ALTERADO").param("size","1")).andExpect(status().isOk()));
        assertThat(preco.get("totalElementos").asInt()).isEqualTo(1);
        var registro=preco.get("conteudo").get(0);
        assertThat(registro.get("recursoId").asLong()).isEqualTo(variacao);
        assertThat(registro.at("/detalhes/antes/variacoes/0/preco").decimalValue()).isEqualByComparingTo("10");
        assertThat(registro.at("/detalhes/depois/variacoes/0/preco").decimalValue()).isEqualByComparingTo("12.50");
        // A mesma consulta do roteiro combina ator, operação e limites inclusivos.
        String usuarioId=Long.toString(registro.get("usuarioId").asLong());
        String momento=registro.get("ocorridoEm").asString();
        mvc.perform(get("/api/admin/auditoria").header("Authorization",admin).param("usuarioId",usuarioId)
                .param("operacao","VARIACAO_PRECO_ALTERADO").param("inicio",momento).param("fim",momento))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(1));
        mvc.perform(get("/api/admin/auditoria").header("Authorization",admin).param("operacao","CAMPANHA_CRIADA"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.conteudo[0].recursoId").value(campanha.get("id").asLong()));
        mvc.perform(get("/api/admin/auditoria").header("Authorization",admin).param("operacao","PEDIDO_CANCELADO"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.conteudo[0].recursoId").value(id))
                .andExpect(jsonPath("$.conteudo[0].detalhes.antes.estado").value("PAGO"))
                .andExpect(jsonPath("$.conteudo[0].detalhes.depois.motivo").value("Teste"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM auditoria_operacao",Integer.class)).isEqualTo(3);
    }
    List<Integer> concorrer(Callable<Integer> a,Callable<Integer> b)throws Exception{
        try(var pool=Executors.newFixedThreadPool(2)) {
            var partida=new CountDownLatch(1);
            var f1=pool.submit(()->{partida.await();return a.call();});var f2=pool.submit(()->{partida.await();return b.call();});partida.countDown();
            return List.of(f1.get(20,TimeUnit.SECONDS),f2.get(20,TimeUnit.SECONDS));
        }
    }
}
