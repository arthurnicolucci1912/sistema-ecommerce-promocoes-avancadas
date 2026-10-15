package br.com.fatec.ecommerce.cliente.dominio;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/** As 27 unidades da federacao (siglas). */
public enum Uf {
    AC, AL, AP, AM, BA, CE, DF, ES, GO, MA, MT, MS, MG, PA, PB, PR, PE, PI, RJ, RN, RS, RO, RR, SC, SP, SE, TO;

    /** Converte a sigla (sem diferenciar maiusculas, sem espacos nas pontas), se for valida. */
    public static Optional<Uf> da(String sigla) {
        if (sigla == null) {
            return Optional.empty();
        }
        String normalizada = sigla.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(values()).filter(uf -> uf.name().equals(normalizada)).findFirst();
    }
}
