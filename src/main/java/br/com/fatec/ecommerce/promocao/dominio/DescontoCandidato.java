package br.com.fatec.ecommerce.promocao.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;

/**
 * Desconto que uma promocao elegivel propoe, ainda sujeito a {@link PoliticaConflito}.
 *
 * <p>Contrato provisorio (onda 0): mudancas via PR revisado pelos consumidores e registradas em docs/03-arquitetura.md.
 *
 * @param promocaoId  promocao de origem
 * @param nome        nome da promocao, para o detalhamento ao cliente
 * @param valor       valor do desconto
 * @param alvo        onde incide (itens ou frete)
 * @param modo        como se acumula com os demais
 * @param prioridade  desempate/ordenacao pela politica de conflito
 */
public record DescontoCandidato(
        Long promocaoId,
        String nome,
        Dinheiro valor,
        AlvoDesconto alvo,
        ModoAcumulacao modo,
        int prioridade, java.time.Instant criadoEm) {
    public DescontoCandidato(Long promocaoId, String nome, Dinheiro valor, AlvoDesconto alvo,
            ModoAcumulacao modo, int prioridade) {
        this(promocaoId, nome, valor, alvo, modo, prioridade, java.time.Instant.EPOCH);
    }
}
