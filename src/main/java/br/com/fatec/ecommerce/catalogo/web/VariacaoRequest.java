package br.com.fatec.ecommerce.catalogo.web;

import br.com.fatec.ecommerce.catalogo.aplicacao.NovaVariacao;
import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Map;

@Schema(description = "Variação de um produto: SKU único no catálogo, preço e atributos livres (ex.: tamanho, cor).")
public record VariacaoRequest(
        @NotBlank(message = "o SKU é obrigatório") @Size(max = 60, message = "no máximo 60 caracteres") String sku,
        @NotNull(message = "o preço é obrigatório")
        @DecimalMin(value = "0.00", inclusive = false, message = "o preço deve ser maior que zero")
        @Digits(integer = 10, fraction = 2, message = "o preço aceita até 2 casas decimais") BigDecimal preco,
        Map<String, String> atributos) {

    NovaVariacao paraDados() {
        return new NovaVariacao(sku, Dinheiro.de(preco), atributos == null ? Map.of() : atributos);
    }
}
