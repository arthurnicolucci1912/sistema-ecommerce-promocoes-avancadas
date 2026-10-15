package br.com.fatec.ecommerce.entrega.dominio;

/**
 * Contrato que o dominio exige de uma transportadora/modalidade de frete.
 *
 * <p>Pattern: Adapter - papel: alvo (Target); o servico de cada transportadora e adaptado a este contrato.
 *
 * <p>Problema do enunciado: novas transportadoras e modalidades sem alterar o calculo de entrega. O modulo
 * {@code entrega} coleta as cotacoes de todos os provedores que atendem o contexto.
 *
 * <p>Implementacoes concretas: etapa E7 (modulo {@code entrega}). Nao ha implementacao na fundacao.
 *
 * <p>Contrato provisorio (onda 0): mudancas via PR revisado pelos consumidores e registradas em docs/03-arquitetura.md.
 *
 * <p>Ver docs/04-design-patterns.md, secao 3.5 (Adapter; a extensao para transportadoras esta descrita ali).
 */
public interface ProvedorFrete {

    String modalidade();

    /** Se o provedor atende o contexto (regiao, peso, valor). */
    boolean atende(ContextoFrete contexto);

    /** Custo e prazo para o contexto. */
    CotacaoFrete cotar(ContextoFrete contexto);
}
