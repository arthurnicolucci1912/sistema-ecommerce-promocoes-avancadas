package br.com.fatec.ecommerce.compartilhado.infra;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.fatec.ecommerce.TesteDeIntegracao;
import br.com.fatec.ecommerce.compartilhado.dominio.EventoDominio;
import br.com.fatec.ecommerce.compartilhado.dominio.PublicadorEventos;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;

/** Um evento de dominio publicado pela interface do dominio chega a um ouvinte do Spring. */
@TesteDeIntegracao
@Import(PublicadorEventosSpringTests.OuvinteDeTeste.class)
class PublicadorEventosSpringTests {

    record EventoDeTeste(Instant ocorridoEm, String conteudo) implements EventoDominio {
    }

    @TestConfiguration
    static class OuvinteDeTeste {
        final List<EventoDeTeste> recebidos = new ArrayList<>();

        @EventListener
        void ao(EventoDeTeste evento) {
            recebidos.add(evento);
        }
    }

    @Autowired
    PublicadorEventos publicador;

    @Autowired
    OuvinteDeTeste ouvinte;

    @Test
    void eventoPublicadoChegaAoOuvinte() {
        EventoDeTeste evento = new EventoDeTeste(Instant.now(), "ola");

        publicador.publicar(evento);

        assertThat(ouvinte.recebidos).containsExactly(evento);
    }

    @Test
    void implementacaoDoSpringEhOPublicadorInjetado() {
        assertThat(publicador).isInstanceOf(PublicadorEventosSpring.class);
    }
}
