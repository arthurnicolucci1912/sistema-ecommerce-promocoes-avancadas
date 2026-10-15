package br.com.fatec.ecommerce.compartilhado.dominio;

/**
 * Porta pela qual o dominio e a aplicacao publicam {@link EventoDominio}, sem depender do mecanismo de entrega.
 *
 * <p>Pattern: Observer - papel: sujeito desacoplado dos ouvintes; os observadores se registram no mecanismo
 * de eventos e o publicador apenas anuncia o acontecimento.
 *
 * <p>Problema do enunciado: ver {@link EventoDominio}. Esta interface mantem o dominio livre do Spring; o
 * adaptador sobre o {@code ApplicationEventPublisher} fica em {@code compartilhado.infra}.
 *
 * <p>Implementacao: {@code PublicadorEventosSpring} (fundacao); ouvintes: etapa E8 e demais donos.
 *
 * <p>Contrato provisorio (onda 0): mudancas via PR revisado pelos consumidores e registradas em docs/03-arquitetura.md.
 *
 * <p>Ver docs/04-design-patterns.md, secao 3.6 (Observer: eventos de dominio).
 */
public interface PublicadorEventos {

    void publicar(EventoDominio evento);
}
