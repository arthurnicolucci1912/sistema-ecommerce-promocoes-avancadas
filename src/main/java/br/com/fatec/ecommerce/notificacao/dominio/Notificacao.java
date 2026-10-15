package br.com.fatec.ecommerce.notificacao.dominio;

/**
 * Mensagem a ser enviada a um cliente por algum canal.
 *
 * <p>Contrato provisorio (onda 0): mudancas via PR revisado pelos consumidores e registradas em docs/03-arquitetura.md.
 *
 * @param destinatario endereco do destinatario no canal (e-mail, telefone etc.)
 * @param assunto      assunto da mensagem
 * @param mensagem     corpo da mensagem
 */
public record Notificacao(String destinatario, String assunto, String mensagem) {
}
