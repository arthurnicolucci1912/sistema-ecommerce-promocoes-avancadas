package br.com.fatec.ecommerce.promocao.dominio;

/** Composite/Specification: folha de elegibilidade PerfilDoCliente. */
public record PerfilDoCliente(String perfil) implements CondicaoElegibilidade {
    @Override public boolean avaliar(ContextoPreco contexto) { return contexto.perfis().contains(perfil); }
}
