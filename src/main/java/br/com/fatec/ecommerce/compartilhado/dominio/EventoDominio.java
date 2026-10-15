package br.com.fatec.ecommerce.compartilhado.dominio;

import java.time.Instant;

/**
 * Fato relevante ocorrido no dominio (pedido criado, pagamento confirmado...), publicado via {@link PublicadorEventos}.
 *
 * <p>Pattern: Observer - papel: evento; quem o publica nao conhece quem reage.
 *
 * <p>Problema do enunciado: o sistema deve evitar que uma unica classe conheca diretamente todos os
 * componentes interessados em, por exemplo, um pagamento confirmado.
 *
 * <p>Eventos concretos e ouvintes: etapa E8 (e publicadores nas etapas E4, E6, E7). Nao ha evento concreto na
 * fundacao.
 *
 * <p>Contrato provisorio (onda 0): mudancas via PR revisado pelos consumidores e registradas em docs/03-arquitetura.md.
 *
 * <p>Ver docs/04-design-patterns.md, secao 3.6 (Observer: eventos de dominio).
 */
public interface EventoDominio {

    Instant ocorridoEm();
}
