package br.com.fatec.ecommerce.notificacao.aplicacao;

import br.com.fatec.ecommerce.notificacao.dominio.CanalNotificacao;
import br.com.fatec.ecommerce.notificacao.dominio.DestinatariosNotificacao;
import br.com.fatec.ecommerce.notificacao.dominio.Notificacao;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Emissor dos canais Strategy/Adapter: um novo bean participa sem editar os canais existentes.
 * Falhas de cada canal sao isoladas; dados pessoais e mensagens arbitrarias de excecao nao vao ao log.
 */
@Service
public class EnviarNotificacoes {
    private static final Logger LOG = LoggerFactory.getLogger(EnviarNotificacoes.class);
    private final DestinatariosNotificacao destinatarios;
    private final Map<String, CanalNotificacao> canais;

    public EnviarNotificacoes(DestinatariosNotificacao destinatarios, List<CanalNotificacao> canais) {
        this.destinatarios = destinatarios;
        if (canais == null || canais.isEmpty()) {
            throw new IllegalArgumentException("Ao menos um canal de notificação é obrigatório.");
        }
        var registrados = new LinkedHashMap<String, CanalNotificacao>();
        for (var canal : canais) {
            String nome = canal == null ? null : canal.canal();
            if (nome == null || !nome.matches("[a-z0-9][a-z0-9-]{0,59}")
                    || registrados.putIfAbsent(nome, canal) != null) {
                throw new IllegalArgumentException("Canal de notificação inválido ou duplicado.");
            }
        }
        this.canais = java.util.Collections.unmodifiableMap(registrados);
    }

    public void enviar(Long pedidoId, Long clienteId, String acontecimento) {
        String email = destinatarios.emailDoCliente(clienteId)
                .filter(valor -> !valor.isBlank())
                .orElseThrow(() -> new IllegalStateException("Destinatário não encontrado."));
        var notificacao = new Notificacao(email, "Pedido " + pedidoId + ": " + acontecimento,
                "O pedido " + pedidoId + " está em " + acontecimento + ".");
        canais.forEach((nome, canal) -> {
            try {
                canal.enviar(notificacao);
                LOG.info("Notificacao enviada pedido={} acontecimento={} canal={}", pedidoId, acontecimento, nome);
            } catch (RuntimeException erro) {
                LOG.warn("Falha de notificacao pedido={} acontecimento={} canal={} categoria={}",
                        pedidoId, acontecimento, nome, erro.getClass().getSimpleName());
            }
        });
    }
}
