package br.com.fatec.ecommerce.cliente.web;

import br.com.fatec.ecommerce.cliente.aplicacao.DadosDoEndereco;
import br.com.fatec.ecommerce.cliente.dominio.UfValida;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados de um endereço de entrega.")
public record EnderecoRequest(
        @NotBlank(message = "o apelido é obrigatório") @Size(max = 60, message = "no máximo 60 caracteres") String apelido,
        @Schema(description = "CEP com 8 dígitos, com ou sem hífen (ex.: 01310-100).")
        @NotBlank(message = "o CEP é obrigatório") @Pattern(regexp = "\\d{5}-?\\d{3}", message = "CEP inválido: informe 8 dígitos") String cep,
        @NotBlank(message = "o logradouro é obrigatório") @Size(max = 255, message = "no máximo 255 caracteres") String logradouro,
        @NotBlank(message = "o número é obrigatório") @Size(max = 20, message = "no máximo 20 caracteres") String numero,
        @Size(max = 100, message = "no máximo 100 caracteres") String complemento,
        @NotBlank(message = "o bairro é obrigatório") @Size(max = 100, message = "no máximo 100 caracteres") String bairro,
        @NotBlank(message = "a cidade é obrigatória") @Size(max = 100, message = "no máximo 100 caracteres") String cidade,
        @Schema(description = "Sigla de estado brasileiro (ex.: SP).")
        @NotBlank(message = "a UF é obrigatória") @UfValida String uf) {

    DadosDoEndereco paraDados() {
        return new DadosDoEndereco(apelido, cep, logradouro, numero, complemento, bairro, cidade, uf);
    }
}
