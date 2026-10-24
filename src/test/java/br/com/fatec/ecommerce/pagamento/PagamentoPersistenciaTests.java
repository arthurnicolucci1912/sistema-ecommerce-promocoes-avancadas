package br.com.fatec.ecommerce.pagamento;

import static org.assertj.core.api.Assertions.*;
import br.com.fatec.ecommerce.TesteDeIntegracao;
import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import br.com.fatec.ecommerce.pagamento.dominio.*;
import br.com.fatec.ecommerce.seguranca.UsuariosParaTeste;
import java.time.Instant;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@TesteDeIntegracao
class PagamentoPersistenciaTests {
    @Autowired PagamentosRepository repo;
    @Autowired UsuariosParaTeste usuarios;
    @Autowired PlatformTransactionManager manager;
    final Instant agora=Instant.parse("2026-10-22T12:00:00Z");
    @BeforeEach void limpar(){usuarios.limpar();}
    Cobranca cobranca(long pedido,String tx){return new Cobranca(pedido,"pix",FormaPagamento.PIX,Dinheiro.de("90"),tx,ResultadoCobranca.Situacao.PENDENTE,agora,agora);}
    @Test void identidadeUnicaPorPedidoETransacao(){
        repo.criar(cobranca(1,"TX"));
        assertThat(repo.buscarPorPedido(1L)).contains(cobranca(1,"TX"));
        assertThat(repo.buscarPorTransacao("TX")).contains(cobranca(1,"TX"));
        assertThatThrownBy(()->repo.criar(cobranca(1,"OUTRA"))).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(()->repo.criar(cobranca(2,"TX"))).isInstanceOf(DataIntegrityViolationException.class);
    }
    @Test void duplicataNaoInvalidaTransacaoESucessoDoEstornoEUnico(){
        new TransactionTemplate(manager).executeWithoutResult(tx->{
            repo.criar(cobranca(1,"TX"));
            var c=new ConfirmacaoPagamento("TX",ResultadoCobranca.Situacao.APROVADO,agora);
            assertThat(repo.registrar(c)).isTrue();
            assertThat(repo.registrar(c)).isFalse();
            repo.atualizar(cobranca(1,"TX").confirmar(c.resultado(),agora));
            repo.salvar(new Estorno(1L,"TX",Dinheiro.de("90"),Estorno.Situacao.CONCLUIDO,"REF",1,null,agora));
        });
        assertThat(repo.buscarConfirmacao("TX")).isPresent();
        assertThat(repo.buscarPorPedido(1L).orElseThrow().situacao()).isEqualTo(ResultadoCobranca.Situacao.APROVADO);
        assertThat(repo.buscarEstorno(1L).orElseThrow().idTransacaoExterna()).isEqualTo("REF");
        assertThatThrownBy(()->repo.salvar(new Estorno(2L,"TX",Dinheiro.de("90"),Estorno.Situacao.CONCLUIDO,"REF-2",1,null,agora))).isInstanceOf(DataIntegrityViolationException.class);
    }
}
