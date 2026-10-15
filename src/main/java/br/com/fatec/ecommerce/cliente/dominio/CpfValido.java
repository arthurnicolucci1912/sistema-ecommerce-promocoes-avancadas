package br.com.fatec.ecommerce.cliente.dominio;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * CPF com digitos verificadores corretos (com ou sem mascara). Valores nulos sao aceitos aqui;
 * combine com {@code @NotBlank} para exigir o campo.
 */
@Documented
@Constraint(validatedBy = CpfValidoValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface CpfValido {

    String message() default "CPF inválido.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
