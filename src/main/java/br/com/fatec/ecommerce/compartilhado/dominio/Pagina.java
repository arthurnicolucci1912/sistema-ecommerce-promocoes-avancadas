package br.com.fatec.ecommerce.compartilhado.dominio;

import java.util.List;

/**
 * Pagina de um resultado de consulta, sem depender de framework: o conteudo da pagina (base 0), o tamanho pedido
 * e o total de elementos da consulta inteira.
 */
public record Pagina<T>(List<T> conteudo, int pagina, int tamanho, long totalElementos) {

    public Pagina {
        conteudo = List.copyOf(conteudo);
    }

    public int totalPaginas() {
        return tamanho == 0 ? 0 : (int) ((totalElementos + tamanho - 1) / tamanho);
    }
}
