package br.com.fatec.ecommerce.notificacao;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.com.fatec.ecommerce.notificacao.aplicacao.EnviarNotificacoes;
import br.com.fatec.ecommerce.notificacao.aplicacao.OuvinteDePedidosDaNotificacao;
import br.com.fatec.ecommerce.notificacao.dominio.*;
import br.com.fatec.ecommerce.notificacao.infra.integracao.CanalNotificacaoSimulado;
import br.com.fatec.ecommerce.pedido.dominio.SituacaoPedido;
import br.com.fatec.ecommerce.pedido.dominio.evento.PedidoPago;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class EnviarNotificacoesTests {
    @Test void registroRejeitaAusentesDuplicadosENomesInvalidos() {
        DestinatariosNotificacao destinatarios = id -> Optional.of("cliente@teste.local");
        assertThatIllegalArgumentException().isThrownBy(() -> new EnviarNotificacoes(destinatarios, List.of()));
        var canal = new CanalNotificacaoSimulado();
        assertThatIllegalArgumentException().isThrownBy(() -> new EnviarNotificacoes(destinatarios, List.of(canal, canal)));
        for (String nome : Arrays.asList(null, "", " ", "canal\nforjado", "x".repeat(61))) {
            var invalido = mock(CanalNotificacao.class);
            when(invalido.canal()).thenReturn(nome);
            assertThatIllegalArgumentException().isThrownBy(() -> new EnviarNotificacoes(destinatarios, List.of(invalido)));
        }
        assertThatIllegalArgumentException().isThrownBy(() -> new EnviarNotificacoes(destinatarios, Arrays.asList((CanalNotificacao) null)));
    }

    @Test void novoCanalRecebeMensagemMesmoSeOutroFalharSemVazarDados(CapturedOutput output) {
        var falho = mock(CanalNotificacao.class);
        when(falho.canal()).thenReturn("falho");
        doThrow(new IllegalStateException("senha-token-sentinela cliente@teste.local")).when(falho).enviar(any());
        var novo = mock(CanalNotificacao.class);
        when(novo.canal()).thenReturn("novo");
        var emissor = new EnviarNotificacoes(id -> Optional.of("cliente@teste.local"),
                List.of(falho, new CanalNotificacaoSimulado(), novo));
        emissor.enviar(42L, 7L, "PAGO");
        verify(novo).enviar(new Notificacao("cliente@teste.local", "Pedido 42: PAGO", "O pedido 42 está em PAGO."));
        assertThat(output.getOut()).contains("pedido=42", "acontecimento=PAGO", "canal=falho", "categoria=IllegalStateException")
                .doesNotContain("senha-token-sentinela", "cliente@teste.local", "O pedido 42 está");
    }

    @Test void destinatarioAusenteOuFalhaDeLeituraNaoEscapaDoOuvinte(CapturedOutput output) {
        var canal = mock(CanalNotificacao.class);
        when(canal.canal()).thenReturn("teste");
        var evento = new PedidoPago(42L, 7L, SituacaoPedido.AGUARDANDO_PAGAMENTO, SituacaoPedido.PAGO, Instant.now());
        for (DestinatariosNotificacao fonte : List.<DestinatariosNotificacao>of(
                id -> Optional.empty(), id -> Optional.of(" "),
                id -> { throw new IllegalStateException("segredo-do-banco"); })) {
            var ouvinte = new OuvinteDePedidosDaNotificacao(new EnviarNotificacoes(fonte, List.of(canal)));
            assertThatCode(() -> ouvinte.ao(evento)).doesNotThrowAnyException();
        }
        verify(canal, never()).enviar(any());
        assertThat(output.getOut()).contains("Falha de preparacao", "pedido=42", "acontecimento=PAGO")
                .doesNotContain("segredo-do-banco");
    }
}
