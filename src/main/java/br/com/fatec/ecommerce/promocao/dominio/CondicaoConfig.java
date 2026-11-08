package br.com.fatec.ecommerce.promocao.dominio;

import java.util.List;
import java.util.Map;

/** Arvore declarativa; compilada pelo registro, nunca executada como expressao ou codigo. */
public record CondicaoConfig(String tipo, Map<String, Object> parametros, List<CondicaoConfig> filhos) {
    public CondicaoConfig {
        parametros = Parametros.copiaSemNulos(parametros);
        filhos = filhos == null ? List.of() : copiaFilhos(filhos);
    }
    private static List<CondicaoConfig> copiaFilhos(List<CondicaoConfig> filhos) {
        if (filhos.stream().anyMatch(java.util.Objects::isNull)) { throw Parametros.invalido("condicao filha nula"); }
        return List.copyOf(filhos);
    }
}
