package br.com.fatec.ecommerce.catalogo.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados de uma categoria: nome e, opcionalmente, a categoria pai (vazio = categoria raiz).")
public record CategoriaRequest(
        @NotBlank(message = "o nome é obrigatório") @Size(max = 100, message = "no máximo 100 caracteres") String nome,
        Long paiId) {
}
