package br.com.fatec.ecommerce.promocao.dominio;

import java.time.Instant;
import java.util.Optional;

/**
 * Contrato de uma promocao: calcula o beneficio que ela oferece para um contexto de compra.
 *
 * <p>Pattern: Strategy - papel: interface da estrategia (cada tipo de promocao e uma estrategia concreta).
 *
 * <p>Problema do enunciado: pelo menos quatro tipos de promocao, com uma quinta ou sexta entrando com
 * impacto reduzido, sem transformar o codigo em uma sequencia de estruturas condicionais. O motor de
 * promocoes trata todas de forma uniforme; novo tipo = nova implementacao, sem alterar as existentes.
 *
 * <p>Implementacao concreta: etapa E5 (modulo {@code promocao}). Nao ha implementacao na fundacao.
 *
 * <p>Contrato provisorio (onda 0): mudancas via PR revisado pelos consumidores e registradas em docs/03-arquitetura.md.
 *
 * <p>Ver docs/04-design-patterns.md, secao 3.1 (Strategy: tipos de promocao e politica de conflito).
 */
public interface Promocao {

    Long id();

    String nome();

    int prioridade();

    ModoAcumulacao modo();

    boolean vigenteEm(Instant momento);

    /** Avalia o contexto; vazio se a promocao nao for elegivel. */
    Optional<DescontoCandidato> avaliar(ContextoPreco contexto);
}
