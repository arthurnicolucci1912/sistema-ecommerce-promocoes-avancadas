package br.com.fatec.ecommerce.notificacao.infra.integracao;

import br.com.fatec.ecommerce.notificacao.dominio.CanalNotificacao;
import br.com.fatec.ecommerce.notificacao.dominio.Notificacao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Strategy/Adapter: canal de e-mail simulado; nao transmite dados a servicos externos. */
@Component
public class CanalNotificacaoSimulado implements CanalNotificacao {
    private static final Logger LOG = LoggerFactory.getLogger(CanalNotificacaoSimulado.class);

    @Override
    public String canal() {
        return "email-simulado";
    }

    @Override
    public void enviar(Notificacao notificacao) {
        // O contexto comercial e registrado pelo emissor; nao registrar destinatario nem corpo.
        LOG.info("Envio simulado concluido canal=email-simulado");
    }
}
