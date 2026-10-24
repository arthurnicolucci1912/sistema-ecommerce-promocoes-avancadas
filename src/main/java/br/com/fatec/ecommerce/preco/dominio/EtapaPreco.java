package br.com.fatec.ecommerce.preco.dominio;

/**
 * Contrato de uma etapa da formacao do preco (subtotal, promocoes, cupom, frete, total).
 *
 * <p>Pattern: Chain of Responsibility (pipeline) - papel: handler/etapa; cada etapa le e atualiza o
 * {@link CalculoPreco} e o servico de preco as executa em ordem explicita.
 *
 * <p>Problema do enunciado: o valor final resulta de varias etapas e o calculo nao deve ficar concentrado em
 * um unico metodo com muitas estruturas condicionais. Cada etapa e testavel sozinha e etapa nova (taxa,
 * cashback) nao quebra as existentes.
 *
 * <p>Implementacoes concretas da E3: {@link EtapaSubtotal} e {@link EtapaTotal}, compostas explicitamente
 * pelo PipelinePreco. Promocoes, cupom e frete entram nas etapas seguintes.
 *
 * <p>Contrato provisorio (onda 0): mudancas via PR revisado pelos consumidores e registradas em docs/03-arquitetura.md.
 *
 * <p>Ver docs/04-design-patterns.md, secao 3.3 (Chain of Responsibility / pipeline: formacao do preco).
 */
public interface EtapaPreco {

    /** Le e atualiza subtotal, descontos, frete e total do calculo. */
    void processar(CalculoPreco calculo);
}
