package br.com.fatec.ecommerce.promocao.dominio;

import java.util.*;

/** Compilador da configuracao Composite; limita a arvore antes de construir os objetos. */
public class CondicaoRegistry {
    private final Map<String, CriadorCondicao> criadores = new TreeMap<>();
    public CondicaoRegistry(List<CriadorCondicao> lista) {
        lista.forEach(c -> { if (criadores.putIfAbsent(c.tipo(), c) != null) {
            throw new IllegalArgumentException("Tipo de condição duplicado: " + c.tipo());
        } });
    }
    public CondicaoElegibilidade criar(CondicaoConfig config) { return criar(config, 1, new int[]{0}); }
    private CondicaoElegibilidade criar(CondicaoConfig config, int nivel, int[] nos) {
        if (config == null) { return contexto -> true; }
        if (nivel > 16 || ++nos[0] > 128) { throw Parametros.invalido("limite da árvore de condições"); }
        var criador = criadores.get(config.tipo());
        if (criador == null) { throw Parametros.invalido("tipo de condição " + config.tipo()); }
        return criador.criar(config.parametros(), config.filhos().stream().map(f -> criar(f, nivel + 1, nos)).toList());
    }
    public Map<String, List<String>> tipos() {
        Map<String, List<String>> resultado = new TreeMap<>();
        criadores.forEach((k, v) -> resultado.put(k, v.parametros()));
        return Collections.unmodifiableMap(resultado);
    }
}
