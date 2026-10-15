package br.com.fatec.ecommerce.cliente.dominio;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Sigla de estado brasileiro valida (ver {@link Uf}). Valores nulos sao aceitos; combine com {@code @NotBlank}. */
@Documented
@Constraint(validatedBy = UfValidaValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface UfValida {

    String message() default "UF inválida: use a sigla de um estado brasileiro.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
