package br.com.fatec.ecommerce.pagamento;

import static org.assertj.core.api.Assertions.*;
import br.com.fatec.ecommerce.compartilhado.dominio.*;
import br.com.fatec.ecommerce.pagamento.dominio.*;
import br.com.fatec.ecommerce.pagamento.infra.gateway.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

class PagamentoDominioTests {
    final Instant agora = Instant.parse("2026-10-22T12:00:00Z");
    Cobranca cobranca() { return new Cobranca(1L,"pix",FormaPagamento.PIX,Dinheiro.de("90"),"TX-1",ResultadoCobranca.Situacao.PENDENTE,agora,agora); }
    @Test void valorPreservadoEConfirmacaoTerminal() {
        var aprovada = cobranca().confirmar(ResultadoCobranca.Situacao.APROVADO,agora);
        assertThat(aprovada.total()).isEqualTo(Dinheiro.de("90.00"));
        assertThat(aprovada.confirmar(ResultadoCobranca.Situacao.APROVADO,agora)).isEqualTo(aprovada);
        assertThatThrownBy(() -> aprovada.confirmar(ResultadoCobranca.Situacao.RECUSADO,agora)).isInstanceOf(ConflitoException.class);
        assertThatThrownBy(() -> cobranca().confirmar(ResultadoCobranca.Situacao.PENDENTE,agora)).isInstanceOf(DadoInvalidoException.class);
    }
    @Test void dadosInvalidosNaoFormamCobrancaOuConfirmacao() {
        assertThatThrownBy(() -> new Cobranca(0L,"pix",FormaPagamento.PIX,Dinheiro.de("90"),"TX",ResultadoCobranca.Situacao.PENDENTE,agora,agora)).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> new Cobranca(1L,"pix",FormaPagamento.PIX,Dinheiro.de("-1"),"TX",ResultadoCobranca.Situacao.PENDENTE,agora,agora)).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> new ConfirmacaoPagamento(" ",ResultadoCobranca.Situacao.APROVADO,agora)).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> new Cobranca(1L,"pix",FormaPagamento.PIX,Dinheiro.zero(),"TX",ResultadoCobranca.Situacao.PENDENTE,agora,agora.minusSeconds(1))).isInstanceOf(DadoInvalidoException.class);
        assertThat(new Cobranca(1L,"pix",FormaPagamento.PIX,Dinheiro.zero(),"TX",ResultadoCobranca.Situacao.PENDENTE,agora,agora).total()).isEqualTo(Dinheiro.zero());
    }
    @Test void falhaDeEstornoRecuperadaESucessoFinalNaoRepeteTentativa() {
        var pendente = new Estorno(1L,"TX",Dinheiro.de("90"),Estorno.Situacao.PENDENTE,null,0,null,agora);
        var falhou = pendente.registrar(new ResultadoEstorno(false,null),null,agora);
        assertThat(falhou.situacao()).isEqualTo(Estorno.Situacao.FALHOU);
        var sucesso = falhou.registrar(new ResultadoEstorno(true,"REF-TX"),null,agora);
        assertThat(sucesso.tentativas()).isEqualTo(2);
        assertThat(sucesso.registrar(null,"erro",agora)).isSameAs(sucesso);
        assertThatThrownBy(() -> new Estorno(1L,null,Dinheiro.de("90"),Estorno.Situacao.CONCLUIDO,"REF",1,null,agora)).isInstanceOf(DadoInvalidoException.class);
    }
    @Test void adaptersTraduzemTresApisEUsamChavesEstaveis() {
        var cenarios = new CenariosPagamento(true,false);
        List<GatewayPagamento> gateways=List.of(new AdaptadorCartaoSimulado(cenarios),new AdaptadorPixSimulado(cenarios),new AdaptadorBoletoSimulado(cenarios));
        for(var g:gateways) {
            var forma=Arrays.stream(FormaPagamento.values()).filter(g::suporta).findFirst().orElseThrow();
            var pedido=new SolicitacaoCobranca(37L,forma,Dinheiro.de("12.34"));
            var r=g.cobrar(pedido);
            assertThat(g.cobrar(pedido)).isEqualTo(r);
            assertThat(r.situacao()).isEqualTo(forma==FormaPagamento.CARTAO_CREDITO?ResultadoCobranca.Situacao.APROVADO:ResultadoCobranca.Situacao.PENDENTE);
            var estorno=new SolicitacaoEstorno(r.idTransacaoExterna(),pedido.valor());
            assertThat(g.estornar(estorno).estornado()).isTrue();
            assertThat(g.estornar(estorno)).isEqualTo(g.estornar(estorno));
        }
        assertThat(new AdaptadorCartaoSimulado(new CenariosPagamento(false,true)).cobrar(new SolicitacaoCobranca(1L,FormaPagamento.CARTAO_CREDITO,Dinheiro.de("1"))).situacao()).isEqualTo(ResultadoCobranca.Situacao.RECUSADO);
        for(var g:List.of(new AdaptadorCartaoSimulado(new CenariosPagamento(true,true)),new AdaptadorPixSimulado(new CenariosPagamento(true,true)),new AdaptadorBoletoSimulado(new CenariosPagamento(true,true))))
            assertThat(g.estornar(new SolicitacaoEstorno("TX",Dinheiro.de("1"))).estornado()).isFalse();
    }
    @Test void novoFornecedorRegistradoSubstituiCartaoEPreservaOriginal() {
        var c = new CenariosPagamento(true,false);
        GatewayPagamento novo=new GatewayPagamento() {
            public String fornecedor(){return "cartao-novo";}
            public boolean suporta(FormaPagamento f){return f==FormaPagamento.CARTAO_CREDITO;}
            public ResultadoCobranca cobrar(SolicitacaoCobranca s){return new ResultadoCobranca(ResultadoCobranca.Situacao.APROVADO,"NOVO-"+s.pedidoId());}
            public ResultadoEstorno estornar(SolicitacaoEstorno s){return new ResultadoEstorno(true,"REF-"+s.idTransacaoExterna());}
        };
        var original=new AdaptadorCartaoSimulado(c);
        List<GatewayPagamento> todos=List.of(original,novo,new AdaptadorPixSimulado(c),new AdaptadorBoletoSimulado(c));
        assertThatThrownBy(() -> new RegistroGateways(todos,Map.of())).isInstanceOf(IllegalArgumentException.class);
        var registro=new RegistroGateways(todos,Map.of(FormaPagamento.CARTAO_CREDITO,"cartao-novo"));
        assertThat(registro.para(FormaPagamento.CARTAO_CREDITO)).isSameAs(novo);
        assertThat(registro.fornecedor("cartao-simulado")).isSameAs(original);
        assertThat(registro.para(FormaPagamento.CARTAO_CREDITO).cobrar(new SolicitacaoCobranca(1L,FormaPagamento.CARTAO_CREDITO,Dinheiro.de("1"))).idTransacaoExterna()).isEqualTo("NOVO-1");
        assertThatThrownBy(() -> new RegistroGateways(todos,Map.of(FormaPagamento.CARTAO_CREDITO,"pix-simulado"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RegistroGateways(List.of(original,original),Map.of())).isInstanceOf(IllegalArgumentException.class);
    }
}
