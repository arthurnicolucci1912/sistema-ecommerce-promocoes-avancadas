package br.com.fatec.ecommerce.compartilhado.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Objeto de valor imutavel para quantias monetarias: {@link BigDecimal} com escala 2 e
 * arredondamento {@link RoundingMode#HALF_EVEN}. Toda operacao devolve uma nova instancia.
 * A igualdade e por valor normalizado: {@code 10} e {@code 10.00} sao iguais.
 */
public final class Dinheiro implements Comparable<Dinheiro> {

    public static final int ESCALA = 2;
    public static final RoundingMode ARREDONDAMENTO = RoundingMode.HALF_EVEN;

    private static final Dinheiro ZERO = new Dinheiro(BigDecimal.ZERO);

    private final BigDecimal valor;

    private Dinheiro(BigDecimal valor) {
        this.valor = valor.setScale(ESCALA, ARREDONDAMENTO);
    }

    public static Dinheiro zero() {
        return ZERO;
    }

    public static Dinheiro de(BigDecimal valor) {
        return new Dinheiro(Objects.requireNonNull(valor, "o valor monetario nao pode ser nulo"));
    }

    public static Dinheiro de(String valor) {
        Objects.requireNonNull(valor, "o valor monetario nao pode ser nulo");
        return new Dinheiro(new BigDecimal(valor.trim()));
    }

    public static Dinheiro deCentavos(long centavos) {
        return new Dinheiro(BigDecimal.valueOf(centavos, ESCALA));
    }

    public BigDecimal valor() {
        return valor;
    }

    public Dinheiro somar(Dinheiro outro) {
        return new Dinheiro(valor.add(outro.valor));
    }

    public Dinheiro subtrair(Dinheiro outro) {
        return new Dinheiro(valor.subtract(outro.valor));
    }

    public Dinheiro multiplicar(int fator) {
        return new Dinheiro(valor.multiply(BigDecimal.valueOf(fator)));
    }

    /** Multiplica sem perder precisao e arredonda (HALF_EVEN) uma unica vez, no resultado. */
    public Dinheiro multiplicar(BigDecimal fator) {
        return new Dinheiro(valor.multiply(Objects.requireNonNull(fator, "o fator nao pode ser nulo")));
    }

    public boolean maiorQue(Dinheiro outro) {
        return compareTo(outro) > 0;
    }

    public boolean maiorOuIgualA(Dinheiro outro) {
        return compareTo(outro) >= 0;
    }

    public boolean menorQue(Dinheiro outro) {
        return compareTo(outro) < 0;
    }

    public boolean menorOuIgualA(Dinheiro outro) {
        return compareTo(outro) <= 0;
    }

    public boolean ehZero() {
        return valor.signum() == 0;
    }

    public boolean ehNegativo() {
        return valor.signum() < 0;
    }

    @Override
    public int compareTo(Dinheiro outro) {
        return valor.compareTo(outro.valor);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Dinheiro d && valor.equals(d.valor));
    }

    @Override
    public int hashCode() {
        return valor.hashCode();
    }

    @Override
    public String toString() {
        return valor.toPlainString();
    }
}
