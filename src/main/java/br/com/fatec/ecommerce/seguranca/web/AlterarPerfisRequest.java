package br.com.fatec.ecommerce.seguranca.web;

import br.com.fatec.ecommerce.seguranca.dominio.Perfil;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record AlterarPerfisRequest(
        @NotEmpty(message = "informe ao menos um perfil") Set<@NotNull(message = "perfil inválido") Perfil> perfis) {
}
