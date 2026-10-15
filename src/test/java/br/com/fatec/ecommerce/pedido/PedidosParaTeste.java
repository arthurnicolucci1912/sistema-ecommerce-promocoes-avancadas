package br.com.fatec.ecommerce.pedido;

import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import br.com.fatec.ecommerce.pedido.dominio.AlvoDoDesconto;
import br.com.fatec.ecommerce.pedido.dominio.DadosNovoPedido;
import br.com.fatec.ecommerce.pedido.dominio.EnderecoEntrega;
import br.com.fatec.ecommerce.pedido.dominio.OrigemDesconto;
import br.com.fatec.ecommerce.pedido.dominio.Pedido;
import br.com.fatec.ecommerce.pedido.dominio.Responsavel;
import br.com.fatec.ecommerce.pedido.dominio.SituacaoPedido;
import java.time.Instant;
import java.util.List;

/**
 * Apoio aos testes do modulo pedido: monta snapshots validos e leva um pedido em memoria a uma situacao.
 * Valores: 2 x R$ 100,00 + 1 x R$ 220,00 = R$ 420,00; desconto de R$ 20,00 e frete de R$ 15,00 dao total de R$ 415,00.
 */
public final class PedidosParaTeste {

    public static final Responsavel ADMIN = Responsavel.administrativo(1L);
    public static final Responsavel CLIENTE = Responsavel.cliente(2L);
    public static final Instant MOMENTO = Instant.parse("2026-01-10T12:00:00Z");

    private PedidosParaTeste() {
    }

    public static EnderecoEntrega enderecoPadrao() {
        return new EnderecoEntrega("Casa", "13010000", "Rua das Flores", "10", "Casa 2", "Centro", "Campinas", "SP");
    }

    public static DadosNovoPedido.Item itemCamiseta() {
        return new DadosNovoPedido.Item("CAM-001-M", "Camiseta", "Azul, M", 2, Dinheiro.de("100.00"));
    }

    public static DadosNovoPedido.Item itemTenis(String preco) {
        return new DadosNovoPedido.Item("TEN-042", "Tênis", "Preto, 42", 1, Dinheiro.de(preco));
    }

    /** Snapshot valido (ver valores na documentacao da classe). */
    public static DadosNovoPedido validos(Long clienteId) {
        return validos(clienteId, enderecoPadrao(), itemTenis("220.00"));
    }

    /** Snapshot valido com o endereco e o segundo item informados; recalcula os totais. */
    public static DadosNovoPedido validos(Long clienteId, EnderecoEntrega endereco, DadosNovoPedido.Item segundoItem) {
        List<DadosNovoPedido.Item> itens = List.of(itemCamiseta(), segundoItem);
        Dinheiro subtotal = itens.stream().map(i -> i.precoUnitario().multiplicar(i.quantidade()))
                .reduce(Dinheiro.zero(), Dinheiro::somar);
        Dinheiro desconto = Dinheiro.de("20.00");
        Dinheiro frete = Dinheiro.de("15.00");
        return new DadosNovoPedido(clienteId, endereco, itens,
                List.of(new DadosNovoPedido.Desconto("Promo de inverno", OrigemDesconto.PROMOCAO,
                        AlvoDoDesconto.ITENS, desconto)),
                frete, subtotal, desconto, subtotal.subtrair(desconto).somar(frete));
    }

    /** Copia do snapshot valido com o total informado (para testar totais inconsistentes). */
    public static DadosNovoPedido comTotal(DadosNovoPedido base, Dinheiro total) {
        return new DadosNovoPedido(base.clienteId(), base.endereco(), base.itens(), base.descontos(), base.frete(),
                base.subtotal(), base.totalDescontos(), total);
    }

    /** Pedido em memoria (sem id, sem eventos pendentes) levado a situacao pedida pelo caminho valido mais curto. */
    public static Pedido emSituacao(SituacaoPedido alvo, Long clienteId) {
        Pedido pedido = Pedido.criar(validos(clienteId), MOMENTO);
        switch (alvo) {
            case CRIADO -> { }
            case AGUARDANDO_PAGAMENTO -> pedido.aguardarPagamento(Responsavel.sistema(), MOMENTO);
            case PAGAMENTO_RECUSADO -> {
                pedido.aguardarPagamento(Responsavel.sistema(), MOMENTO);
                pedido.recusarPagamento(Responsavel.sistema(), null, MOMENTO);
            }
            case CANCELADO -> pedido.cancelar(CLIENTE, "desistência", MOMENTO);
            default -> {
                pedido.aguardarPagamento(Responsavel.sistema(), MOMENTO);
                pedido.confirmarPagamento(Responsavel.sistema(), null, MOMENTO);
                if (alvo != SituacaoPedido.PAGO) {
                    pedido.iniciarPreparacao(ADMIN, MOMENTO);
                }
                if (alvo == SituacaoPedido.ENVIADO || alvo == SituacaoPedido.ENTREGUE) {
                    pedido.enviar(ADMIN, MOMENTO);
                }
                if (alvo == SituacaoPedido.ENTREGUE) {
                    pedido.entregar(ADMIN, MOMENTO);
                }
            }
        }
        pedido.consumirEventos();
        return pedido;
    }
}
