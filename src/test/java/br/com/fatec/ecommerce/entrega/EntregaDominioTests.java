package br.com.fatec.ecommerce.entrega;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import br.com.fatec.ecommerce.compartilhado.dominio.*;
import br.com.fatec.ecommerce.entrega.dominio.*;
import br.com.fatec.ecommerce.entrega.infra.*;
import br.com.fatec.ecommerce.pedido.dominio.DadosEntregaSelecionada;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.mock.env.MockEnvironment;

class EntregaDominioTests {
    ContextoFrete contexto(String uf){return new ContextoFrete(uf,BigDecimal.ZERO,Dinheiro.de("100"));}
    @ParameterizedTest @EnumSource(RegiaoEntrega.class)
    void duasApisDistintasTraduzemRegioesETodosEstados(RegiaoEntrega regiao){
        var e=new AdaptadorEntregaEconomica(new MockEnvironment());var x=new AdaptadorEntregaExpressa(new MockEnvironment());
        int[] valores={25,30,35,40,45},dias={5,6,7,9,12},expressos={40,48,56,64,72},rapidos={2,3,3,4,6};
        for(String uf:regiao.ufs()){
            assertThat(e.atende(contexto(uf))).isTrue();assertThat(x.atende(contexto(uf))).isTrue();
            assertThat(e.cotar(contexto(uf)).custo()).isEqualTo(Dinheiro.de(Integer.toString(valores[regiao.ordinal()])));
            assertThat(e.cotar(contexto(uf)).prazoDias()).isEqualTo(dias[regiao.ordinal()]);
            assertThat(x.cotar(contexto(uf)).custo()).isEqualTo(Dinheiro.de(Integer.toString(expressos[regiao.ordinal()])));
            assertThat(x.cotar(contexto(uf)).prazoDias()).isEqualTo(rapidos[regiao.ordinal()]);
        }
    }
    @Test void configuracaoDeTarifaPrazoECoberturaEValidada(){
        var env=new MockEnvironment().withProperty("ecommerce.entrega.economica.sudeste.valor","27.45")
                .withProperty("ecommerce.entrega.economica.sudeste.prazo-dias","7").withProperty("ecommerce.entrega.economica.ufs","SP,RJ");
        var adapter=new AdaptadorEntregaEconomica(env);
        assertThat(adapter.cotar(contexto("sp")).custo()).isEqualTo(Dinheiro.de("27.45"));
        assertThat(adapter.cotar(contexto("SP")).prazoDias()).isEqualTo(7);assertThat(adapter.atende(contexto("BA"))).isFalse();
        assertThat(new AdaptadorEntregaEconomica(new MockEnvironment().withProperty("ecommerce.entrega.economica.ufs","")).atende(contexto("SP"))).isFalse();
        for(var par:List.of(Map.entry("sudeste.valor","-1"),Map.entry("sudeste.prazo-dias","0"),Map.entry("ufs","ZZ")))
            assertThatThrownBy(()->new AdaptadorEntregaEconomica(new MockEnvironment().withProperty("ecommerce.entrega.economica."+par.getKey(),par.getValue()))).isInstanceOf(RuntimeException.class);
    }
    @Test void invariantesDeContextoCotacaoESnapshot(){
        for(String uf:Arrays.asList(null,"ZZ",""))assertThatThrownBy(()->contexto(uf)).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(()->new ContextoFrete("SP",BigDecimal.ONE.negate(),Dinheiro.zero())).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(()->new CotacaoFrete("ECONOMICA",Dinheiro.de("-1"),1)).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(()->new CotacaoFrete("ECONOMICA",Dinheiro.zero(),0)).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(()->new DadosEntregaSelecionada("outra","fornecedor","ECONOMICA",1)).isInstanceOf(DadoInvalidoException.class);
        assertThat(new CotacaoFrete("ECONOMICA",Dinheiro.zero(),1).custo()).isEqualTo(Dinheiro.zero());
    }
    @Test void falhaParcialNaoOcultaOpcoesENenhumaFalhaViraFreteGratis(){
        var a=spy(new AdaptadorEntregaEconomica(new MockEnvironment()));var b=new AdaptadorEntregaExpressa(new MockEnvironment());
        doThrow(new IllegalStateException("offline")).when(a).cotar(any());
        assertThat(new RegistroProvedoresFrete(List.of(a,b)).cotar(contexto("SP"))).singleElement().satisfies(o->assertThat(o.custoBruto()).isEqualByComparingTo("40"));
        assertThatThrownBy(()->new RegistroProvedoresFrete(List.of(a)).cotar(contexto("SP"))).isInstanceOf(ServicoIndisponivelException.class);
        assertThat(new RegistroProvedoresFrete(List.of()).cotar(contexto("SP"))).isEmpty();
    }
    @Test void opcaoDuplicadaERespostaIncoerenteSaoRejeitadas(){
        var a=spy(new AdaptadorEntregaEconomica(new MockEnvironment()));
        assertThatThrownBy(()->new RegistroProvedoresFrete(List.of(a,a))).isInstanceOf(IllegalArgumentException.class);
        doReturn(new CotacaoFrete("OUTRA",Dinheiro.de("1"),1)).when(a).cotar(any());
        assertThatThrownBy(()->new RegistroProvedoresFrete(List.of(a)).cotar(contexto("SP"))).isInstanceOf(ServicoIndisponivelException.class);
    }
}
