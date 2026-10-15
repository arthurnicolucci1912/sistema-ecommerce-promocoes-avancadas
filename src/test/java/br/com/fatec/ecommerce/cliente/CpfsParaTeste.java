package br.com.fatec.ecommerce.cliente;

/** Gera CPFs ficticios validos para os testes, calculando os digitos verificadores. */
public final class CpfsParaTeste {

    private CpfsParaTeste() {
    }

    /** CPF valido (somente digitos) cuja base de 9 digitos deriva de {@code semente}; sementes diferentes dao CPFs diferentes. */
    public static String valido(int semente) {
        String base = String.format("%09d", 123_456_000 + semente);
        String comPrimeiro = base + dv(base);
        return comPrimeiro + dv(comPrimeiro);
    }

    /** Mesmo CPF de {@link #valido(int)} formatado como 000.000.000-00. */
    public static String comMascara(int semente) {
        String c = valido(semente);
        return c.substring(0, 3) + "." + c.substring(3, 6) + "." + c.substring(6, 9) + "-" + c.substring(9);
    }

    private static int dv(String digitos) {
        int soma = 0;
        int peso = digitos.length() + 1;
        for (char c : digitos.toCharArray()) {
            soma += (c - '0') * peso--;
        }
        int resto = soma * 10 % 11;
        return resto == 10 ? 0 : resto;
    }
}
