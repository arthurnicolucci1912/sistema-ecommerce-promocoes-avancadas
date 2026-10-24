package br.com.fatec.ecommerce.promocao.dominio;

import java.util.*;
/** Registro de criadores: novo beneficio nao modifica beneficios existentes. */
public class BeneficioCupomRegistry {
    private final Map<String,BeneficioCupomFactory> fabricas=new TreeMap<>();
    public BeneficioCupomRegistry(List<BeneficioCupomFactory> lista) {
        lista.forEach(f->{ if(fabricas.putIfAbsent(f.tipo(),f)!=null) { throw new IllegalArgumentException("Benefício duplicado: "+f.tipo()); } });
    }
    public BeneficioCupom criar(Cupom cupom) {
        var f=fabricas.get(cupom.tipo()); if(f==null) { throw Parametros.invalido("tipo de benefício "+cupom.tipo()); }
        return f.criar(cupom.parametros());
    }
    public Map<String,List<String>> tipos() { Map<String,List<String>> r=new TreeMap<>(); fabricas.forEach((k,v)->r.put(k,v.parametros())); return Map.copyOf(r); }
    public record Fabrica(String tipo,List<String> parametros,java.util.function.Function<Map<String,Object>,BeneficioCupom> criador) implements BeneficioCupomFactory {
        public BeneficioCupom criar(Map<String,Object> p) { return criador.apply(p); }
    }
    public static List<BeneficioCupomFactory> padrao() {
        return List.of(new Fabrica("PERCENTUAL",List.of("percentual"),PercentualCupom::new),
            new Fabrica("VALOR_FIXO",List.of("valor"),ValorFixoCupom::new),
            new Fabrica("FRETE_GRATIS",List.of(),FreteGratisCupom::new),
            new Fabrica("REDUCAO_FRETE",List.of("valor"),ReducaoFreteCupom::new));
    }
}
