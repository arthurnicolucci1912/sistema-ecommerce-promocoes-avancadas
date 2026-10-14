package br.com.fatec.ecommerce.seguranca.infra.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuracao do JWT (prefixo {@code app.jwt}).
 *
 * <p>O segredo vem de {@code APP_JWT_SECRET}; apenas o perfil {@code dev} tem valor padrao.
 * Como a classe e validada, a aplicacao nao inicia sem o segredo fora desse perfil.
 *
 * @param segredo   chave HS256, com no minimo 32 caracteres
 * @param expiracao tempo de validade do token
 */
@Validated
@ConfigurationProperties("app.jwt")
public record JwtProperties(
        @NotBlank @Size(min = 32, message = "o segredo JWT deve ter no minimo 32 caracteres") String segredo,
        @NotNull Duration expiracao) {
}
