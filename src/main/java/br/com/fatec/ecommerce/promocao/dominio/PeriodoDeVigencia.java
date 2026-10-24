package br.com.fatec.ecommerce.promocao.dominio;

/** Composite/Specification: folha de elegibilidade PeriodoDeVigencia. */
public record PeriodoDeVigencia(java.time.Instant inicio, java.time.Instant fim) implements CondicaoElegibilidade {
    @Override public boolean avaliar(ContextoPreco contexto) { return !contexto.momento().isBefore(inicio) && contexto.momento().isBefore(fim); }
}
