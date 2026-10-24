package br.com.fatec.ecommerce.preco.dominio;

/** Chain of Responsibility: transporta cotacao corrente da entrega, recebida antes da avaliacao de beneficios. */
public class EtapaFreteBruto implements EtapaPreco {
    @Override public void processar(CalculoPreco calculo) { calculo.definirFrete(calculo.contexto().freteBruto()); }
}
