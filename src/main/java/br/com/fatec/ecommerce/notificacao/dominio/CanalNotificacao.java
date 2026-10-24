package br.com.fatec.ecommerce.notificacao.dominio;

/**
 * Contrato de um canal de comunicacao com o cliente (e-mail, SMS, WhatsApp...).
 *
 * <p>Pattern: Strategy/Adapter de canal - papel: interface da estrategia; cada canal e uma implementacao
 * intercambiavel que adapta a tecnologia de envio.
 *
 * <p>Problema do enunciado: novos canais sem forte dependencia entre o modulo de pedidos e a tecnologia de
 * notificacao. O modulo {@code pedido} nao conhece nenhum canal; {@code notificacao} reage a eventos e envia
 * por todos os canais ativos.
 *
 * <p>Implementacao E8: {@code CanalNotificacaoSimulado}; {@code EnviarNotificacoes} distribui a mensagem aos
 * beans registrados e isola falhas por canal. Os ouvintes executam apenas apos commit.
 *
 * <p>Contrato provisorio (onda 0): mudancas via PR revisado pelos consumidores e registradas em docs/03-arquitetura.md.
 *
 * <p>Ver docs/04-design-patterns.md, secao 3.6 (Observer: eventos de dominio e canais de notificacao).
 */
public interface CanalNotificacao {

    String canal();

    void enviar(Notificacao notificacao);
}
