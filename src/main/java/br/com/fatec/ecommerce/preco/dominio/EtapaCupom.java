package br.com.fatec.ecommerce.preco.dominio;

import br.com.fatec.ecommerce.promocao.dominio.AvaliadorCupom;

/** Chain of Responsibility: valida/calcula cupom, preservando rejeicao recuperavel sem consumir uso. */
public class EtapaCupom implements EtapaPreco {
    private final AvaliadorCupom avaliador;
    public EtapaCupom(AvaliadorCupom avaliador) { this.avaliador=avaliador; }
    @Override public void processar(CalculoPreco calculo) {
        var resultado=avaliador.avaliar(calculo.contexto(),calculo.descontos());
        calculo.definirCupom(resultado.situacao());
        if(resultado.desconto()!=null) { calculo.adicionarDesconto(resultado.desconto()); }
    }
}
