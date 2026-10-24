package br.com.fatec.ecommerce.promocao.dominio;

/** Composite/Specification: folha de elegibilidade ItemPorSku. */
public record ItemPorSku(String sku) implements CondicaoElegibilidade {
    @Override public boolean avaliar(ContextoPreco contexto) { return contexto.itens().stream().anyMatch(i -> i.sku().equals(sku)); }
}
