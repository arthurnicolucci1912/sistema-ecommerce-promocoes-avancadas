package br.com.fatec.ecommerce.promocao.dominio;

/**
 * Contrato de uma condicao de elegibilidade (primeira compra, valor minimo, categoria, regiao, periodo, perfil).
 *
 * <p>Pattern: Composite (Specification) - papel: componente; condicoes simples sao folhas e
 * {@code CondicaoE}, {@code CondicaoOu} e {@code CondicaoNao} sao os compostos, todos com esta mesma interface.
 *
 * <p>Problema do enunciado: varias condicoes simultaneas sem metodos excessivamente grandes ou dificeis de
 * manter. Um {@code CondicaoE} com tres filhos substitui um bloco de ifs.
 *
 * <p>Implementacao concreta (folhas e compostos): etapa E5 (modulo {@code promocao}). Nao ha implementacao
 * na fundacao.
 *
 * <p>Contrato provisorio (onda 0): mudancas via PR revisado pelos consumidores e registradas em docs/03-arquitetura.md.
 *
 * <p>Ver docs/04-design-patterns.md, secao 3.2 (Composite / Specification: condicoes de elegibilidade).
 */
public interface CondicaoElegibilidade {

    boolean avaliar(ContextoPreco contexto);
}
