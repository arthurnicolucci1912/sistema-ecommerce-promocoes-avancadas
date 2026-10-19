package br.com.fatec.ecommerce.catalogo.aplicacao;

import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import java.util.Map;

/** Dados de uma variacao a cadastrar: SKU, preco e atributos nome/valor (livres, sem cadastro previo). */
public record NovaVariacao(String sku, Dinheiro preco, Map<String, String> atributos) {
}
