package br.com.fatec.ecommerce.catalogo.aplicacao;

import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;

/**
 * Filtros opcionais (nulos) da listagem publica de produtos: categoria (com todas as descendentes), trecho do nome
 * (sem diferenciar maiusculas) e faixa de preco sobre as variacoes ativas (extremos incluidos).
 */
public record FiltroDeProdutos(Long categoriaId, String nome, Dinheiro precoMinimo, Dinheiro precoMaximo) {

    public static FiltroDeProdutos semFiltro() {
        return new FiltroDeProdutos(null, null, null, null);
    }
}
