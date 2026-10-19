package br.com.fatec.ecommerce.catalogo.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(description = "Novo preço de uma variação.")
public record AlterarPrecoRequest(
        @NotNull(message = "o preço é obrigatório")
        @DecimalMin(value = "0.00", inclusive = false, message = "o preço deve ser maior que zero")
        @Digits(integer = 10, fraction = 2, message = "o preço aceita até 2 casas decimais") BigDecimal preco) {
}
