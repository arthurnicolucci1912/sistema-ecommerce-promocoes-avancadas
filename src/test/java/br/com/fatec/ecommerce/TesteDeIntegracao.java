package br.com.fatec.ecommerce;

import br.com.fatec.ecommerce.cliente.ClientesParaTeste;
import br.com.fatec.ecommerce.seguranca.UsuariosParaTeste;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/** Teste de integracao completo: Spring Boot + MockMvc + PostgreSQL via Testcontainers, perfil "test". */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({TestcontainersConfiguration.class, UsuariosParaTeste.class, ClientesParaTeste.class})
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
public @interface TesteDeIntegracao {
}
