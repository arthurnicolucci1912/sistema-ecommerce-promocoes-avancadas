package br.com.fatec.ecommerce.pedido.dominio.estado;

import br.com.fatec.ecommerce.compartilhado.dominio.EventoDominio;
import br.com.fatec.ecommerce.pedido.dominio.OperacaoPedido;
import br.com.fatec.ecommerce.pedido.dominio.Responsavel;
import br.com.fatec.ecommerce.pedido.dominio.TransicaoInvalidaException;
import java.util.Optional;

/**
 * Comportamento padrao dos estados: toda operacao e rejeitada com {@link TransicaoInvalidaException}, informando
 * o estado atual e a operacao. Cada estado concreto sobrescreve somente as transicoes que admite.
 *
 * <p>Pattern: State - papel: estado base (rejeicao padrao num unico lugar). Os estados finais nao sobrescrevem
 * nenhuma operacao.
 *
 * <p>Ver docs/04-design-patterns.md, secao 3.4 (State: ciclo de vida do pedido).
 */
public abstract class EstadoPedidoBase implements EstadoPedido {

    @Override
    public EstadoPedido aguardarPagamento() {
        throw rejeitar(OperacaoPedido.AGUARDAR_PAGAMENTO);
    }

    @Override
    public EstadoPedido confirmarPagamento() {
        throw rejeitar(OperacaoPedido.CONFIRMAR_PAGAMENTO);
    }

    @Override
    public EstadoPedido recusarPagamento() {
        throw rejeitar(OperacaoPedido.RECUSAR_PAGAMENTO);
    }

    @Override
    public EstadoPedido iniciarPreparacao() {
        throw rejeitar(OperacaoPedido.INICIAR_PREPARACAO);
    }

    @Override
    public EstadoPedido enviar() {
        throw rejeitar(OperacaoPedido.ENVIAR);
    }

    @Override
    public EstadoPedido entregar() {
        throw rejeitar(OperacaoPedido.ENTREGAR);
    }

    @Override
    public EstadoPedido cancelar(Responsavel responsavel) {
        throw rejeitar(OperacaoPedido.CANCELAR);
    }

    @Override
    public Optional<EventoDominio> eventoDeChegada(DadosDaTransicao transicao) {
        return Optional.empty();
    }

    /** Para estados que cancelam so por responsavel administrativo: rejeita os demais como transicao invalida. */
    protected final void exigirResponsavelAdministrativo(Responsavel responsavel) {
        if (!responsavel.ehAdministrativo()) {
            throw new TransicaoInvalidaException(situacao(), OperacaoPedido.CANCELAR,
                    "Um pedido em " + situacao() + " só pode ser cancelado por um responsável administrativo.");
        }
    }

    private TransicaoInvalidaException rejeitar(OperacaoPedido operacao) {
        return new TransicaoInvalidaException(situacao(), operacao);
    }
}
