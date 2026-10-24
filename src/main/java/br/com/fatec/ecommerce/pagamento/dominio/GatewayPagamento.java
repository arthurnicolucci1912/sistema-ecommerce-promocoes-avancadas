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
 * <p>Implementacoes concretas na E6: AdaptadorCartaoSimulado, AdaptadorPixSimulado e
 * AdaptadorBoletoSimulado, em pagamento.infra.gateway. Identidade do fornecedor e persistida para estornos.
 *
 * <p>Contrato provisorio (onda 0): mudancas via PR revisado pelos consumidores e registradas em docs/03-arquitetura.md.
 *
 * <p>Ver docs/04-design-patterns.md, secao 3.5 (Adapter: gateways de pagamento).
 */
public interface GatewayPagamento {

    /** Identidade estavel: cobrancas antigas continuam usando este fornecedor no estorno. */
    String fornecedor();

    boolean suporta(FormaPagamento forma);

    ResultadoCobranca cobrar(SolicitacaoCobranca solicitacao);

    ResultadoEstorno estornar(SolicitacaoEstorno solicitacao);
}
