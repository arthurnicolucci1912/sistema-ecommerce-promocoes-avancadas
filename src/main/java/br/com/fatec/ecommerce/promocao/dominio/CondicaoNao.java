package br.com.fatec.ecommerce.promocao.dominio;

/** Composite/Specification: negacao de uma condicao, inclusive outro composto. */
public record CondicaoNao(CondicaoElegibilidade filho) implements CondicaoElegibilidade {
    public CondicaoNao { java.util.Objects.requireNonNull(filho); }
    @Override public boolean avaliar(ContextoPreco contexto) { return !filho.avaliar(contexto); }
}
