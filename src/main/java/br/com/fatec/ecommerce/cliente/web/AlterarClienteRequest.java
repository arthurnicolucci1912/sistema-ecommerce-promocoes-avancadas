package br.com.fatec.ecommerce.cliente.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados alteráveis do cliente: somente nome e telefone. E-mail e CPF não são alteráveis "
        + "por este recurso; se enviados, são ignorados.")
public record AlterarClienteRequest(
        @NotBlank(message = "o nome é obrigatório") @Size(max = 255, message = "no máximo 255 caracteres") String nome,
        @Size(max = 20, message = "no máximo 20 caracteres") String telefone) {
}
