package br.com.fatec.ecommerce.catalogo.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Combo: SKU próprio, preço próprio e ao menos um componente (variação ou outro combo).")
public record CadastrarComboRequest(
        @NotBlank(message = "o SKU é obrigatório") @Size(max = 60, message = "no máximo 60 caracteres") String sku,
        @NotBlank(message = "o nome é obrigatório") @Size(max = 255, message = "no máximo 255 caracteres") String nome,
        @NotNull(message = "o preço é obrigatório")
        @DecimalMin(value = "0.00", inclusive = false, message = "o preço deve ser maior que zero")
        @Digits(integer = 10, fraction = 2, message = "o preço aceita até 2 casas decimais") BigDecimal preco,
        @NotEmpty(message = "informe ao menos um componente")
        List<@Valid @NotNull ComponenteDeComboRequest> componentes) {
}
