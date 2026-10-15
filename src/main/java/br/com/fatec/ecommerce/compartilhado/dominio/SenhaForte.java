package br.com.fatec.ecommerce.compartilhado.dominio;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Politica minima de senha: ao menos 8 caracteres, com pelo menos uma letra e um digito.
 * Valores nulos sao aceitos aqui; combine com {@code @NotBlank} para exigir o campo.
 */
@Documented
@Constraint(validatedBy = SenhaForteValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface SenhaForte {

    String message() default "A senha deve ter no mínimo 8 caracteres, com pelo menos uma letra e um dígito.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
