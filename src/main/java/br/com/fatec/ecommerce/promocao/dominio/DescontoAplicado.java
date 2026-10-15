package br.com.fatec.ecommerce.promocao.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;

/**
 * Desconto que a {@link PoliticaConflito} decidiu aplicar; compoe o detalhamento do preco.
 *
 * <p>Contrato provisorio (onda 0): mudancas via PR revisado pelos consumidores e registradas em docs/03-arquitetura.md.
 *
 * @param promocaoId  promocao de origem
 * @param nome        nome da promocao
 * @param valor       valor efetivamente abatido
 * @param alvo        onde incidiu (itens ou frete)
 */
public record DescontoAplicado(Long promocaoId, String nome, Dinheiro valor, AlvoDesconto alvo) {
}
