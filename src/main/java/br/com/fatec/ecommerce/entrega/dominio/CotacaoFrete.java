package br.com.fatec.ecommerce.entrega.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;

/**
 * Cotacao de frete devolvida por um provedor.
 *
 * <p>Contrato provisorio (onda 0): mudancas via PR revisado pelos consumidores e registradas em docs/03-arquitetura.md.
 *
 * @param modalidade modalidade de entrega cotada
 * @param custo      custo do frete
 * @param prazoDias  prazo de entrega em dias
 */
public record CotacaoFrete(String modalidade, Dinheiro custo, int prazoDias) {
}
