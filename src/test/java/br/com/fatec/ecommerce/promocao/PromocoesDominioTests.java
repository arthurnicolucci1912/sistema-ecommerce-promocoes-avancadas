package br.com.fatec.ecommerce.promocao;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import br.com.fatec.ecommerce.promocao.dominio.*;
import br.com.fatec.ecommerce.promocao.aplicacao.ContextoDeCompra;
import br.com.fatec.ecommerce.pedido.dominio.PedidoRepository;
import br.com.fatec.ecommerce.cliente.dominio.EnderecoRepository;
import br.com.fatec.ecommerce.compartilhado.dominio.*;
import br.com.fatec.ecommerce.preco.dominio.*;
import br.com.fatec.ecommerce.preco.aplicacao.PipelinePreco;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

class PromocoesDominioTests {
    static final Instant AGORA=Instant.parse("2026-10-22T12:00:00Z");
    CondicaoRegistry condicoes=new CondicaoRegistry(CriadoresDeCondicao.padrao());
    PromocaoFactoryRegistry fabricas=new PromocaoFactoryRegistry(List.of(new LeveXPagueYFactory(),new PercentualPorCategoriaFactory(),
            new ValorFixoAcimaDeFactory(),new PercentualPrimeiraCompraFactory(),new FreteGratisFactory()),condicoes);
    BeneficioCupomRegistry beneficios=new BeneficioCupomRegistry(BeneficioCupomRegistry.padrao());
    ContextoPreco contexto(String subtotal) { return contexto(subtotal,true,null,"0"); }
    ContextoPreco contexto(String subtotal,boolean primeira,String cupom,String frete) {
        return new ContextoPreco(List.of(new ItemPreco(1L,"LIVRO",Set.of(1L,2L),1,Dinheiro.de("100")),
                new ItemPreco(2L,"OUTRO",Set.of(3L),1,Dinheiro.de("300"))),5L,primeira,"SP",Dinheiro.de(subtotal),AGORA,cupom,Set.of("CLIENTE"),Dinheiro.de(frete));
    }
    PromocaoConfig config(long id,String tipo,Map<String,Object> parametros,int prioridade,ModoAcumulacao modo,CondicaoConfig condicao) {
        var c=new PromocaoConfig("P"+id,tipo,AGORA.minusSeconds(60),AGORA.plusSeconds(60),prioridade,modo,parametros,condicao,AGORA.minusSeconds(id));
        ReflectionTestUtils.setField(c,"id",id); return c;
    }
    Promocao campanha(long id,String tipo,Map<String,Object> p,int prioridade,ModoAcumulacao modo) { return fabricas.criar(config(id,tipo,p,prioridade,modo,null)); }
    DescontoCandidato candidato(long id,String valor,int prioridade,ModoAcumulacao modo) {
        return new DescontoCandidato(id,"P"+id,Dinheiro.de(valor),AlvoDesconto.ITENS,modo,prioridade,AGORA.minusSeconds(id));
    }
    CondicaoConfig folha(String tipo,Map<String,Object> p) { return new CondicaoConfig(tipo,p,List.of()); }
    @Test void contratosLegadosSaoCompativeisEContextoCopiaColecoes() {
        var itens=new ArrayList<ItemPreco>(); var c=new ContextoPreco(itens,1L,true,null,Dinheiro.zero(),AGORA);
        itens.add(new ItemPreco(1L,"A",Set.of(),1,Dinheiro.de("10"))); assertThat(c.itens()).isEmpty();
        assertThat(c.freteBruto()).isEqualTo(Dinheiro.zero()); assertThat(c.perfis()).isEmpty();
        assertThat(new DescontoAplicado(1L,"P",Dinheiro.de("1"),AlvoDesconto.ITENS).promocaoId()).isEqualTo(1L);
        assertThat(new DescontoCandidato(1L,"P",Dinheiro.de("1"),AlvoDesconto.ITENS,ModoAcumulacao.ACUMULAVEL,1).criadoEm()).isEqualTo(Instant.EPOCH);
    }
    @ParameterizedTest @CsvSource({"true,true,true,true","true,false,false,true","false,true,false,true","false,false,false,false"})
    void tabelaVerdadeDosCompostos(boolean a,boolean b,boolean e,boolean ou) {
        CondicaoElegibilidade primeira=c->a,segunda=c->b;
        assertThat(new CondicaoE(List.of(primeira,segunda)).avaliar(contexto("400"))).isEqualTo(e);
        assertThat(new CondicaoOu(List.of(primeira,segunda)).avaliar(contexto("400"))).isEqualTo(ou);
        assertThat(new CondicaoNao(primeira).avaliar(contexto("400"))).isEqualTo(!a);
    }
    @Test void folhasEArvoreDeclarativaUsamDadosDoContexto() {
        var c=contexto("400");
        for(var f:List.of(folha("PRIMEIRA_COMPRA",Map.of()),folha("VALOR_MINIMO",Map.of("minimo",300)),
                folha("CATEGORIA",Map.of("categoriaId",1)),folha("SKU",Map.of("sku","LIVRO")),
                folha("REGIAO",Map.of("ufs",List.of("sp"))),folha("PERFIL",Map.of("perfil","CLIENTE")),
                folha("PERIODO",Map.of("inicio",AGORA.toString(),"fim",AGORA.plusSeconds(1).toString())))) {
            assertThat(condicoes.criar(f).avaliar(c)).as(f.tipo()).isTrue();
        }
        assertThat(condicoes.criar(new CondicaoConfig("E",null,List.of(folha("VALOR_MINIMO",Map.of("minimo",300)),
                new CondicaoConfig("NAO",null,List.of(folha("PRIMEIRA_COMPRA",Map.of())))))).avaliar(c)).isFalse();
        var semUf=new ContextoPreco(c.itens(),5L,true,null,c.subtotal(),AGORA);
        assertThat(condicoes.criar(folha("REGIAO",Map.of("ufs",List.of("SP")))).avaliar(semUf)).isFalse();
    }
    @Test void invalidaTiposFilhosProfundidadeTamanhoEParametros() {
        assertThatThrownBy(()->condicoes.criar(folha("DESCONHECIDA",Map.of()))).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(()->condicoes.criar(folha("E",Map.of()))).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(()->condicoes.criar(folha("NAO",Map.of()))).isInstanceOf(DadoInvalidoException.class);
        CondicaoConfig arvore=folha("PRIMEIRA_COMPRA",Map.of());
        for(int i=0;i<16;i++) { arvore=new CondicaoConfig("NAO",null,List.of(arvore)); }
        final var profunda=arvore;
        assertThatThrownBy(()->condicoes.criar(profunda)).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(()->condicoes.criar(new CondicaoConfig("E",null,Collections.nCopies(129,folha("PRIMEIRA_COMPRA",Map.of()))))).isInstanceOf(DadoInvalidoException.class);
        for(var p:List.of(Map.<String,Object>of("percentual",101),Map.<String,Object>of("percentual",0))) {
            assertThatThrownBy(()->campanha(1,"PERCENTUAL_PRIMEIRA_COMPRA",p,1,ModoAcumulacao.ACUMULAVEL)).isInstanceOf(DadoInvalidoException.class);
        }
        assertThatThrownBy(()->campanha(1,"LEVE_X_PAGUE_Y",Map.of("sku","A","leve",2,"pague",3),1,ModoAcumulacao.ACUMULAVEL)).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(()->new PromocaoFactoryRegistry(List.of(new FreteGratisFactory(),new FreteGratisFactory()),condicoes)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void leveXPagueYArredondamentoVigenciaEKitsPeloSkuComercial() {
        var ctx=new ContextoPreco(List.of(new ItemPreco(1L,"KIT",Set.of(),7,Dinheiro.de("10"))),5L,true,null,Dinheiro.de("70"),AGORA);
        var p=campanha(1,"LEVE_X_PAGUE_Y",Map.of("sku","KIT","leve",3,"pague",2),1,ModoAcumulacao.ACUMULAVEL);
        assertThat(p.avaliar(ctx).orElseThrow().valor()).isEqualTo(Dinheiro.de("20"));
        var percentual=campanha(2,"PERCENTUAL_PRIMEIRA_COMPRA",Map.of("percentual",15),1,ModoAcumulacao.ACUMULAVEL);
        assertThat(percentual.avaliar(contexto("33.30")).orElseThrow().valor()).isEqualTo(Dinheiro.de("5"));
        assertThat(percentual.avaliar(contexto("33.30",false,null,"0"))).isEmpty();
        assertThat(percentual.vigenteEm(AGORA.minusSeconds(60))).isTrue();
        assertThat(percentual.vigenteEm(AGORA.plusSeconds(60))).isFalse();
    }
    @Test void politicaD4CenariosADeterminismoETetos() {
        var a=candidato(1,"50",10,ModoAcumulacao.ACUMULAVEL);
        var b=candidato(2,"20",20,ModoAcumulacao.ACUMULAVEL);
        var c=candidato(3,"40",5,ModoAcumulacao.NAO_ACUMULAVEL);
        var d=candidato(4,"60",5,ModoAcumulacao.NAO_ACUMULAVEL);
        var politica=new PoliticaPadrao();
        assertThat(politica.resolver(List.of(a,b,c))).extracting(DescontoAplicado::referenciaId).containsExactly(2L,1L,3L);
        assertThat(politica.resolver(List.of(c,d,b,a))).extracting(DescontoAplicado::referenciaId).containsExactly(2L,1L,4L);
        assertThat(politica.resolver(List.of(a,b,c,d,candidato(5,"100",1,ModoAcumulacao.EXCLUSIVA)))).hasSize(1).first().extracting(DescontoAplicado::referenciaId).isEqualTo(5L);
        assertThat(politica.resolver(List.of(a,b,c,candidato(6,"40",8,ModoAcumulacao.NAO_ACUMULAVEL)))).extracting(DescontoAplicado::referenciaId).contains(6L).doesNotContain(3L);
        assertThat(politica.resolver(List.of(candidato(7,"40",8,ModoAcumulacao.NAO_ACUMULAVEL),candidato(6,"40",8,ModoAcumulacao.NAO_ACUMULAVEL)))).first().extracting(DescontoAplicado::referenciaId).isEqualTo(7L);
        var fixo1=campanha(1,"VALOR_FIXO_ACIMA_DE",Map.of("minimo",0,"valor",50),20,ModoAcumulacao.ACUMULAVEL);
        var fixo2=campanha(2,"VALOR_FIXO_ACIMA_DE",Map.of("minimo",0,"valor",20),10,ModoAcumulacao.ACUMULAVEL);
        assertThat(new MotorDePromocoes(politica).calcular(contexto("60"),List.of(fixo2,fixo1))).extracting(dsc->dsc.valor().toString()).containsExactly("50.00","10.00");
        assertThat(new MotorDePromocoes(new PoliticaSomenteMaiorDesconto()).calcular(contexto("60"),List.of(fixo1,fixo2))).hasSize(1);
    }
    Cupom cupom(String tipo,Map<String,Object> parametros) {
        var c=new Cupom("BEMVINDO10","Bem vindo",tipo,AGORA.minusSeconds(60),AGORA.plusSeconds(60),true,10,1,parametros,null,AGORA);
        ReflectionTestUtils.setField(c,"id",9L); ReflectionTestUtils.setField(c,"versao",0L); return c;
    }
    @Test void cadeiaDesconta400Menos110Menos29ESeparaFreteSemEstadoCompartilhado() {
        var fonte=mock(FonteDeCupons.class); var cupom=cupom("PERCENTUAL",Map.of("percentual",10));
        when(fonte.porCodigo("BEMVINDO10",5L)).thenReturn(Optional.of(new FonteDeCupons.Disponivel(cupom,0,0)));
        var promocoes=List.of(campanha(1,"VALOR_FIXO_ACIMA_DE",Map.of("minimo",300,"valor",50),10,ModoAcumulacao.ACUMULAVEL),
                campanha(2,"PERCENTUAL_CATEGORIA",Map.of("categoriaId",1,"percentual",20),20,ModoAcumulacao.ACUMULAVEL),
                campanha(3,"PERCENTUAL_PRIMEIRA_COMPRA",Map.of("percentual",10),5,ModoAcumulacao.NAO_ACUMULAVEL));
        var etapas=new ArrayList<EtapaPreco>(List.of(new EtapaSubtotal(),new EtapaPromocoes(m->promocoes,new MotorDePromocoes(new PoliticaPadrao())),
                new EtapaCupom(new AvaliadorCupom(fonte,condicoes,beneficios)),new EtapaFreteBruto(),new EtapaTotal()));
        var visitado=new java.util.concurrent.atomic.AtomicBoolean(); etapas.add(1,c->visitado.set(true));
        var pipeline=new PipelinePreco(etapas);
        var calculo=pipeline.calcular(contexto("400",true,"BEMVINDO10","0"));
        assertThat(calculo.total()).isEqualTo(Dinheiro.de("261")); assertThat(visitado).isTrue();
        assertThat(calculo.descontos()).hasSize(4); assertThat(calculo.cupom().valido()).isTrue();
        assertThat(pipeline.calcular(contexto("400",true,null,"25")).total()).isEqualTo(Dinheiro.de("315"));
        var gratis=campanha(4,"FRETE_GRATIS",Map.of(),1,ModoAcumulacao.ACUMULAVEL);
        var frete=new MotorDePromocoes(new PoliticaPadrao()).calcular(contexto("400",true,null,"25"),List.of(gratis));
        assertThat(frete.getFirst().valor()).isEqualTo(Dinheiro.de("25")); assertThat(frete.getFirst().alvo()).isEqualTo(AlvoDesconto.FRETE);
    }
    @Test void beneficiosDeCupomTetoFreteZeroEExtensao() {
        assertThat(new ValorFixoCupom(Map.of("valor",400)).calcular(Dinheiro.de("290"),Dinheiro.zero())).isEqualTo(Dinheiro.de("290"));
        assertThat(new FreteGratisCupom(Map.of()).calcular(Dinheiro.de("290"),Dinheiro.zero())).isEqualTo(Dinheiro.zero());
        assertThat(new ReducaoFreteCupom(Map.of("valor",40)).calcular(Dinheiro.de("290"),Dinheiro.de("25"))).isEqualTo(Dinheiro.de("25"));
        var extras=new ArrayList<>(BeneficioCupomRegistry.padrao()); extras.add(new BeneficioCupomRegistry.Fabrica("NOVO",List.of(),p->new ValorFixoCupom(Map.of("valor",3))));
        assertThat(new BeneficioCupomRegistry(extras).criar(cupom("NOVO",Map.of())).calcular(Dinheiro.de("10"),Dinheiro.zero())).isEqualTo(Dinheiro.de("3"));
    }
    @Test void primeiraCompraDoServidorNaoConfiaEmCriacaoDePedidoEUsaInstanteUnico() {
        var pedidos=mock(PedidoRepository.class); var enderecos=mock(EnderecoRepository.class);
        var montador=new ContextoDeCompra(pedidos,enderecos,Clock.fixed(AGORA,ZoneOffset.UTC));
        assertThat(montador.criar(List.of(),5L,null,Dinheiro.zero(),null).primeiraCompra()).isTrue();
        when(pedidos.existePagamentoAprovadoAte(5L,AGORA)).thenReturn(true);
        assertThat(montador.criar(List.of(),5L,null,Dinheiro.zero(),null).primeiraCompra()).isFalse();
        verify(pedidos,never()).existeDoCliente(any());
    }
}
