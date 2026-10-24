package br.com.fatec.ecommerce.promocao.dominio;

/** Composite/Specification: folha de elegibilidade PrimeiraCompra. */
public record PrimeiraCompra() implements CondicaoElegibilidade {
    @Override public boolean avaliar(ContextoPreco contexto) { return contexto.primeiraCompra(); }
}
