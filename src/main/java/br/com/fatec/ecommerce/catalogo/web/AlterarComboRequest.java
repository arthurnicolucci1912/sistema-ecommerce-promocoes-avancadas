package br.com.fatec.ecommerce.catalogo.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Alteração de um combo: preço e/ou componentes. Informe ao menos um dos dois; "
        + "os componentes informados substituem os atuais.")
public record AlterarComboRequest(
        @DecimalMin(value = "0.00", inclusive = false, message = "o preço deve ser maior que zero")
        @Digits(integer = 10, fraction = 2, message = "o preço aceita até 2 casas decimais") BigDecimal preco,
        List<@Valid @NotNull ComponenteDeComboRequest> componentes) {
}
