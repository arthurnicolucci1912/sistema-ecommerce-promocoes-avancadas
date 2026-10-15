package br.com.fatec.ecommerce.compartilhado.infra;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Metadados da documentacao OpenAPI e esquema de seguranca Bearer JWT aplicado globalmente. */
@Configuration
public class ConfiguracaoOpenApi {

    static final String ESQUEMA_BEARER = "bearer-jwt";

    @Bean
    OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("E-commerce FATEC")
                        .description("API REST do sistema de e-commerce academico (FATEC).")
                        .version("0.0.1"))
                .components(new Components().addSecuritySchemes(ESQUEMA_BEARER,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_BEARER));
    }
}
