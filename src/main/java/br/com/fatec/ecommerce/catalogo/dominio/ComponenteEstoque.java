package br.com.fatec.ecommerce.catalogo.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.DadoInvalidoException;

/**
 * Parcela de estoque que uma venda consome: a variacao e a quantidade. E o resultado de
 * {@link ItemVendavel#componentesDeEstoque(int)} e o contrato que o modulo de estoque usa para reservar.
 */
public record ComponenteEstoque(Long variacaoId, int quantidade) {

    public ComponenteEstoque {
        if (quantidade <= 0) {
            throw new DadoInvalidoException("A quantidade de um componente de estoque deve ser maior que zero.");
        }
    }
}
