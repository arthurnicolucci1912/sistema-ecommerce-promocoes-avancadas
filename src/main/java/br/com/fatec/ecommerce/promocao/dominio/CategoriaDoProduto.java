package br.com.fatec.ecommerce.promocao.dominio;

/** Composite/Specification: folha de elegibilidade CategoriaDoProduto. */
public record CategoriaDoProduto(Long categoriaId) implements CondicaoElegibilidade {
    @Override public boolean avaliar(ContextoPreco contexto) { return contexto.itens().stream().anyMatch(i -> i.categoriaIds().contains(categoriaId)); }
}
