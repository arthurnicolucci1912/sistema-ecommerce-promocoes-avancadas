package br.com.fatec.ecommerce.entrega;

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
import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import br.com.fatec.ecommerce.entrega.dominio.*;
import br.com.fatec.ecommerce.entrega.infra.*;
import br.com.fatec.ecommerce.seguranca.UsuariosParaTeste;
import br.com.fatec.ecommerce.seguranca.dominio.Perfil;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.*;
import tools.jackson.databind.*;

@TesteDeIntegracao
@Import({CatalogoPersistidoParaTeste.class,EntregaIntegracaoTests.Extensao.class})
class EntregaIntegracaoTests {
    static final String CHECK="/api/clientes/me/checkout", CAR="/api/clientes/me/carrinho";
    static final String ECO="economica-simulada:ECONOMICA", EXP="expressa-simulada:EXPRESSA", NOVA="teste:NOVA";
    @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired UsuariosParaTeste usuarios;
    @Autowired ClientesParaTeste clientes;@Autowired CatalogoPersistidoParaTeste catalogo;@Autowired EnderecoRepository enderecos;
    @Autowired JdbcTemplate jdbc;@Autowired NovoProvedor novo;
    @MockitoSpyBean AdaptadorEntregaEconomica economica;
    @MockitoSpyBean AdaptadorEntregaExpressa expressa;
    ClienteDeTeste cliente,outro;Endereco endereco,alheio;String admin;
    @TestConfiguration static class Extensao {@Bean NovoProvedor novoProvedor(){return new NovoProvedor();}}
    static class NovoProvedor implements ProvedorFrete {
        boolean ativo;int prazo=3;Dinheiro custo=Dinheiro.de("25");
        public String fornecedor(){return "teste";}public String modalidade(){return "NOVA";}
        public boolean atende(ContextoFrete c){return ativo;}
        public CotacaoFrete cotar(ContextoFrete c){return new CotacaoFrete(modalidade(),custo,prazo);}
    }
    @BeforeEach void preparar(){
        usuarios.limpar();novo.ativo=false;novo.prazo=3;novo.custo=Dinheiro.de("25");
        cliente=clientes.criar("entrega@teste.local",1701);outro=clientes.criar("outraentrega@teste.local",1702);
        endereco=endereco(cliente,"SP");alheio=endereco(outro,"BA");admin=usuarios.bearer(usuarios.criar("adminentrega@teste.local",Perfil.ADMIN));
        catalogo.variacao(catalogo.categoria("Entrega",null),"ITEM-ENTREGA","10");jdbc.update("UPDATE saldo_estoque SET disponivel=10");
    }
    Endereco endereco(ClienteDeTeste c,String uf){return enderecos.salvar(new Endereco(c.cliente().getId(),"Casa","01001000","Rua","1",null,"Centro","Cidade",uf));}
    ResultActions postJson(String url,String token,Object body)throws Exception{
        var r=post(url).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));if(token!=null)r.header("Authorization",token);return mvc.perform(r);
    }
    JsonNode node(ResultActions r)throws Exception{return json.readTree(r.andReturn().getResponse().getContentAsString());}
    void incluir(int q)throws Exception{postJson(CAR+"/itens",cliente.bearer(),Map.of("sku","ITEM-ENTREGA","quantidade",q)).andExpect(status().isOk());}
    ResultActions opcoes(Endereco e)throws Exception{return postJson(CHECK+"/fretes",cliente.bearer(),Map.of("enderecoId",e.getId()));}
    JsonNode previa(String opcao)throws Exception{return node(postJson(CHECK+"/previa",cliente.bearer(),Map.of("enderecoId",endereco.getId(),"opcaoEntrega",opcao)).andExpect(status().isOk()));}
    ResultActions concluir(String opcao,JsonNode p)throws Exception{return postJson(CHECK,cliente.bearer(),Map.of("enderecoId",endereco.getId(),"opcaoEntrega",opcao,"confirmacao",p.get("confirmacao")));}
    long comprar(String opcao)throws Exception{return node(concluir(opcao,previa(opcao)).andExpect(status().isCreated())).get("id").asLong();}
    void campanha(String tipo,Map<String,Object> parametros)throws Exception{
        postJson("/api/admin/promocoes",admin,Map.of("nome","Campanha "+tipo,"tipo",tipo,"inicio",Instant.now().minusSeconds(60).toString(),
                "fim",Instant.now().plusSeconds(3600).toString(),"prioridade",10,"modo","ACUMULAVEL","parametros",parametros)).andExpect(status().isCreated());
    }
    void cupomFrete()throws Exception{
        postJson("/api/admin/cupons",admin,Map.of("codigo","ENTREGA","nome","Cupom entrega","tipo","FRETE_GRATIS",
                "inicio",Instant.now().minusSeconds(60).toString(),"fim",Instant.now().plusSeconds(3600).toString(),"acumulavel",true,"limiteTotal",5,"parametros",Map.of())).andExpect(status().isCreated());
        mvc.perform(put(CAR+"/cupom").header("Authorization",cliente.bearer()).contentType(MediaType.APPLICATION_JSON).content("{\"codigo\":\"ENTREGA\"}")).andExpect(status().isOk());
    }
    int contar(String tabela){return jdbc.queryForObject("SELECT count(*) FROM "+tabela,Integer.class);}
    @Test void opcoesRegionaisSemEscritaEPrecoComposto()throws Exception{
        incluir(2);long versao=jdbc.queryForObject("SELECT versao FROM carrinho",Long.class);
        opcoes(endereco).andExpect(status().isOk()).andExpect(jsonPath("$.opcoes.length()").value(2))
                .andExpect(jsonPath("$.opcoes[0].custoBruto").value(25)).andExpect(jsonPath("$.opcoes[0].prazoDias").value(5))
                .andExpect(jsonPath("$.opcoes[1].custoBruto").value(40));
        opcoes(endereco(cliente,"BA")).andExpect(jsonPath("$.opcoes[0].custoBruto").value(40));
        var p=previa(ECO);assertThat(p.at("/confirmacao/total").decimalValue()).isEqualByComparingTo("45");
        assertThat(previa(EXP).at("/confirmacao/total").decimalValue()).isEqualByComparingTo("60");
        assertThat(jdbc.queryForObject("SELECT versao FROM carrinho",Long.class)).isEqualTo(versao);
        for(String tabela:List.of("pedido","reserva_estoque","uso_cupom"))assertThat(contar(tabela)).isZero();
    }
    @Test void novaTransportadoraESelecionadaSemMudarCheckout()throws Exception{
        incluir(2);novo.ativo=true;
        opcoes(endereco).andExpect(jsonPath("$.opcoes.length()").value(3));long id=comprar(NOVA);
        mvc.perform(get("/api/clientes/me/pedidos/"+id).header("Authorization",cliente.bearer()))
                .andExpect(jsonPath("$.opcaoEntrega.codigo").value(NOVA)).andExpect(jsonPath("$.total").value(45));
    }
    @Test void prazoOuModalidadeDeMesmoPrecoExigemConfirmacao()throws Exception{
        incluir(2);novo.ativo=true;var p=previa(NOVA);novo.prazo=7;
        concluir(NOVA,p).andExpect(status().isConflict()).andExpect(jsonPath("$.previaAtualizada.confirmacao.opcaoEntrega.prazoDias").value(7));
        var e=previa(ECO);concluir(NOVA,e).andExpect(status().isConflict()).andExpect(jsonPath("$.divergencias[0].campo").value("opcaoEntrega"));
        assertThat(contar("pedido")).isZero();
    }
    @Test void freteGratisNaoOcultaTarifaAlteradaETetoNaoDuplicaAbatimento()throws Exception{
        incluir(2);novo.ativo=true;campanha("FRETE_GRATIS",Map.of());cupomFrete();var p=previa(NOVA);
        assertThat(p.at("/confirmacao/frete").decimalValue()).isEqualByComparingTo("25");
        assertThat(p.at("/confirmacao/freteLiquido").decimalValue()).isZero();
        assertThat(p.at("/confirmacao/total").decimalValue()).isEqualByComparingTo("20");novo.custo=Dinheiro.de("30");
        concluir(NOVA,p).andExpect(status().isConflict()).andExpect(jsonPath("$.previaAtualizada.confirmacao.frete").value(30))
                .andExpect(jsonPath("$.previaAtualizada.confirmacao.total").value(20));
        comprar(NOVA);assertThat(contar("uso_cupom")).isZero(); // promocao ja cobre tudo
    }
    @Test void total261ComFreteGratisEPromocoesECupom()throws Exception{
        incluir(40);campanha("VALOR_FIXO_ACIMA_DE",Map.of("minimo",0,"valor",110));campanha("FRETE_GRATIS",Map.of());
        postJson("/api/admin/cupons",admin,Map.of("codigo","DEZ","nome","Dez","tipo","PERCENTUAL","inicio",Instant.now().minusSeconds(60).toString(),
                "fim",Instant.now().plusSeconds(3600).toString(),"acumulavel",true,"limiteTotal",10,"parametros",Map.of("percentual",10))).andExpect(status().isCreated());
        mvc.perform(put(CAR+"/cupom").header("Authorization",cliente.bearer()).contentType(MediaType.APPLICATION_JSON).content("{\"codigo\":\"DEZ\"}")).andExpect(status().isOk());
        var p=previa(ECO);assertThat(p.at("/confirmacao/total").decimalValue()).isEqualByComparingTo("261");
        assertThat(p.at("/confirmacao/freteLiquido").decimalValue()).isZero();
    }
    @Test void perdaDeCoberturaRetornaOpcoesSemTrocarAutomaticamente()throws Exception{
        incluir(2);var p=previa(ECO);doReturn(false).when(economica).atende(any());
        concluir(ECO,p).andExpect(status().isConflict()).andExpect(jsonPath("$.opcoesEntrega[0].codigo").value(EXP));
        doReturn(false).when(expressa).atende(any());opcoes(endereco).andExpect(status().isOk()).andExpect(jsonPath("$.opcoes").isEmpty());
        concluir(ECO,p).andExpect(status().isConflict());assertThat(contar("pedido")).isZero();assertThat(contar("reserva_estoque")).isZero();
    }
    @Test void falhasDeProvedorDiferemDeCoberturaENaoGeramZero()throws Exception{
        incluir(2);doThrow(new IllegalStateException("offline")).when(economica).cotar(any());
        opcoes(endereco).andExpect(status().isOk()).andExpect(jsonPath("$.opcoes.length()").value(1)).andExpect(jsonPath("$.opcoes[0].custoBruto").value(40));
        doThrow(new IllegalStateException("offline")).when(expressa).cotar(any());opcoes(endereco).andExpect(status().isServiceUnavailable());
        postJson(CHECK+"/previa",cliente.bearer(),Map.of("enderecoId",endereco.getId(),"opcaoEntrega",ECO)).andExpect(status().isServiceUnavailable());
        assertThat(contar("pedido")).isZero();
    }
    @Test void segurancaValidacaoESemSelecaoNaoHaFreteImplicito()throws Exception{
        postJson(CHECK+"/fretes",null,Map.of("enderecoId",endereco.getId())).andExpect(status().isUnauthorized());
        postJson(CHECK+"/fretes",admin,Map.of("enderecoId",endereco.getId())).andExpect(status().isForbidden());
        opcoes(endereco).andExpect(status().isConflict());incluir(1);opcoes(alheio).andExpect(status().isNotFound());
        for(Object body:List.of(Map.of(),Map.of("enderecoId",0),Map.of("enderecoId",1.5)))postJson(CHECK+"/fretes",cliente.bearer(),body).andExpect(status().isBadRequest());
        postJson(CHECK+"/previa",cliente.bearer(),Map.of("enderecoId",endereco.getId())).andExpect(status().isBadRequest());
        postJson(CHECK+"/previa",cliente.bearer(),Map.of("enderecoId",endereco.getId(),"opcaoEntrega","inexistente")).andExpect(status().isConflict());
        postJson(CHECK,cliente.bearer(),Map.of("enderecoId",endereco.getId(),"confirmacao",previa(ECO).get("confirmacao"))).andExpect(status().isBadRequest());
        assertThat(contar("pedido")).isZero();
    }
    @Test void snapshotPagoAteEntregaPermaneceMesmoDepoisDeMudarCotacao()throws Exception{
        incluir(2);novo.ativo=true;long id=comprar(NOVA);novo.custo=Dinheiro.de("99");novo.prazo=12;
        postJson("/api/clientes/me/pedidos/"+id+"/pagamento",cliente.bearer(),Map.of("forma","CARTAO_CREDITO")).andExpect(status().isCreated()).andExpect(jsonPath("$.total").value(45));
        postJson("/api/admin/pedidos/"+id+"/envio",admin,Map.of()).andExpect(status().isConflict());
        for(String passo:List.of("preparacao","envio","entrega"))postJson("/api/admin/pedidos/"+id+"/"+passo,admin,Map.of()).andExpect(status().isOk());
        mvc.perform(get("/api/clientes/me/pedidos/"+id).header("Authorization",cliente.bearer())).andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("ENTREGUE")).andExpect(jsonPath("$.opcaoEntrega.prazoDias").value(3))
                .andExpect(jsonPath("$.frete").value(25)).andExpect(jsonPath("$.total").value(45));
        mvc.perform(get("/api/clientes/me/pedidos/"+id).header("Authorization",outro.bearer())).andExpect(status().isNotFound());
    }
    @Test void cupomDeFreteConsumidoEDevolvidoUmaVezNoEstornoOriginal()throws Exception{
        incluir(2);cupomFrete();long id=comprar(ECO);
        assertThat(contar("uso_cupom")).isEqualTo(1);
        postJson("/api/clientes/me/pedidos/"+id+"/pagamento",cliente.bearer(),Map.of("forma","CARTAO_CREDITO")).andExpect(status().isCreated()).andExpect(jsonPath("$.total").value(20));
        doReturn(new CotacaoFrete("ECONOMICA",Dinheiro.de("99"),8)).when(economica).cotar(any());
        postJson("/api/admin/pedidos/"+id+"/cancelamento",admin,Map.of("motivo","Teste")).andExpect(status().isOk());
        postJson("/api/admin/pagamentos/"+id+"/estorno/tentativas",admin,Map.of()).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(20));
        assertThat(jdbc.queryForObject("SELECT usos_ativos FROM saldo_cupom",Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT disponivel FROM saldo_estoque",Integer.class)).isEqualTo(10);
        assertThat(jdbc.queryForObject("SELECT tentativas FROM estorno",Integer.class)).isEqualTo(1);
    }
    @Test void promocaoExclusivaBloqueiaFreteGratisEReducaoFicaNoAlvoCorreto()throws Exception{
        incluir(2);campanha("FRETE_GRATIS",Map.of());
        var exclusiva=Map.of("nome","Exclusiva","tipo","VALOR_FIXO_ACIMA_DE","inicio",Instant.now().minusSeconds(60).toString(),
                "fim",Instant.now().plusSeconds(3600).toString(),"prioridade",1,"modo","EXCLUSIVA","parametros",Map.of("minimo",0,"valor",5));
        postJson("/api/admin/promocoes",admin,exclusiva).andExpect(status().isCreated());
        var p=previa(ECO);assertThat(p.at("/confirmacao/freteLiquido").decimalValue()).isEqualByComparingTo("25");
        assertThat(p.at("/confirmacao/total").decimalValue()).isEqualByComparingTo("40");
        jdbc.update("UPDATE promocao_config SET ativo=false");
        postJson("/api/admin/cupons",admin,Map.of("codigo","REDUZ","nome","Reduz frete","tipo","REDUCAO_FRETE",
                "inicio",Instant.now().minusSeconds(60).toString(),"fim",Instant.now().plusSeconds(3600).toString(),"acumulavel",true,"limiteTotal",5,
                "parametros",Map.of("valor",10))).andExpect(status().isCreated());
        mvc.perform(put(CAR+"/cupom").header("Authorization",cliente.bearer()).contentType(MediaType.APPLICATION_JSON).content("{\"codigo\":\"REDUZ\"}")).andExpect(status().isOk());
        assertThat(previa(ECO).at("/confirmacao/freteLiquido").decimalValue()).isEqualByComparingTo("15");
        assertThat(previa(EXP).at("/confirmacao/freteLiquido").decimalValue()).isEqualByComparingTo("30");
    }
    @Test void recusaDePagamentoDevolveCupomDeFreteSemGerarEstorno()throws Exception{
        incluir(2);cupomFrete();long id=comprar(ECO);
        var c=node(postJson("/api/clientes/me/pedidos/"+id+"/pagamento",cliente.bearer(),Map.of("forma","PIX")).andExpect(status().isCreated()));
        var confirmacao=Map.of("idTransacaoExterna",c.get("idTransacaoExterna").asText(),"situacao","RECUSADO");
        for(int i=0;i<2;i++)postJson("/api/admin/pagamentos/confirmacoes",admin,confirmacao).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT usos_ativos FROM saldo_cupom",Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT disponivel FROM saldo_estoque",Integer.class)).isEqualTo(10);
        assertThat(contar("estorno")).isZero();
    }

}
