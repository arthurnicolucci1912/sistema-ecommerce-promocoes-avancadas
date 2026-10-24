package br.com.fatec.ecommerce.seguranca.infra.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Seguranca da API: stateless, CSRF desabilitado (sem cookies), resource server JWT (HS256),
 * perfis do claim {@code perfis} mapeados para {@code ROLE_<PERFIL>}, e erros 401/403 em Problem Details.
 * Apenas mecanismos do framework (cadeia de filtros e DelegatingPasswordEncoder).
 */
@Configuration
@EnableMethodSecurity
public class ConfiguracaoSeguranca {

    static final String CLAIM_PERFIS = "perfis";

    @Bean
    SecurityFilterChain filtrosDeSeguranca(HttpSecurity http, ProblemDetailAuthenticationEntryPoint entryPoint,
            ProblemDetailAccessDeniedHandler accessDeniedHandler) {
        http
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/clientes").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**",
                                "/actuator/health/**")
                        .permitAll()
                        // Pedidos: regras por rota e metodo, antes da regra geral. A ordem importa (vence a primeira).
                        .requestMatchers(HttpMethod.GET, "/api/admin/pedidos", "/api/admin/pedidos/*")
                        .hasAnyRole("ADMIN", "OPERADOR_COMERCIAL")
                        .requestMatchers(HttpMethod.POST, "/api/admin/pedidos/*/preparacao",
                                "/api/admin/pedidos/*/envio", "/api/admin/pedidos/*/entrega")
                        .hasAnyRole("ADMIN", "ESTOQUE")
                        .requestMatchers(HttpMethod.POST, "/api/admin/pedidos/*/cancelamento")
                        .hasAnyRole("ADMIN", "OPERADOR_COMERCIAL")
                        // Vitrine do catalogo: leitura publica. Escrita em /api/catalogo/** cai em authenticated.
                        .requestMatchers(HttpMethod.GET, "/api/catalogo/**").permitAll()
                        // Catalogo administrativo e estoque: por area, antes da regra geral de /api/admin/**.
                        .requestMatchers("/api/admin/categorias/**", "/api/admin/produtos/**",
                                "/api/admin/combos/**")
                        .hasAnyRole("ADMIN", "OPERADOR_COMERCIAL")
                        .requestMatchers("/api/admin/estoque/**").hasAnyRole("ADMIN", "ESTOQUE")
                        // Relatorios (UC-A10): campanhas tambem para PROMOCOES; a regra mais especifica vem primeiro.
                        .requestMatchers("/api/admin/relatorios/campanhas")
                        .hasAnyRole("ADMIN", "OPERADOR_COMERCIAL", "PROMOCOES")
                        .requestMatchers("/api/admin/relatorios/**").hasAnyRole("ADMIN", "OPERADOR_COMERCIAL")
                        // O restante da area administrativa (inclusive usuarios, confirmacao e retentativa de estorno) e so do ADMIN.
                        .requestMatchers("/api/admin/promocoes/**", "/api/admin/cupons/**").hasAnyRole("ADMIN", "PROMOCOES")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/clientes/me/**", "/api/clientes/me").hasRole("CLIENTE")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(o -> o
                        .jwt(j -> j.jwtAuthenticationConverter(conversorDeJwt()))
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    JwtDecoder jwtDecoder(JwtProperties propriedades) {
        return NimbusJwtDecoder.withSecretKey(chave(propriedades)).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    JwtEncoder jwtEncoder(JwtProperties propriedades) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chave(propriedades)));
    }

    private static SecretKey chave(JwtProperties propriedades) {
        return new SecretKeySpec(propriedades.segredo().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    private static Converter<Jwt, AbstractAuthenticationToken> conversorDeJwt() {
        JwtGrantedAuthoritiesConverter autoridades = new JwtGrantedAuthoritiesConverter();
        autoridades.setAuthoritiesClaimName(CLAIM_PERFIS);
        autoridades.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(autoridades);
        return conversor;
    }
}
