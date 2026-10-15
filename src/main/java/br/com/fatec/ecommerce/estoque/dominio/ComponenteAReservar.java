package br.com.fatec.ecommerce.estoque.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.DadoInvalidoException;

/**
 * Parcela de estoque a reservar: a variacao e a quantidade. O estoque define o proprio tipo (nao depende do
 * catalogo); quem reserva traduz os componentes do item vendavel para este contrato.
 */
public record ComponenteAReservar(Long variacaoId, int quantidade) {

    public ComponenteAReservar {
        if (variacaoId == null) {
            throw new DadoInvalidoException("A variação do componente a reservar é obrigatória.");
        }
        if (quantidade <= 0) {
            throw new DadoInvalidoException("A quantidade a reservar deve ser maior que zero.");
        }
    }
}
