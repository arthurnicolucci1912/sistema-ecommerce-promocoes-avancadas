package br.com.fatec.ecommerce.promocao.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.DadoInvalidoException;
import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import java.math.BigDecimal;
import java.util.Map;

/** Conversoes exatas dos dados declarativos; erros de configuracao sao dados invalidos. */
public final class Parametros {
    private Parametros() { }
    /** Copia imutavel do mapa declarativo; valor nulo (JSON {"k": null}) e dado invalido, nao NPE. */
    public static Map<String, Object> copiaSemNulos(Map<String, Object> origem) {
        if (origem == null) { return Map.of(); }
        if (origem.entrySet().stream().anyMatch(e -> e.getKey() == null || e.getValue() == null)) { throw invalido("parametros com valor nulo"); }
        return Map.copyOf(origem);
    }
    public static String texto(Map<String, Object> p, String nome) {
        Object valor = p.get(nome);
        if (!(valor instanceof String s) || s.isBlank()) { throw invalido(nome); }
        return s.trim();
    }
    public static BigDecimal numero(Map<String, Object> p, String nome) {
        try {
            Object valor = p.get(nome);
            if (!(valor instanceof Number) && !(valor instanceof String)) { throw invalido(nome); }
            return new BigDecimal(valor.toString());
        } catch (NumberFormatException erro) { throw invalido(nome); }
    }
    public static long id(Map<String, Object> p, String nome) {
        try { long n = numero(p, nome).longValueExact(); if (n <= 0) { throw invalido(nome); } return n; }
        catch (ArithmeticException erro) { throw invalido(nome); }
    }
    public static int inteiro(Map<String, Object> p, String nome) {
        try { return Math.toIntExact(id(p, nome)); } catch (ArithmeticException erro) { throw invalido(nome); }
    }
    public static Dinheiro dinheiro(Map<String, Object> p, String nome) {
        BigDecimal n = numero(p, nome);
        if (n.signum() < 0 || n.stripTrailingZeros().scale() > 2 || n.precision() - n.scale() > 10) { throw invalido(nome); }
        return Dinheiro.de(n);
    }
    public static BigDecimal percentual(Map<String, Object> p) {
        BigDecimal n = numero(p, "percentual");
        if (n.signum() <= 0 || n.compareTo(new BigDecimal("100")) > 0) { throw invalido("percentual"); }
        return n.movePointLeft(2);
    }
    public static DadoInvalidoException invalido(String campo) {
        return new DadoInvalidoException("Parâmetro inválido: " + campo + ".");
    }
}
