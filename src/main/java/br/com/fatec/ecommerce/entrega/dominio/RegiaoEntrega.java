package br.com.fatec.ecommerce.entrega.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.DadoInvalidoException;
import java.util.*;

/** Regioes e UFs usadas pelas tabelas simuladas; nenhum peso ficticio e inferido. */
public enum RegiaoEntrega {
    SUDESTE("ES MG RJ SP"), SUL("PR RS SC"), CENTRO_OESTE("DF GO MS MT"),
    NORDESTE("AL BA CE MA PB PE PI RN SE"), NORTE("AC AM AP PA RO RR TO");
    private final Set<String> ufs;
    RegiaoEntrega(String ufs) { this.ufs = Set.of(ufs.split(" ")); }
    public Set<String> ufs() { return ufs; }
    public static RegiaoEntrega da(String uf) {
        if (uf == null) throw new DadoInvalidoException("UF inválida para entrega.");
        return Arrays.stream(values()).filter(r -> r.ufs.contains(uf)).findFirst()
                .orElseThrow(() -> new DadoInvalidoException("UF inválida para entrega."));
    }
}
