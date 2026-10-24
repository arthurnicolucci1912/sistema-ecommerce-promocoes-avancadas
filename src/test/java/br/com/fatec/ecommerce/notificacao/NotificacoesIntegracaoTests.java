package br.com.fatec.ecommerce.notificacao;

import static org.assertj.core.api.Assertions.*;
import br.com.fatec.ecommerce.TesteDeIntegracao;
import br.com.fatec.ecommerce.cliente.ClientesParaTeste;
import br.com.fatec.ecommerce.compartilhado.dominio.PublicadorEventos;
import br.com.fatec.ecommerce.notificacao.dominio.*;
import br.com.fatec.ecommerce.pedido.dominio.*;
import br.com.fatec.ecommerce.pedido.dominio.evento.*;
import br.com.fatec.ecommerce.seguranca.UsuariosParaTeste;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@TesteDeIntegracao
@Import(NotificacoesIntegracaoTests.Extensao.class)
class NotificacoesIntegracaoTests {
    @Autowired PublicadorEventos eventos;
    @Autowired PlatformTransactionManager transacoes;
    @Autowired UsuariosParaTeste usuarios;
    @Autowired ClientesParaTeste clientes;
    @Autowired CanalAdicional canal;
    @Autowired InteressadoAdicional interessado;
    long clienteId;

    @TestConfiguration static class Extensao {
        @Bean CanalAdicional canalAdicional() { return new CanalAdicional(); }
        @Bean InteressadoAdicional interessadoAdicional() { return new InteressadoAdicional(); }
    }
    static class CanalAdicional implements CanalNotificacao {
        final List<Notificacao> mensagens = new ArrayList<>();
        public String canal() { return "adicional"; }
        public void enviar(Notificacao notificacao) { mensagens.add(notificacao); }
    }
    static class InteressadoAdicional {
        final List<PedidoCancelado> cancelamentos = new ArrayList<>();
        @EventListener public void ao(PedidoCancelado evento) { cancelamentos.add(evento); }
    }

    @BeforeEach void preparar() {
        usuarios.limpar(); canal.mensagens.clear(); interessado.cancelamentos.clear();
        clienteId = clientes.criar("destino@teste.local", 1801).cliente().getId();
        clientes.criar("outro@teste.local", 1802);
    }

    PedidoCriado criado() {
        return new PedidoCriado(987L, clienteId, SituacaoPedido.CRIADO, SituacaoPedido.AGUARDANDO_PAGAMENTO, Instant.now());
    }

    @Test void todosOsAcontecimentosChegamAoNovoCanalApenasDepoisDoCommit() {
        new TransactionTemplate(transacoes).executeWithoutResult(tx -> {
            eventos.publicar(criado());
            eventos.publicar(new PedidoPago(987L, clienteId, SituacaoPedido.AGUARDANDO_PAGAMENTO, SituacaoPedido.PAGO, Instant.now()));
            eventos.publicar(new PedidoPagamentoRecusado(988L, clienteId, SituacaoPedido.AGUARDANDO_PAGAMENTO, SituacaoPedido.PAGAMENTO_RECUSADO, Instant.now()));
            eventos.publicar(new PedidoEnviado(987L, clienteId, SituacaoPedido.EM_PREPARACAO, SituacaoPedido.ENVIADO, Instant.now()));
            eventos.publicar(new PedidoCancelado(989L, clienteId, SituacaoPedido.CRIADO, SituacaoPedido.CANCELADO, Instant.now(), Responsavel.sistema(), "Teste"));
            assertThat(canal.mensagens).isEmpty();
        });
        assertThat(canal.mensagens).extracting(Notificacao::assunto).containsExactly(
                "Pedido 987: AGUARDANDO_PAGAMENTO", "Pedido 987: PAGO", "Pedido 988: PAGAMENTO_RECUSADO",
                "Pedido 987: ENVIADO", "Pedido 989: CANCELADO");
        assertThat(canal.mensagens).extracting(Notificacao::destinatario).containsOnly("destino@teste.local");
        assertThat(interessado.cancelamentos).singleElement().satisfies(e -> assertThat(e.de()).isEqualTo(SituacaoPedido.CRIADO));
    }

    @Test void rollbackNaoEnviaENaoHaFallbackForaDaTransacao() {
        new TransactionTemplate(transacoes).executeWithoutResult(tx -> {
            eventos.publicar(criado());
            tx.setRollbackOnly();
        });
        eventos.publicar(criado());
        assertThat(canal.mensagens).isEmpty();
    }
}
