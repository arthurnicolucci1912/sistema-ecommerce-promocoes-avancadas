package br.com.fatec.ecommerce.promocao.dominio;

import java.time.Instant;
import java.util.*;
import java.util.function.BiFunction;

/** Registro explicito de criadores; o compilador nao possui condicionais por tipo. */
public final class CriadoresDeCondicao {
    private CriadoresDeCondicao() { }
    public record Criador(String tipo, List<String> parametros,
            BiFunction<Map<String, Object>, List<CondicaoElegibilidade>, CondicaoElegibilidade> fabrica)
            implements CriadorCondicao {
        @Override public CondicaoElegibilidade criar(Map<String, Object> p, List<CondicaoElegibilidade> filhos) {
            if (!Set.of("E", "OU", "NAO").contains(tipo) && !filhos.isEmpty()) { throw Parametros.invalido("folha com filhos"); }
            return fabrica.apply(p, filhos);
        }
    }
    public static List<CriadorCondicao> padrao() {
        return List.of(
            new Criador("PRIMEIRA_COMPRA", List.of(), (p,f) -> new PrimeiraCompra()),
            new Criador("VALOR_MINIMO", List.of("minimo"), (p,f) -> new ValorMinimo(Parametros.dinheiro(p,"minimo"))),
            new Criador("CATEGORIA", List.of("categoriaId"), (p,f) -> new CategoriaDoProduto(Parametros.id(p,"categoriaId"))),
            new Criador("SKU", List.of("sku"), (p,f) -> new ItemPorSku(Parametros.texto(p,"sku"))),
            new Criador("REGIAO", List.of("ufs"), (p,f) -> new RegiaoDeEntrega(ufs(p))),
            new Criador("PERFIL", List.of("perfil"), (p,f) -> new PerfilDoCliente(Parametros.texto(p,"perfil"))),
            new Criador("PERIODO", List.of("inicio","fim"), (p,f) -> periodo(p)),
            new Criador("E", List.of(), (p,f) -> new CondicaoE(f)),
            new Criador("OU", List.of(), (p,f) -> new CondicaoOu(f)),
            new Criador("NAO", List.of(), (p,f) -> { if(f.size()!=1) { throw Parametros.invalido("NAO exige um filho"); } return new CondicaoNao(f.getFirst()); })
        );
    }
    private static Set<String> ufs(Map<String, Object> p) {
        if (!(p.get("ufs") instanceof List<?> lista) || lista.isEmpty()) { throw Parametros.invalido("ufs"); }
        Set<String> resultado = new TreeSet<>();
        for (Object uf : lista) {
            if (!(uf instanceof String s) || br.com.fatec.ecommerce.cliente.dominio.Uf.da(s).isEmpty()) { throw Parametros.invalido("ufs"); }
            resultado.add(s.trim().toUpperCase(Locale.ROOT));
        }
        return Set.copyOf(resultado);
    }
    private static CondicaoElegibilidade periodo(Map<String, Object> p) {
        try {
            Instant inicio = Instant.parse(Parametros.texto(p,"inicio")), fim = Instant.parse(Parametros.texto(p,"fim"));
            if (!inicio.isBefore(fim)) { throw Parametros.invalido("periodo"); }
            return new PeriodoDeVigencia(inicio,fim);
        } catch (java.time.format.DateTimeParseException erro) { throw Parametros.invalido("periodo"); }
    }
}
