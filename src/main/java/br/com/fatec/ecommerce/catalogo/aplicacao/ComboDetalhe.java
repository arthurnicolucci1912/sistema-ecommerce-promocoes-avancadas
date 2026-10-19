package br.com.fatec.ecommerce.catalogo.aplicacao;

import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import java.util.List;

/**
 * Visao publica de um combo disponivel para venda, com os componentes diretos e a disponibilidade: quantos combos
 * completos o estoque atual permite montar.
 */
public record ComboDetalhe(Long id, String sku, String nome, Dinheiro preco, int disponibilidade,
        List<Componente> componentes) {

    public ComboDetalhe {
        componentes = List.copyOf(componentes);
    }

    public record Componente(Long itemId, String sku, String nome, int quantidade) {
    }
}
