package br.com.fatec.ecommerce.promocao.dominio;

/** Factory Method: criador concreto do tipo FRETE_GRATIS. */
public class FreteGratisFactory implements PromocaoFactory {
    public String tipo() { return "FRETE_GRATIS"; }
    public java.util.List<String> parametros() { return java.util.List.of(); }
    public Promocao criar(PromocaoConfig config, CondicaoElegibilidade condicao) { return new PromocaoFreteGratis(config,condicao); }
}
