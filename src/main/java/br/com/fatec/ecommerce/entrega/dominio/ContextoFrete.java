package br.com.fatec.ecommerce.entrega.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import java.math.BigDecimal;

/**
 * Dados de que um provedor precisa para atender e cotar um frete.
 *
 * <p>Contrato provisorio (onda 0): mudancas via PR revisado pelos consumidores e registradas em docs/03-arquitetura.md.
 *
 * @param uf       unidade federativa (regiao) de destino
 * @param pesoKg   peso total da entrega, em quilogramas
 * @param subtotal valor dos itens
 */
public record ContextoFrete(String uf, BigDecimal pesoKg, Dinheiro subtotal) {
}
