package br.com.fatec.ecommerce.catalogo.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "Produto com suas variações (ao menos uma).")
public record CadastrarProdutoRequest(
        @NotBlank(message = "o nome é obrigatório") @Size(max = 200, message = "no máximo 200 caracteres") String nome,
        String descricao,
        @Size(max = 100, message = "no máximo 100 caracteres") String marca,
        @NotNull(message = "a categoria é obrigatória") Long categoriaId,
        @NotEmpty(message = "informe ao menos uma variação") List<@Valid @NotNull VariacaoRequest> variacoes) {
}
