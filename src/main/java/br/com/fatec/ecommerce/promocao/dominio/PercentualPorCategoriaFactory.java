package br.com.fatec.ecommerce.promocao.dominio;

/** Factory Method: criador concreto do tipo PERCENTUAL_CATEGORIA. */
public class PercentualPorCategoriaFactory implements PromocaoFactory {
    public String tipo() { return "PERCENTUAL_CATEGORIA"; }
    public java.util.List<String> parametros() { return java.util.List.of("categoriaId","percentual"); }
    public Promocao criar(PromocaoConfig config, CondicaoElegibilidade condicao) { return new PromocaoPercentualPorCategoria(config,condicao); }
}
