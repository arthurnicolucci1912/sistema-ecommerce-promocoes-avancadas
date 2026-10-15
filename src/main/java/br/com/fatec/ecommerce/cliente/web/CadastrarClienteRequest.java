package br.com.fatec.ecommerce.cliente.web;

import br.com.fatec.ecommerce.cliente.dominio.CpfValido;
import br.com.fatec.ecommerce.compartilhado.dominio.SenhaForte;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados do autocadastro de cliente.")
public record CadastrarClienteRequest(
        @NotBlank(message = "o nome é obrigatório") @Size(max = 255, message = "no máximo 255 caracteres") String nome,
        @NotBlank(message = "o e-mail é obrigatório") @Email(message = "e-mail inválido") @Size(max = 255, message = "no máximo 255 caracteres") String email,
        @NotBlank(message = "a senha é obrigatória") @SenhaForte String senha,
        @Schema(description = "CPF com ou sem máscara (ex.: 529.982.247-25).")
        @NotBlank(message = "o CPF é obrigatório") @CpfValido String cpf,
        @Size(max = 20, message = "no máximo 20 caracteres") String telefone) {
}
