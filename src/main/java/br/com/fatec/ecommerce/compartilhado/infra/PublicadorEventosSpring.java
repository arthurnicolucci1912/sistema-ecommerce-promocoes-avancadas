package br.com.fatec.ecommerce.compartilhado.infra;

import br.com.fatec.ecommerce.compartilhado.dominio.EventoDominio;
import br.com.fatec.ecommerce.compartilhado.dominio.PublicadorEventos;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Adapter do mecanismo de eventos do Spring ({@link ApplicationEventPublisher}) para a interface
 * {@link PublicadorEventos} do dominio. Os ouvintes sao metodos {@code @EventListener} ou
 * {@code @TransactionalEventListener} que recebem o proprio {@link EventoDominio} publicado.
 *
 * <p>Pattern: Observer (infraestrutura) - ver docs/04-design-patterns.md, secao 3.6.
 */
@Component
public class PublicadorEventosSpring implements PublicadorEventos {

    private final ApplicationEventPublisher publicador;

    public PublicadorEventosSpring(ApplicationEventPublisher publicador) {
        this.publicador = publicador;
    }

    @Override
    public void publicar(EventoDominio evento) {
        publicador.publishEvent(evento);
    }
}
