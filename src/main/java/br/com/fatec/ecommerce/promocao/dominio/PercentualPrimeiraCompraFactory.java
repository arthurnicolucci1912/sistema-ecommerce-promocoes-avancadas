package br.com.fatec.ecommerce.promocao.dominio;

/** Factory Method: criador concreto do tipo PERCENTUAL_PRIMEIRA_COMPRA. */
public class PercentualPrimeiraCompraFactory implements PromocaoFactory {
    public String tipo() { return "PERCENTUAL_PRIMEIRA_COMPRA"; }
    public java.util.List<String> parametros() { return java.util.List.of("percentual"); }
    public Promocao criar(PromocaoConfig config, CondicaoElegibilidade condicao) { return new PromocaoPercentualPrimeiraCompra(config,condicao); }
}
