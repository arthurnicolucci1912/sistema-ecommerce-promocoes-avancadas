package br.com.fatec.ecommerce.pedido.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Motivo do cancelamento do pedido (obrigatório).")
public record MotivoRequest(
        @NotBlank(message = "o motivo é obrigatório") @Size(max = 500, message = "no máximo 500 caracteres") String motivo) {
}
