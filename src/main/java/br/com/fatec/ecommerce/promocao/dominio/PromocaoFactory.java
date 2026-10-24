package br.com.fatec.ecommerce.promocao.dominio;

import java.util.List;
/** Factory Method: cada criador materializa um tipo configurado de estrategia. */
public interface PromocaoFactory {
    String tipo();
    List<String> parametros();
    Promocao criar(PromocaoConfig config, CondicaoElegibilidade condicao);
}
