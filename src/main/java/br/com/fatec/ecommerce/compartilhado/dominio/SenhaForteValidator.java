package br.com.fatec.ecommerce.compartilhado.dominio;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Implementa a politica de {@link SenhaForte}. */
public class SenhaForteValidator implements ConstraintValidator<SenhaForte, CharSequence> {

    static final int TAMANHO_MINIMO = 8;

    @Override
    public boolean isValid(CharSequence senha, ConstraintValidatorContext contexto) {
        if (senha == null) {
            return true;
        }
        if (senha.length() < TAMANHO_MINIMO) {
            return false;
        }
        boolean temLetra = false;
        boolean temDigito = false;
        for (int i = 0; i < senha.length(); i++) {
            char c = senha.charAt(i);
            temLetra |= Character.isLetter(c);
            temDigito |= Character.isDigit(c);
        }
        return temLetra && temDigito;
    }
}
