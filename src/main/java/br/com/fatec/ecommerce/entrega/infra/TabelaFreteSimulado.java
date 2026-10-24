package br.com.fatec.ecommerce.entrega.infra;

import br.com.fatec.ecommerce.entrega.dominio.*;
import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import java.util.*;
import org.springframework.core.env.Environment;

/** Configuracao imutavel validada no startup; valores de demonstracao nao representam tarifas reais. */
public class TabelaFreteSimulado {
    public record Tarifa(Dinheiro valor, int dias) {}
    private final Map<RegiaoEntrega,Tarifa> tarifas = new EnumMap<>(RegiaoEntrega.class);
    private final Set<String> cobertura;
    public TabelaFreteSimulado(Environment env, String modalidade) {
        boolean expressa = modalidade.equals("expressa");
        int[] precos = expressa ? new int[]{40,48,56,64,72} : new int[]{25,30,35,40,45};
        int[] dias = expressa ? new int[]{2,3,3,4,6} : new int[]{5,6,7,9,12};
        String prefixo = "ecommerce.entrega." + modalidade + ".";
        for (var regiao : RegiaoEntrega.values()) {
            String chave = prefixo + regiao.name().toLowerCase(Locale.ROOT) + ".";
            var valor = Dinheiro.de(env.getProperty(chave + "valor", Integer.toString(precos[regiao.ordinal()])));
            int prazo = env.getProperty(chave + "prazo-dias", Integer.class, dias[regiao.ordinal()]);
            new CotacaoFrete(modalidade.toUpperCase(Locale.ROOT), valor, prazo);
            tarifas.put(regiao, new Tarifa(valor,prazo));
        }
        String ufs = env.getProperty(prefixo + "ufs", String.join(",", Arrays.stream(RegiaoEntrega.values())
                .flatMap(r -> r.ufs().stream()).sorted().toList()));
        Set<String> selecao = new HashSet<>();
        if (!ufs.isBlank()) for (String uf : ufs.split(",", -1)) {
            String normalizada = uf.trim().toUpperCase(Locale.ROOT);
            RegiaoEntrega.da(normalizada); selecao.add(normalizada);
        }
        cobertura = Set.copyOf(selecao);
    }
    public boolean atende(String uf) { return cobertura.contains(uf); }
    public Tarifa tarifa(RegiaoEntrega regiao) { return tarifas.get(regiao); }
}
