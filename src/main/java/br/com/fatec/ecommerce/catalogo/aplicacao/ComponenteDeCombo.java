package br.com.fatec.ecommerce.catalogo.aplicacao;

/** Componente de um combo na entrada: o SKU de uma variacao ou de outro combo e a quantidade. */
public record ComponenteDeCombo(String sku, int quantidade) {
}
