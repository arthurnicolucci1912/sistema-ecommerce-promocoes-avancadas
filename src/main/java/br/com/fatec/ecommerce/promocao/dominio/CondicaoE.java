package br.com.fatec.ecommerce.promocao.dominio;

/** Composite/Specification: composto com a mesma interface das folhas. */
public record CondicaoE(java.util.List<CondicaoElegibilidade> filhos) implements CondicaoElegibilidade {
    public CondicaoE {
        filhos = java.util.List.copyOf(filhos);
        if (filhos.size() < 2) { throw Parametros.invalido("CondicaoE exige dois ou mais filhos"); }
    }
    @Override public boolean avaliar(ContextoPreco contexto) { return filhos.stream().allMatch(f -> f.avaliar(contexto)); }
}
