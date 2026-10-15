package br.com.fatec.ecommerce.promocao.dominio;

import java.util.List;

/**
 * Contrato da regra que decide quais descontos elegiveis serao de fato aplicados quando promocoes conflitam.
 *
 * <p>Pattern: Strategy - papel: interface da estrategia (acumulacao e prioridade intercambiaveis).
 *
 * <p>Problema do enunciado: a estrategia de resolucao de conflitos entre promocoes deve ser clara e
 * demonstravel. Trocar a politica nao altera nenhuma promocao nem o motor.
 *
 * <p>Implementacao concreta: etapa E5 (modulo {@code promocao}). Nao ha implementacao na fundacao.
 *
 * <p>Contrato provisorio (onda 0): mudancas via PR revisado pelos consumidores e registradas em docs/03-arquitetura.md.
 *
 * <p>Ver docs/04-design-patterns.md, secao 3.1 (Strategy: tipos de promocao e politica de conflito).
 */
public interface PoliticaConflito {

    List<DescontoAplicado> resolver(List<DescontoCandidato> elegiveis);
}
