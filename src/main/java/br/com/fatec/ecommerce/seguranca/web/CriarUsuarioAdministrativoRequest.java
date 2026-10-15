package br.com.fatec.ecommerce.seguranca.web;

import br.com.fatec.ecommerce.compartilhado.dominio.SenhaForte;
import br.com.fatec.ecommerce.seguranca.dominio.Perfil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record CriarUsuarioAdministrativoRequest(
        @NotBlank(message = "o e-mail é obrigatório") @Email(message = "e-mail inválido") String email,
        @NotBlank(message = "a senha é obrigatória") @SenhaForte String senha,
        @NotEmpty(message = "informe ao menos um perfil") Set<@NotNull(message = "perfil inválido") Perfil> perfis) {
}
