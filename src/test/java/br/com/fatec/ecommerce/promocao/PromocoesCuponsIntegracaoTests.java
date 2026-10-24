package br.com.fatec.ecommerce.promocao;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.fatec.ecommerce.TesteDeIntegracao;
import br.com.fatec.ecommerce.catalogo.CatalogoPersistidoParaTeste;
import br.com.fatec.ecommerce.cliente.ClientesParaTeste;
import br.com.fatec.ecommerce.cliente.ClientesParaTeste.ClienteDeTeste;
import br.com.fatec.ecommerce.cliente.dominio.*;
import br.com.fatec.ecommerce.promocao.dominio.*;
import br.com.fatec.ecommerce.promocao.aplicacao.*;
import br.com.fatec.ecommerce.promocao.infra.ConfiguracoesJpa;
import br.com.fatec.ecommerce.seguranca.UsuariosParaTeste;
import br.com.fatec.ecommerce.seguranca.dominio.Perfil;
import br.com.fatec.ecommerce.compartilhado.dominio.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.*;

@TesteDeIntegracao
@Import({CatalogoPersistidoParaTeste.class,PromocoesCuponsIntegracaoTests.Extensoes.class})
class PromocoesCuponsIntegracaoTests {
    static final String CAR="/api/clientes/me/carrinho", CHECK="/api/clientes/me/checkout";
    @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired UsuariosParaTeste usuarios;
    @Autowired ClientesParaTeste clientes; @Autowired CatalogoPersistidoParaTeste catalogo;
    @Autowired EnderecoRepository enderecos; @Autowired JdbcTemplate jdbc;
    @Autowired GerenciarPromocoes gerenciar; @Autowired GerenciarCupons gerenciarCupons;
    @MockitoSpyBean ConfiguracoesJpa configuracoes;
    @MockitoSpyBean ConfirmarUsoCupom usos;
    @MockitoSpyBean br.com.fatec.ecommerce.pedido.aplicacao.ConfirmarReservaDoPedido confirmarPedido;
    @Autowired(required=false) br.com.fatec.ecommerce.infra.demo.PromocoesDeDemonstracao cargaDev;
    @MockitoSpyBean br.com.fatec.ecommerce.carrinho.dominio.CarrinhoRepository carrinhos;
    ClienteDeTeste cliente,outro; Endereco endereco,enderecoOutro; String admin,promotor; Long categoriaId;
    @TestConfiguration static class Extensoes {
        @Bean PromocaoFactory sextaPromocao() {
            return new PromocaoFactory() {
                public String tipo() { return "SEXTA_TESTE"; }
                public List<String> parametros() { return List.of("valor"); }
                public Promocao criar(PromocaoConfig c,CondicaoElegibilidade condicao) {
                    return new PromocaoBase(c,condicao) {
                        private final Dinheiro valor=Parametros.dinheiro(c.parametros(),"valor");
                        protected Dinheiro beneficio(ContextoPreco contexto) { return valor; }
                    };
                }
            };
        }
        @Bean CriadorCondicao novaCondicao() {
            return new CriadoresDeCondicao.Criador("SEMPRE_TESTE",List.of(),(p,f)->contexto->true);
        }
    }
    @BeforeEach void preparar() {
        usuarios.limpar(); cliente=clientes.criar("comprador@prom.local",901); outro=clientes.criar("outro@prom.local",902);
        endereco=endereco(cliente); enderecoOutro=endereco(outro);
        admin=usuarios.bearer(usuarios.criar("admin@prom.local",Perfil.ADMIN));
        promotor=usuarios.bearer(usuarios.criar("campanhas@prom.local",Perfil.PROMOCOES));
        var cat=catalogo.categoria("Livros",null); categoriaId=cat.getId();
        catalogo.variacao(cat,"LIVRO","100"); catalogo.variacao(cat,"TECLADO","200");
        jdbc.update("UPDATE saldo_estoque SET disponivel=100");
    }
    void confirmarPagamento(long id, boolean aprovado) throws Exception {
        var inicio=postJson("/api/clientes/me/pedidos/"+id+"/pagamento",cliente.bearer(),Map.of("forma","PIX"))
                .andExpect(status().isCreated()).andReturn();
        var tx=json.readTree(inicio.getResponse().getContentAsString()).get("idTransacaoExterna").asText();
        postJson("/api/admin/pagamentos/confirmacoes",admin,Map.of("idTransacaoExterna",tx,
                "situacao",aprovado?"APROVADO":"RECUSADO")).andExpect(status().isOk());
    }
    Endereco endereco(ClienteDeTeste c) {
        var e=new Endereco(c.cliente().getId(),"Casa","01001000","Rua","1",null,"Centro","São Paulo","SP");
        e.marcarPrincipal(); return enderecos.salvar(e);
    }
    ResultActions postJson(String url,String token,Object corpo) throws Exception {
        return mvc.perform(post(url).header("Authorization",token).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corpo)));
    }
    JsonNode node(ResultActions resultado) throws Exception { return json.readTree(resultado.andReturn().getResponse().getContentAsString()); }
    Map<String,Object> campanha(String tipo,Map<String,Object> parametros) {
        return new HashMap<>(Map.of("nome","Campanha "+tipo,"tipo",tipo,"inicio",Instant.now().minusSeconds(60).toString(),
                "fim",Instant.now().plusSeconds(3600).toString(),"prioridade",10,"modo","ACUMULAVEL","parametros",parametros));
    }
    JsonNode criarCampanha(String tipo,Map<String,Object> parametros) throws Exception {
        return node(postJson("/api/admin/promocoes",promotor,campanha(tipo,parametros)).andExpect(status().isCreated()));
    }
    Map<String,Object> cupomDados(String codigo,int limite,Integer individual) {
        var mapa=new HashMap<String,Object>(Map.of("codigo",codigo,"nome","Cupom "+codigo,"tipo","PERCENTUAL",
                "inicio",Instant.now().minusSeconds(60).toString(),"fim",Instant.now().plusSeconds(3600).toString(),
                "acumulavel",true,"limiteTotal",limite,"parametros",Map.of("percentual",10)));
        if(individual!=null) { mapa.put("limiteCliente",individual); } return mapa;
    }
    JsonNode criarCupom(String codigo,int limite,Integer individual) throws Exception {
        return node(postJson("/api/admin/cupons",promotor,cupomDados(codigo,limite,individual)).andExpect(status().isCreated()));
    }
    void incluir(ClienteDeTeste c,String sku,int q) throws Exception {
        postJson(CAR+"/itens",c.bearer(),Map.of("sku",sku,"quantidade",q)).andExpect(status().isOk());
    }
    ResultActions aplicar(ClienteDeTeste c,String codigo) throws Exception {
        return mvc.perform(put(CAR+"/cupom").header("Authorization",c.bearer()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("codigo",codigo))));
    }
    JsonNode previa(ClienteDeTeste c,Endereco e) throws Exception {
        return node(postJson(CHECK+"/previa",c.bearer(),Map.of("opcaoEntrega","economica-simulada:ECONOMICA","enderecoId",e.getId())).andExpect(status().isOk()));
    }
    ResultActions concluir(ClienteDeTeste c,Endereco e,JsonNode p) throws Exception {
        return postJson(CHECK,c.bearer(),Map.of("opcaoEntrega","economica-simulada:ECONOMICA","enderecoId",e.getId(),"confirmacao",p.get("confirmacao")));
    }
    JsonNode comprar(ClienteDeTeste c,Endereco e) throws Exception { return node(concluir(c,e,previa(c,e)).andExpect(status().isCreated())); }
    void cancelar(long id,String token) throws Exception {
        String rota=token.equals(admin)?"/api/admin/pedidos/":"/api/clientes/me/pedidos/";
        postJson(rota+id+"/cancelamento",token,Map.of("motivo","Teste de cancelamento")).andExpect(status().isOk());
    }
    int contar(String tabela) { return jdbc.queryForObject("SELECT count(*) FROM "+tabela,Integer.class); }
    int ativos(long id) { return jdbc.queryForObject("SELECT usos_ativos FROM saldo_cupom WHERE cupom_id=?",Integer.class,id); }
    @Test void cadastroNormalizacaoAutorizacaoTiposEExtensaoPorRegistro() throws Exception {
        assertThat(cargaDev).isNull();
        for(String area:List.of("promocoes","cupons")) {
            mvc.perform(get("/api/admin/"+area)).andExpect(status().isUnauthorized());
            for(Perfil perfil:List.of(Perfil.CLIENTE,Perfil.ESTOQUE,Perfil.OPERADOR_COMERCIAL)) {
                String token=usuarios.bearer(usuarios.criar(area+perfil+"@teste.local",perfil));
                mvc.perform(get("/api/admin/"+area).header("Authorization",token)).andExpect(status().isForbidden());
            }
            mvc.perform(get("/api/admin/"+area+"/999999").header("Authorization",promotor)).andExpect(status().isNotFound());
            mvc.perform(get("/api/admin/"+area+"?size=101").header("Authorization",promotor)).andExpect(status().isBadRequest());
        }
        var dados=campanha("SEXTA_TESTE",Map.of("valor",7)); dados.put("condicao",Map.of("tipo","E","filhos",List.of(Map.of("tipo","SEMPRE_TESTE"),Map.of("tipo","VALOR_MINIMO","parametros",Map.of("minimo",1)))));
        postJson("/api/admin/promocoes",promotor,dados).andExpect(status().isCreated());
        incluir(cliente,"LIVRO",1);
        mvc.perform(get(CAR).header("Authorization",cliente.bearer())).andExpect(jsonPath("$.total").value(93));
        mvc.perform(get("/api/admin/promocoes/tipos").header("Authorization",promotor)).andExpect(jsonPath("$.SEXTA_TESTE").exists());
        mvc.perform(get("/api/admin/promocoes/condicoes").header("Authorization",promotor)).andExpect(jsonPath("$.SEMPRE_TESTE").exists());
        criarCupom(" bemvindo10 ",10,1);
        postJson("/api/admin/cupons",promotor,cupomDados("BEMVINDO10",10,1)).andExpect(status().isConflict());
        for(var invalida:List.of(campanha("DESCONHECIDA",Map.of()),campanha("PERCENTUAL_PRIMEIRA_COMPRA",Map.of("percentual",101)),
                campanha("LEVE_X_PAGUE_Y",Map.of("sku","INEXISTENTE","leve",3,"pague",2)),
                campanha("PERCENTUAL_CATEGORIA",Map.of("categoriaId",99999,"percentual",10)))) {
            postJson("/api/admin/promocoes",promotor,invalida).andExpect(status().isBadRequest());
        }
        assertThat(contar("promocao_config")).isEqualTo(1);
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/admin/cupons'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['"+CAR+"/cupom'].put").exists());
    }
    @Test @WithMockUser(roles="ESTOQUE") void aplicacaoTambemExigeResponsavelPorPromocoes() {
        assertThatThrownBy(()->gerenciar.listar(0,20)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(()->gerenciarCupons.listar(0,20)).isInstanceOf(AccessDeniedException.class);
    }
    @Test void cupomPersistidoSubstituicaoRejeitadaEExpiracaoRecuperavel() throws Exception {
        var c=criarCupom("DEZ",10,1); criarCupom("OUTRO",10,null);
        incluir(cliente,"LIVRO",1);
        aplicar(cliente," dez ").andExpect(status().isOk()).andExpect(jsonPath("$.total").value(90))
                .andExpect(jsonPath("$.cupom.codigo").value("DEZ"));
        aplicar(cliente,"AUSENTE").andExpect(status().isConflict()).andExpect(jsonPath("$.motivo").value("INEXISTENTE"));
        mvc.perform(get(CAR).header("Authorization",cliente.bearer())).andExpect(jsonPath("$.cupom.codigo").value("DEZ"));
        mvc.perform(get(CAR).header("Authorization",outro.bearer())).andExpect(jsonPath("$.cupom").isEmpty());
        jdbc.update("UPDATE cupom SET fim=now()-interval '1 second',inicio=now()-interval '1 day' WHERE id=?",c.get("id").asLong());
        mvc.perform(get(CAR).header("Authorization",cliente.bearer())).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(100)).andExpect(jsonPath("$.cupom.motivo").value("FORA_DA_VIGENCIA"));
        incluir(cliente,"LIVRO",1);
        var p=previa(cliente,endereco);
        concluir(cliente,endereco,p).andExpect(status().isConflict()).andExpect(jsonPath("$.motivo").value("FORA_DA_VIGENCIA"));
        assertThat(contar("pedido")).isZero(); assertThat(contar("uso_cupom")).isZero();
        mvc.perform(delete(CAR+"/cupom").header("Authorization",cliente.bearer())).andExpect(status().isOk());
        mvc.perform(delete(CAR+"/cupom").header("Authorization",cliente.bearer())).andExpect(status().isOk());
        aplicar(cliente,"OUTRO").andExpect(status().isOk()).andExpect(jsonPath("$.total").value(180));
        assertThat(jdbc.queryForObject("SELECT cupom_codigo FROM carrinho WHERE cliente_id=?",String.class,cliente.cliente().getId())).isEqualTo("OUTRO");
        assertThat(ativos(c.get("id").asLong())).isZero();
        String longo="A".repeat(40); criarCupom(longo,10,null);
        aplicar(cliente," "+longo.toLowerCase(Locale.ROOT)+" ").andExpect(status().isOk()).andExpect(jsonPath("$.cupom.codigo").value(longo));
    }
    @Test void cupomInativoLimiteIndividualIncompatibilidadeEQuantidadeRecalculada() throws Exception {
        var c=criarCupom("SOUM",10,1); incluir(cliente,"LIVRO",1);
        aplicar(cliente,"SOUM").andExpect(status().isOk());
        var pedido=comprar(cliente,endereco);
        incluir(cliente,"LIVRO",1);
        aplicar(cliente,"SOUM").andExpect(status().isConflict()).andExpect(jsonPath("$.motivo").value("LIMITE_CLIENTE"));
        cancelar(pedido.get("id").asLong(),cliente.bearer());
        aplicar(cliente,"SOUM").andExpect(status().isOk());
        var exclusiva=campanha("VALOR_FIXO_ACIMA_DE",Map.of("minimo",0,"valor",50)); exclusiva.put("modo","EXCLUSIVA");
        postJson("/api/admin/promocoes",promotor,exclusiva).andExpect(status().isCreated());
        mvc.perform(get(CAR).header("Authorization",cliente.bearer())).andExpect(jsonPath("$.total").value(50))
                .andExpect(jsonPath("$.cupom.motivo").value("INCOMPATIVEL"));
        aplicar(cliente,"SOUM").andExpect(status().isConflict());
        jdbc.update("UPDATE cupom SET ativo=false WHERE id=?",c.get("id").asLong());
        aplicar(cliente,"SOUM").andExpect(status().isConflict()).andExpect(jsonPath("$.motivo").value("INATIVO"));
    }
    @Test void checkoutDiscriminaOrigensPreservaSnapshotEDevolveNaRecusaECancelamento() throws Exception {
        criarCampanha("VALOR_FIXO_ACIMA_DE",Map.of("minimo",0,"valor",10));
        var cupom=criarCupom("DEZ",10,1);
        incluir(cliente,"LIVRO",1); aplicar(cliente,"DEZ").andExpect(status().isOk()).andExpect(jsonPath("$.total").value(81));
        assertThat(contar("uso_cupom")).isZero();
        var pedido=comprar(cliente,endereco); long id=pedido.get("id").asLong();
        assertThat(pedido.get("total").decimalValue()).isEqualByComparingTo("106");
        assertThat(pedido.get("descontos").toString()).contains("PROMOCAO","CUPOM","referenciaId","DEZ");
        assertThat(ativos(cupom.get("id").asLong())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT cupom_codigo FROM carrinho",String.class)).isNull();
        confirmarPagamento(id,false);
        cancelar(id,cliente.bearer()); usos.devolver(id);
        assertThat(ativos(cupom.get("id").asLong())).isZero();
        assertThat(jdbc.queryForObject("SELECT situacao FROM uso_cupom",String.class)).isEqualTo("DEVOLVIDO");
        jdbc.update("UPDATE promocao_config SET ativo=false"); jdbc.update("UPDATE cupom SET nome='Outro nome'");
        mvc.perform(get("/api/clientes/me/pedidos/"+id).header("Authorization",cliente.bearer()))
                .andExpect(jsonPath("$.total").value(106)).andExpect(jsonPath("$.descontos",hasSize(2)));
    }
    @Test void alteracaoDeCampanhaECupomComMesmoValorExigeNovaConfirmacao() throws Exception {
        var campanha=criarCampanha("VALOR_FIXO_ACIMA_DE",Map.of("minimo",0,"valor",10));
        criarCupom("DEZ",10,null); criarCupom("DEZOUTRO",10,null); incluir(cliente,"LIVRO",1);
        aplicar(cliente,"DEZ").andExpect(status().isOk()); var p=previa(cliente,endereco);
        aplicar(cliente,"DEZOUTRO").andExpect(status().isOk());
        concluir(cliente,endereco,p).andExpect(status().isConflict()).andExpect(jsonPath("$.divergencias").isNotEmpty());
        p=previa(cliente,endereco);
        postJson("/api/admin/promocoes/"+campanha.get("id").asLong()+"/inativacao",promotor,Map.of("versao",campanha.get("versao").asLong()))
                .andExpect(status().isOk());
        concluir(cliente,endereco,p).andExpect(status().isConflict()).andExpect(jsonPath("$.previaAtualizada.confirmacao.total").value(115));
        assertThat(contar("pedido")).isZero(); assertThat(contar("uso_cupom")).isZero();
        comprar(cliente,endereco);
    }
    @Test void primeiraCompraFavoreceTentativasNaoPagasMasHistoricoPagoCanceladoConta() throws Exception {
        criarCampanha("PERCENTUAL_PRIMEIRA_COMPRA",Map.of("percentual",10)); incluir(cliente,"LIVRO",1);
        var primeiro=comprar(cliente,endereco); long id=primeiro.get("id").asLong();
        cancelar(id,cliente.bearer()); incluir(cliente,"LIVRO",1);
        mvc.perform(get(CAR).header("Authorization",cliente.bearer())).andExpect(jsonPath("$.total").value(90));
        var segundo=comprar(cliente,endereco); long pago=segundo.get("id").asLong();
        incluir(cliente,"LIVRO",1); var p=previa(cliente,endereco);
        confirmarPagamento(pago,true);
        concluir(cliente,endereco,p).andExpect(status().isConflict()).andExpect(jsonPath("$.previaAtualizada.confirmacao.total").value(125));
        cancelar(pago,admin);
        postJson(CHECK+"/previa",cliente.bearer(),Map.of("opcaoEntrega","economica-simulada:ECONOMICA","enderecoId",endereco.getId(),"primeiraCompra",true,
                "perfis",List.of("ADMIN"),"clienteId",outro.cliente().getId())).andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmacao.total").value(125));
        mvc.perform(get(CAR).header("Authorization",cliente.bearer())).andExpect(jsonPath("$.total").value(100));
    }
    @Test void descontoPorRegiaoUsaPrincipalNoCarrinhoEEscolhidoNaPrevia() throws Exception {
        var dados=campanha("VALOR_FIXO_ACIMA_DE",Map.of("minimo",0,"valor",10));
        dados.put("condicao",Map.of("tipo","REGIAO","parametros",Map.of("ufs",List.of("SP"))));
        postJson("/api/admin/promocoes",promotor,dados).andExpect(status().isCreated()); incluir(cliente,"LIVRO",1);
        mvc.perform(get(CAR).header("Authorization",cliente.bearer())).andExpect(jsonPath("$.total").value(90));
        var rj=enderecos.salvar(new Endereco(cliente.cliente().getId(),"RJ","20000000","Rua","1",null,"Centro","Rio de Janeiro","RJ"));
        assertThat(previa(cliente,rj).at("/confirmacao/total").decimalValue()).isEqualByComparingTo("125");
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"CONSUMO","CONFIRMACAO"})
    void falhaDepoisDoConsumoFazRollbackInclusiveDasReservasESelecao(String etapa) throws Exception {
        var cupom=criarCupom("DEZ",10,null); incluir(cliente,"LIVRO",1); aplicar(cliente,"DEZ").andExpect(status().isOk());
        var p=previa(cliente,endereco);
        if (etapa.equals("CONSUMO")) {
            doAnswer(i->{ i.callRealMethod(); throw new ConflitoException("Falha após consumo"); }).when(usos).executar(anyLong(),anyLong(),anyLong(),anyLong());
        } else {
            doAnswer(i->{ i.callRealMethod(); throw new ConflitoException("Falha após confirmação"); }).when(confirmarPedido).executar(anyLong());
        }
        concluir(cliente,endereco,p).andExpect(status().isConflict());
        assertThat(contar("pedido")).isZero(); assertThat(contar("pedido_historico")).isZero(); assertThat(contar("reserva_estoque")).isZero();
        assertThat(contar("uso_cupom")).isZero(); assertThat(ativos(cupom.get("id").asLong())).isZero();
        assertThat(contar("item_carrinho")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT sum(reservado) FROM saldo_estoque",Integer.class)).isZero();
        assertThat(previa(cliente,endereco).get("confirmacao")).isEqualTo(p.get("confirmacao"));
    }
    @Test void ultimaUtilizacaoSobConcorrenciaNaoCriaPedidoNemReservaNaPerdedora() throws Exception {
        var cupom=criarCupom("UNICO",1,null);
        incluir(cliente,"LIVRO",1); incluir(outro,"LIVRO",1);
        aplicar(cliente,"UNICO").andExpect(status().isOk()); aplicar(outro,"UNICO").andExpect(status().isOk());
        var p1=previa(cliente,endereco); var p2=previa(outro,enderecoOutro); var barreira=new CyclicBarrier(2);
        doAnswer(i->{ var resultado=i.callRealMethod(); barreira.await(15,TimeUnit.SECONDS); return resultado; })
                .when(configuracoes).porCodigo(eq("UNICO"),anyLong());
        try(var executor=Executors.newFixedThreadPool(2)) {
            var a=executor.submit(()->concluir(cliente,endereco,p1).andReturn().getResponse().getStatus());
            var b=executor.submit(()->concluir(outro,enderecoOutro,p2).andReturn().getResponse().getStatus());
            assertThat(List.of(a.get(30,TimeUnit.SECONDS),b.get(30,TimeUnit.SECONDS))).containsExactlyInAnyOrder(201,409);
        }
        assertThat(ativos(cupom.get("id").asLong())).isEqualTo(1); assertThat(contar("pedido")).isEqualTo(1);
        assertThat(contar("uso_cupom")).isEqualTo(1); assertThat(contar("reserva_estoque")).isEqualTo(1);
        assertThat(contar("item_carrinho")).isEqualTo(1);
    }
    @Test void limiteIndividualConcorrenteConsumoDuplicadoEDesativacaoConcorrente() throws Exception {
        var c=criarCupom("UMCLIENTE",10,1); long id=c.get("id").asLong(); long versao=c.get("versao").asLong();
        var largada=new CyclicBarrier(2);
        try(var executor=Executors.newFixedThreadPool(2)) {
            List<Future<Boolean>> resultados=new ArrayList<>();
            for(long pedido:List.of(99001L,99002L)) {
                resultados.add(executor.submit(()->{ largada.await(15,TimeUnit.SECONDS); try { usos.executar(id,versao,pedido,cliente.cliente().getId()); return true; }
                    catch(CupomRejeitadoException erro) { assertThat(erro.propriedades()).containsEntry("motivo","LIMITE_CLIENTE"); return false; } }));
            }
            assertThat(List.of(resultados.get(0).get(30,TimeUnit.SECONDS),resultados.get(1).get(30,TimeUnit.SECONDS))).containsExactlyInAnyOrder(true,false);
        }
        long pedido=jdbc.queryForObject("SELECT pedido_id FROM uso_cupom",Long.class);
        assertThatThrownBy(()->usos.executar(id,versao,pedido,cliente.cliente().getId())).isInstanceOf(ConflitoException.class);
        assertThat(ativos(id)).isEqualTo(1); usos.devolver(pedido); usos.devolver(pedido); assertThat(ativos(id)).isZero();
        postJson("/api/admin/cupons/"+id+"/inativacao",promotor,Map.of("versao",versao)).andExpect(status().isOk());
        assertThatThrownBy(()->usos.executar(id,versao,99003L,cliente.cliente().getId())).isInstanceOf(CupomRejeitadoException.class);
    }
    @Test void edicaoVersionadaEContagemAtivaNaoPodemSerApagadas() throws Exception {
        var c=criarCupom("LIMITADO",10,2); long id=c.get("id").asLong(),versao=c.get("versao").asLong();
        usos.executar(id,versao,991L,cliente.cliente().getId()); usos.executar(id,versao,992L,cliente.cliente().getId());
        var dados=cupomDados("LIMITADO",1,1); dados.put("versao",versao);
        mvc.perform(put("/api/admin/cupons/"+id).header("Authorization",promotor).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(dados))).andExpect(status().isConflict());
        assertThat(ativos(id)).isEqualTo(2);
        dados.put("limiteTotal",20); dados.put("limiteCliente",3);
        mvc.perform(put("/api/admin/cupons/"+id).header("Authorization",promotor).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(dados))).andExpect(status().isOk());
        mvc.perform(put("/api/admin/cupons/"+id).header("Authorization",promotor).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(dados))).andExpect(status().isConflict());
        assertThat(ativos(id)).isEqualTo(2);
    }

    @Test void duasConclusoesComCupomDaMesmaSelecaoConsomemUmaVez() throws Exception {
        var c=criarCupom("DEZ",10,null); incluir(cliente,"LIVRO",1); aplicar(cliente,"DEZ").andExpect(status().isOk());
        var p=previa(cliente,endereco); var barreira=new CyclicBarrier(2);
        doAnswer(i->{ var resultado=i.callRealMethod(); barreira.await(15,TimeUnit.SECONDS); return resultado; })
                .when(configuracoes).porCodigo(eq("DEZ"),anyLong());
        try(var executor=Executors.newFixedThreadPool(2)) {
            var a=executor.submit(()->concluir(cliente,endereco,p).andReturn().getResponse().getStatus());
            var b=executor.submit(()->concluir(cliente,endereco,p).andReturn().getResponse().getStatus());
            assertThat(List.of(a.get(30,TimeUnit.SECONDS),b.get(30,TimeUnit.SECONDS))).containsExactlyInAnyOrder(201,409);
        }
        assertThat(ativos(c.get("id").asLong())).isEqualTo(1);
        assertThat(contar("pedido")).isEqualTo(1); assertThat(contar("uso_cupom")).isEqualTo(1);
        assertThat(contar("reserva_estoque")).isEqualTo(1); assertThat(contar("item_carrinho")).isZero();
    }

    @Test void configuracaoAlteradaDuranteCompraNaoEhConsumidaComVersaoAntiga() throws Exception {
        var c=criarCupom("DEZ",10,null); incluir(cliente,"LIVRO",1); aplicar(cliente,"DEZ").andExpect(status().isOk());
        var p=previa(cliente,endereco); var lida=new CountDownLatch(1); var alterada=new CountDownLatch(1);
        doAnswer(i->{ lida.countDown(); assertThat(alterada.await(15,TimeUnit.SECONDS)).isTrue(); return i.callRealMethod(); })
                .when(usos).executar(anyLong(),anyLong(),anyLong(),anyLong());
        try(var executor=Executors.newSingleThreadExecutor()) {
            var compra=executor.submit(()->concluir(cliente,endereco,p).andReturn().getResponse().getStatus());
            assertThat(lida.await(15,TimeUnit.SECONDS)).isTrue();
            try { postJson("/api/admin/cupons/"+c.get("id").asLong()+"/inativacao",promotor,Map.of("versao",c.get("versao").asLong())).andExpect(status().isOk()); }
            finally { alterada.countDown(); }
            assertThat(compra.get(30,TimeUnit.SECONDS)).isEqualTo(409);
        }
        assertThat(contar("pedido")).isZero(); assertThat(contar("reserva_estoque")).isZero();
        assertThat(contar("uso_cupom")).isZero(); assertThat(ativos(c.get("id").asLong())).isZero();
        assertThat(contar("item_carrinho")).isEqualTo(1);
    }

    @Test void falhaDepoisDoFlushDaLimpezaReverteTambemUsoDoCupom() throws Exception {
        var c=criarCupom("DEZ",10,null); incluir(cliente,"LIVRO",1); aplicar(cliente,"DEZ").andExpect(status().isOk()); var p=previa(cliente,endereco);
        doAnswer(i->{ var resultado=i.callRealMethod();
            if(((br.com.fatec.ecommerce.carrinho.dominio.Carrinho)i.getArgument(0)).getItens().isEmpty()) { throw new ConflitoException("Falha depois do flush"); }
            return resultado;
        }).when(carrinhos).salvar(any());
        concluir(cliente,endereco,p).andExpect(status().isConflict());
        assertThat(contar("pedido")).isZero(); assertThat(contar("pedido_historico")).isZero();
        assertThat(contar("uso_cupom")).isZero(); assertThat(contar("reserva_estoque")).isZero();
        assertThat(ativos(c.get("id").asLong())).isZero();
        assertThat(previa(cliente,endereco).get("confirmacao")).isEqualTo(p.get("confirmacao"));
    }

    @Test void devolucaoENovoConsumoConcorrentesNaoGeramSaldoNegativoNemEsperaCircular() throws Exception {
        var c=criarCupom("UNICO",1,null); long id=c.get("id").asLong(),versao=c.get("versao").asLong();
        usos.executar(id,versao,11001L,cliente.cliente().getId()); var barreira=new CyclicBarrier(2);
        try(var executor=Executors.newFixedThreadPool(2)) {
            var devolucao=executor.submit(()->{ barreira.await(10,TimeUnit.SECONDS); usos.devolver(11001L); return true; });
            var consumo=executor.submit(()->{ barreira.await(10,TimeUnit.SECONDS);
                try { usos.executar(id,versao,11002L,outro.cliente().getId()); return true; }
                catch(CupomRejeitadoException erro) { return false; }
            });
            assertThat(devolucao.get(20,TimeUnit.SECONDS)).isTrue(); boolean consumiu=consumo.get(20,TimeUnit.SECONDS);
            assertThat(ativos(id)).isEqualTo(consumiu?1:0);
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM uso_cupom WHERE situacao='CONSUMIDO'",Integer.class)).isEqualTo(ativos(id));
        assertThat(jdbc.queryForObject("SELECT situacao FROM uso_cupom WHERE pedido_id=11001",String.class)).isEqualTo("DEVOLVIDO");
    }

    @Test void beneficioZeroNaoConsomeEFaltaDeEstoqueNaoGastaCupom() throws Exception {
        var dados=cupomDados("FRETE",1,1); dados.put("tipo","FRETE_GRATIS"); dados.put("parametros",Map.of());
        var c=node(postJson("/api/admin/cupons",promotor,dados).andExpect(status().isCreated()));
        incluir(cliente,"LIVRO",1); aplicar(cliente,"FRETE").andExpect(status().isOk()).andExpect(jsonPath("$.descontos",hasSize(0)));
        criarCampanha("FRETE_GRATIS",Map.of()); // frete ja abatido: beneficio efetivo do cupom continua zero na E7.
        comprar(cliente,endereco);
        assertThat(ativos(c.get("id").asLong())).isZero(); assertThat(contar("uso_cupom")).isZero();
        var cupom=criarCupom("DEZ",1,null); incluir(cliente,"LIVRO",1); aplicar(cliente,"DEZ").andExpect(status().isOk());
        var p=previa(cliente,endereco); jdbc.update("UPDATE saldo_estoque SET disponivel=0");
        concluir(cliente,endereco,p).andExpect(status().isConflict());
        assertThat(ativos(cupom.get("id").asLong())).isZero(); assertThat(contar("uso_cupom")).isZero();
        assertThat(contar("pedido")).isEqualTo(1); assertThat(contar("item_carrinho")).isEqualTo(1);
    }
}
