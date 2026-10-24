package br.com.fatec.ecommerce.promocao.dominio;

/** Composite/Specification: folha de elegibilidade ValorMinimo. */
public record ValorMinimo(br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro minimo) implements CondicaoElegibilidade {
    @Override public boolean avaliar(ContextoPreco contexto) { return contexto.subtotal().maiorOuIgualA(minimo); }
}
