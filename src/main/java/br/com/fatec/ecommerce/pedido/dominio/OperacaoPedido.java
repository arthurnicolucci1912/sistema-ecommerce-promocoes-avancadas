package br.com.fatec.ecommerce.pedido.dominio;

/** Operacoes do ciclo de vida que um {@code EstadoPedido} pode aceitar ou rejeitar. */
public enum OperacaoPedido {
    AGUARDAR_PAGAMENTO,
    CONFIRMAR_PAGAMENTO,
    RECUSAR_PAGAMENTO,
    INICIAR_PREPARACAO,
    ENVIAR,
    ENTREGAR,
    CANCELAR
}
