package br.com.fatec.ecommerce.cliente.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.DadoInvalidoException;

/**
 * Objeto de valor do CPF: 11 digitos, com digitos verificadores corretos e sem sequencias repetidas.
 * Aceita entrada com ou sem mascara ({@code 529.982.247-25}) e guarda apenas os digitos.
 */
public record Cpf(String digitos) {

    private static final int TAMANHO = 11;

    public Cpf {
        if (!ehValido(digitos)) {
            throw new DadoInvalidoException("CPF inválido.");
        }
    }

    /** Normaliza (remove mascara) e valida; lanca {@link DadoInvalidoException} se invalido. */
    public static Cpf de(String entrada) {
        return new Cpf(normalizar(entrada));
    }

    /** Verdadeiro se a entrada (com ou sem mascara) e um CPF valido. */
    public static boolean valido(String entrada) {
        return ehValido(normalizar(entrada));
    }

    /** Remove pontos, tracos e espacos. Entrada nula resulta em texto vazio. */
    public static String normalizar(String entrada) {
        return entrada == null ? "" : entrada.replaceAll("[.\\-\\s]", "");
    }

    private static boolean ehValido(String digitos) {
        if (digitos == null || digitos.length() != TAMANHO || !digitos.chars().allMatch(c -> c >= '0' && c <= '9')) {
            return false;
        }
        if (digitos.chars().distinct().count() == 1) {
            return false;
        }
        int primeiro = digitoVerificador(digitos, 9);
        int segundo = digitoVerificador(digitos, 10);
        return primeiro == digitos.charAt(9) - '0' && segundo == digitos.charAt(10) - '0';
    }

    /** Calcula o DV sobre os primeiros {@code quantidade} digitos (pesos de quantidade+1 ate 2). */
    private static int digitoVerificador(String digitos, int quantidade) {
        int soma = 0;
        for (int i = 0; i < quantidade; i++) {
            soma += (digitos.charAt(i) - '0') * (quantidade + 1 - i);
        }
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }

    @Override
    public String toString() {
        return digitos;
    }
}
