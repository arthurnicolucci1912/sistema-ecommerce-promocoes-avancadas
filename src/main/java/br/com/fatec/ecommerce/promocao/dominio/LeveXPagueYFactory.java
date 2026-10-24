package br.com.fatec.ecommerce.promocao.dominio;

/** Factory Method: criador concreto do tipo LEVE_X_PAGUE_Y. */
public class LeveXPagueYFactory implements PromocaoFactory {
    public String tipo() { return "LEVE_X_PAGUE_Y"; }
    public java.util.List<String> parametros() { return java.util.List.of("sku","leve","pague"); }
    public Promocao criar(PromocaoConfig config, CondicaoElegibilidade condicao) { return new PromocaoLeveXPagueY(config,condicao); }
}
