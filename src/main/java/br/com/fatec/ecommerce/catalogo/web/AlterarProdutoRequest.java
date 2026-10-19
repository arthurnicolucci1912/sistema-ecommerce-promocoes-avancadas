package br.com.fatec.ecommerce.catalogo.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados cadastrais de um produto (as variações têm rotas próprias).")
public record AlterarProdutoRequest(
        @NotBlank(message = "o nome é obrigatório") @Size(max = 200, message = "no máximo 200 caracteres") String nome,
        String descricao,
        @Size(max = 100, message = "no máximo 100 caracteres") String marca,
        @NotNull(message = "a categoria é obrigatória") Long categoriaId) {
}
