package br.com.fatec.ecommerce.cliente.dominio;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Implementa {@link CpfValido} sobre {@link Cpf}. */
public class CpfValidoValidator implements ConstraintValidator<CpfValido, CharSequence> {

    @Override
    public boolean isValid(CharSequence cpf, ConstraintValidatorContext contexto) {
        return cpf == null || Cpf.valido(cpf.toString());
    }
}
