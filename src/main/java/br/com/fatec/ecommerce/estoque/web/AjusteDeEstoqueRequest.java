package br.com.fatec.ecommerce.estoque.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Ajuste do disponível: quantidade positiva soma, negativa subtrai (nunca zero).")
public record AjusteDeEstoqueRequest(
        @NotNull(message = "a quantidade é obrigatória") Integer quantidade,
        @NotBlank(message = "o motivo é obrigatório") @Size(max = 255, message = "no máximo 255 caracteres") String motivo) {
}
