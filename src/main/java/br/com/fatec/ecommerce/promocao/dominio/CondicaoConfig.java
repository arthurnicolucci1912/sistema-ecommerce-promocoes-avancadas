package br.com.fatec.ecommerce.promocao.dominio;

import java.util.List;
import java.util.Map;

/** Arvore declarativa; compilada pelo registro, nunca executada como expressao ou codigo. */
public record CondicaoConfig(String tipo, Map<String, Object> parametros, List<CondicaoConfig> filhos) {
    public CondicaoConfig {
        parametros = parametros == null ? Map.of() : Map.copyOf(parametros);
        filhos = filhos == null ? List.of() : List.copyOf(filhos);
    }
}
