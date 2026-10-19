package br.com.fatec.ecommerce.catalogo.web;

import br.com.fatec.ecommerce.catalogo.aplicacao.ComponenteDeCombo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

@Schema(description = "Componente de um combo: SKU de um item vendável existente e a quantidade.")
public record ComponenteDeComboRequest(
        @NotBlank(message = "o SKU do componente é obrigatório") String sku,
        @Positive(message = "a quantidade deve ser maior que zero") int quantidade) {

    ComponenteDeCombo paraDados() {
        return new ComponenteDeCombo(sku, quantidade);
    }
}
