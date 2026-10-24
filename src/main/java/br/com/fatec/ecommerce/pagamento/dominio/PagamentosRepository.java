package br.com.fatec.ecommerce.pagamento.dominio;

import java.util.Optional;

/** Porta de persistencia. Escritas exigem transacao e bloqueio previo do pedido correspondente. */
public interface PagamentosRepository {
    Optional<Cobranca> buscarPorPedido(Long pedidoId);
    Optional<Cobranca> buscarPorTransacao(String idTransacaoExterna);
    void criar(Cobranca cobranca);
    void atualizar(Cobranca cobranca);
    Optional<ConfirmacaoPagamento> buscarConfirmacao(String idTransacaoExterna);
    /** Insercao idempotente sem invalidar a transacao em caso de duplicidade. */
    boolean registrar(ConfirmacaoPagamento confirmacao);
    Optional<Estorno> buscarEstorno(Long pedidoId);
    void salvar(Estorno estorno);
}
