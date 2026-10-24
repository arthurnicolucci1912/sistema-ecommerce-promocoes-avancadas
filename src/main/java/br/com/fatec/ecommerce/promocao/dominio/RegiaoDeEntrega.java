package br.com.fatec.ecommerce.promocao.dominio;

/** Composite/Specification: folha de elegibilidade RegiaoDeEntrega. */
public record RegiaoDeEntrega(java.util.Set<String> ufs) implements CondicaoElegibilidade {
    public RegiaoDeEntrega { ufs = java.util.Set.copyOf(ufs); }
    @Override public boolean avaliar(ContextoPreco contexto) { return contexto.uf() != null && ufs.contains(contexto.uf()); }
}
