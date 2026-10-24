package br.com.fatec.ecommerce.promocao.dominio;

/** Factory Method: criador concreto do tipo VALOR_FIXO_ACIMA_DE. */
public class ValorFixoAcimaDeFactory implements PromocaoFactory {
    public String tipo() { return "VALOR_FIXO_ACIMA_DE"; }
    public java.util.List<String> parametros() { return java.util.List.of("minimo","valor"); }
    public Promocao criar(PromocaoConfig config, CondicaoElegibilidade condicao) { return new PromocaoValorFixoAcimaDe(config,condicao); }
}
