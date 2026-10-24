package br.com.fatec.ecommerce.promocao.dominio;

/** Composite/Specification: composto com a mesma interface das folhas. */
public record CondicaoOu(java.util.List<CondicaoElegibilidade> filhos) implements CondicaoElegibilidade {
    public CondicaoOu {
        filhos = java.util.List.copyOf(filhos);
        if (filhos.size() < 2) { throw Parametros.invalido("CondicaoOu exige dois ou mais filhos"); }
    }
    @Override public boolean avaliar(ContextoPreco contexto) { return filhos.stream().anyMatch(f -> f.avaliar(contexto)); }
}
