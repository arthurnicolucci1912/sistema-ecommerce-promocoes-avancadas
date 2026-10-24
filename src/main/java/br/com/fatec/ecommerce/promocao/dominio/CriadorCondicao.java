package br.com.fatec.ecommerce.promocao.dominio;

import java.util.List;
import java.util.Map;

/** Criador registrado de folhas/compostos; uma nova folha nao altera os compostos. */
public interface CriadorCondicao {
    String tipo();
    List<String> parametros();
    CondicaoElegibilidade criar(Map<String, Object> parametros, List<CondicaoElegibilidade> filhos);
}
