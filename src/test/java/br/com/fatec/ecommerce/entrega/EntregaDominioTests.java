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
    ContextoFrete contexto(String uf){return new ContextoFrete(uf,BigDecimal.ZERO,Dinheiro.de("100"),1);}
    ContextoFrete contexto(String uf,String pesoKg,String valor,int itens){return new ContextoFrete(uf,new BigDecimal(pesoKg),Dinheiro.de(valor),itens);}
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
        assertThatThrownBy(()->new ContextoFrete("SP",BigDecimal.ONE.negate(),Dinheiro.zero(),0)).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(()->new ContextoFrete("SP",BigDecimal.ONE,Dinheiro.zero(),-1)).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(()->new CotacaoFrete("ECONOMICA",Dinheiro.de("-1"),1)).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(()->new CotacaoFrete("ECONOMICA",Dinheiro.zero(),0)).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(()->new DadosEntregaSelecionada("outra","fornecedor","ECONOMICA",1)).isInstanceOf(DadoInvalidoException.class);
        assertThat(new CotacaoFrete("ECONOMICA",Dinheiro.zero(),1).custo()).isEqualTo(Dinheiro.zero());
    }
    @Test void custoCrescePorPesoAcimaDaFranquiaEmAmbasAsModalidades(){
        var e=new AdaptadorEntregaEconomica(new MockEnvironment());var x=new AdaptadorEntregaExpressa(new MockEnvironment());
        // sudeste: base 25 (economica) e 40 (expressa); franquia 1 kg; R$ 2,00/kg e R$ 4,00/kg acima dela
        assertThat(e.cotar(contexto("SP","0","100",1)).custo()).isEqualTo(Dinheiro.de("25"));
        assertThat(e.cotar(contexto("SP","1","100",1)).custo()).isEqualTo(Dinheiro.de("25"));
        assertThat(e.cotar(contexto("SP","3","100",1)).custo()).isEqualTo(Dinheiro.de("29"));
        assertThat(e.cotar(contexto("SP","1.25","100",1)).custo()).isEqualTo(Dinheiro.de("25.50"));
        assertThat(e.cotar(contexto("SP","6","100",1)).custo()).isGreaterThan(e.cotar(contexto("SP","3","100",1)).custo());
        assertThat(x.cotar(contexto("SP","3","100",1)).custo()).isEqualTo(Dinheiro.de("48"));
        assertThat(x.cotar(contexto("SP","6","100",1)).custo()).isEqualTo(Dinheiro.de("60"));
    }
    @Test void custoCrescePorQuantidadeDeItensAcimaDaFranquia(){
        var e=new AdaptadorEntregaEconomica(new MockEnvironment());var x=new AdaptadorEntregaExpressa(new MockEnvironment());
        // franquia de 5 itens; R$ 1,00 (economica) e R$ 2,00 (expressa) por item excedente
        assertThat(e.cotar(contexto("SP","0","100",5)).custo()).isEqualTo(Dinheiro.de("25"));
        assertThat(e.cotar(contexto("SP","0","100",6)).custo()).isEqualTo(Dinheiro.de("26"));
        assertThat(e.cotar(contexto("SP","0","100",15)).custo()).isEqualTo(Dinheiro.de("35"));
        assertThat(x.cotar(contexto("SP","0","100",5)).custo()).isEqualTo(Dinheiro.de("40"));
        assertThat(x.cotar(contexto("SP","0","100",15)).custo()).isEqualTo(Dinheiro.de("60"));
    }
    @Test void custoCresceComOValorDaCompraPeloSeguroSobreOExcedenteDaFranquia(){
        var e=new AdaptadorEntregaEconomica(new MockEnvironment());var x=new AdaptadorEntregaExpressa(new MockEnvironment());
        // seguro ad valorem sobre o que passa de R$ 200,00: 1% (economica) e 2% (expressa)
        assertThat(e.cotar(contexto("SP","0","200",1)).custo()).isEqualTo(Dinheiro.de("25"));
        assertThat(e.cotar(contexto("SP","0","700",1)).custo()).isEqualTo(Dinheiro.de("30"));
        assertThat(e.cotar(contexto("SP","0","1200",1)).custo()).isEqualTo(Dinheiro.de("35"));
        assertThat(x.cotar(contexto("SP","0","700",1)).custo()).isEqualTo(Dinheiro.de("50"));
        // HALF_EVEN: 0,005 -> 0,00 e 0,015 -> 0,02 (arredonda uma unica vez, no total)
        assertThat(e.cotar(contexto("SP","0","200.50",1)).custo()).isEqualTo(Dinheiro.de("25.00"));
        assertThat(e.cotar(contexto("SP","0","201.50",1)).custo()).isEqualTo(Dinheiro.de("25.02"));
        assertThat(e.cotar(contexto("SP","0","1200",1)).custo()).isGreaterThan(e.cotar(contexto("SP","0","700",1)).custo());
    }
    @Test void fatoresSomamEModalidadesDiferemParaOMesmoContexto(){
        var e=new AdaptadorEntregaEconomica(new MockEnvironment());var x=new AdaptadorEntregaExpressa(new MockEnvironment());
        var c=contexto("BA","4","600",8); // nordeste: base 40/64
        // economica: 40 + 3*2 + 3*1 + 4 = 53; expressa: 64 + 3*4 + 3*2 + 8 = 90
        assertThat(e.cotar(c).custo()).isEqualTo(Dinheiro.de("53"));
        assertThat(x.cotar(c).custo()).isEqualTo(Dinheiro.de("90"));
        assertThat(x.cotar(c).custo()).isGreaterThan(e.cotar(c).custo());
        assertThat(e.cotar(contexto("BA","4","600",8)).custo()).isGreaterThan(e.cotar(contexto("SP","4","600",8)).custo());
    }
    @Test void coeficientesSaoConfiguraveisEValidados(){
        var env=new MockEnvironment().withProperty("ecommerce.entrega.economica.franquia-kg","2")
                .withProperty("ecommerce.entrega.economica.adicional-kg","3").withProperty("ecommerce.entrega.economica.franquia-itens","1")
                .withProperty("ecommerce.entrega.economica.adicional-item","0.5").withProperty("ecommerce.entrega.economica.franquia-valor","100")
                .withProperty("ecommerce.entrega.economica.seguro-percentual","0.5");
        // 25 + (4-2)*3 + (3-1)*0.5 + (300-100)*0.5% = 25 + 6 + 1 + 1 = 33
        assertThat(new AdaptadorEntregaEconomica(env).cotar(contexto("SP","4","300",3)).custo()).isEqualTo(Dinheiro.de("33"));
        assertThatThrownBy(()->new AdaptadorEntregaEconomica(new MockEnvironment().withProperty("ecommerce.entrega.economica.adicional-kg","-1")))
                .isInstanceOf(IllegalArgumentException.class);
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
