package br.com.fatec.ecommerce.catalogo.dominio.evento;

import br.com.fatec.ecommerce.compartilhado.dominio.EventoDominio;
import java.time.Instant;

/**
 * Uma variacao de produto foi cadastrada no catalogo.
 *
 * <p>Pattern: Observer - papel: evento; o catalogo o publica sem conhecer quem reage (o estoque cria o saldo
 * inicial da variacao, sem consultar o catalogo).
 *
 * <p>Ver docs/04-design-patterns.md, secao 3.6 (Observer: eventos de dominio).
 */
public record VariacaoCadastrada(Long variacaoId, String sku, Instant ocorridoEm) implements EventoDominio {
}
