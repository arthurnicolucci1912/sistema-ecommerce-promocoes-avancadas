package br.com.fatec.ecommerce.entrega.infra;

import br.com.fatec.ecommerce.entrega.dominio.*;
import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.core.env.Environment;

/**
 * Configuracao imutavel validada no startup; valores de demonstracao nao representam tarifas reais.
 *
 * <p>Regra tarifaria simulada (fica aqui, no adaptee, e nao no checkout), para cada modalidade:
 * <pre>
 * custo = base da regiao
 *       + max(0, pesoKg - franquiaKg) * adicionalPorKg
 *       + max(0, itens - franquiaItens) * adicionalPorItem
 *       + max(0, valor - franquiaValor) * seguroPercentual / 100    (seguro ad valorem sobre o excedente)
 * </pre>
 * arredondado uma unica vez (HALF_EVEN, 2 casas). Padroes: economica 1 kg, R$ 2,00/kg, 5 itens, R$ 1,00/item,
 * R$ 200,00, 1%; expressa 1 kg, R$ 4,00/kg, 5 itens, R$ 2,00/item, R$ 200,00, 2%. Cada coeficiente pode ser
 * configurado por {@code ecommerce.entrega.<modalidade>.franquia-kg|adicional-kg|franquia-itens|adicional-item|
 * franquia-valor|seguro-percentual}.
 */
public class TabelaFreteSimulado {
    public record Tarifa(Dinheiro valor, int dias) {}
    public record Coeficientes(BigDecimal franquiaKg, Dinheiro adicionalPorKg, int franquiaItens,
            Dinheiro adicionalPorItem, Dinheiro franquiaValor, BigDecimal seguroPercentual) {
        public Coeficientes {
            if (franquiaKg.signum() < 0 || adicionalPorKg.ehNegativo() || franquiaItens < 0
                    || adicionalPorItem.ehNegativo() || franquiaValor.ehNegativo() || seguroPercentual.signum() < 0)
                throw new IllegalArgumentException("Coeficientes de frete inválidos.");
        }
    }
    private final Coeficientes coeficientes;
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
        coeficientes = new Coeficientes(
                new BigDecimal(env.getProperty(prefixo + "franquia-kg", "1")),
                Dinheiro.de(env.getProperty(prefixo + "adicional-kg", expressa ? "4" : "2")),
                env.getProperty(prefixo + "franquia-itens", Integer.class, 5),
                Dinheiro.de(env.getProperty(prefixo + "adicional-item", expressa ? "2" : "1")),
                Dinheiro.de(env.getProperty(prefixo + "franquia-valor", "200")),
                new BigDecimal(env.getProperty(prefixo + "seguro-percentual", expressa ? "2" : "1")));
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
    public Coeficientes coeficientes() { return coeficientes; }
    /** Custo do frete para regiao, peso, quantidade de itens e valor da compra (formula na documentacao da classe). */
    public Dinheiro custo(RegiaoEntrega regiao, BigDecimal pesoKg, int itens, Dinheiro valor) {
        var c = coeficientes;
        var excessoKg = pesoKg.subtract(c.franquiaKg()).max(BigDecimal.ZERO);
        var excessoItens = BigDecimal.valueOf(Math.max(0, itens - c.franquiaItens()));
        var excessoValor = valor.valor().subtract(c.franquiaValor().valor()).max(BigDecimal.ZERO);
        return Dinheiro.de(tarifas.get(regiao).valor().valor()
                .add(excessoKg.multiply(c.adicionalPorKg().valor()))
                .add(excessoItens.multiply(c.adicionalPorItem().valor()))
                .add(excessoValor.multiply(c.seguroPercentual()).movePointLeft(2)));
    }
}
