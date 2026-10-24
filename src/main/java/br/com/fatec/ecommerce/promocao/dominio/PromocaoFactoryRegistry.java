package br.com.fatec.ecommerce.promocao.dominio;

import java.util.*;
/** Registro de Factory Method: novo tipo entra como nova fabrica, sem switch no consumidor. */
public class PromocaoFactoryRegistry {
    private final Map<String,PromocaoFactory> fabricas=new TreeMap<>();
    private final CondicaoRegistry condicoes;
    public PromocaoFactoryRegistry(List<PromocaoFactory> lista, CondicaoRegistry condicoes) {
        this.condicoes=condicoes;
        lista.forEach(f -> { if(fabricas.putIfAbsent(f.tipo(),f)!=null) { throw new IllegalArgumentException("Fábrica duplicada: "+f.tipo()); } });
    }
    public Promocao criar(PromocaoConfig config) {
        var fabrica=fabricas.get(config.tipo());
        if(fabrica==null) { throw Parametros.invalido("tipo de promoção "+config.tipo()); }
        return fabrica.criar(config,condicoes.criar(config.condicao()));
    }
    public Map<String,List<String>> tipos() {
        Map<String,List<String>> tipos=new TreeMap<>(); fabricas.forEach((k,v)->tipos.put(k,v.parametros())); return Map.copyOf(tipos);
    }
}
