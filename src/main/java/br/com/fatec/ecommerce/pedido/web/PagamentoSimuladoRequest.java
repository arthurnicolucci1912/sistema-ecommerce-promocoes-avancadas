package br.com.fatec.ecommerce.pedido.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Resultado simulado do pagamento: true aprova, false recusa.")
public record PagamentoSimuladoRequest(@NotNull(message = "informe se o pagamento foi aprovado") Boolean aprovado) {
}
