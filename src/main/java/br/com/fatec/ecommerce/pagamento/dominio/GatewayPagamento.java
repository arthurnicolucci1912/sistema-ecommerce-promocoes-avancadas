package br.com.fatec.ecommerce.pagamento.dominio;

/**
 * Contrato que o dominio exige de um fornecedor de pagamento.
 *
 * <p>Pattern: Adapter - papel: alvo (Target); cada fornecedor simulado, com interface propria, ganha um
 * adaptador que a traduz para este contrato.
 *
 * <p>Problema do enunciado: fornecedores com interfaces diferentes, e a troca de fornecedor nao deve exigir
 * alteracoes significativas nas regras centrais. Checkout e pedido conhecem apenas {@code GatewayPagamento}.
 *
 * <p>Implementacoes concretas (adaptadores): etapa E6 (modulo {@code pagamento}). Nao ha implementacao na
 * fundacao.
 *
 * <p>Contrato provisorio (onda 0): mudancas via PR revisado pelos consumidores e registradas em docs/03-arquitetura.md.
 *
 * <p>Ver docs/04-design-patterns.md, secao 3.5 (Adapter: gateways de pagamento).
 */
public interface GatewayPagamento {

    boolean suporta(FormaPagamento forma);

    ResultadoCobranca cobrar(SolicitacaoCobranca solicitacao);

    ResultadoEstorno estornar(SolicitacaoEstorno solicitacao);
}
