package br.com.fatec.ecommerce.cliente.dominio;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Implementa {@link UfValida} sobre {@link Uf}. */
public class UfValidaValidator implements ConstraintValidator<UfValida, CharSequence> {

    @Override
    public boolean isValid(CharSequence uf, ConstraintValidatorContext contexto) {
        return uf == null || Uf.da(uf.toString()).isPresent();
    }
}
