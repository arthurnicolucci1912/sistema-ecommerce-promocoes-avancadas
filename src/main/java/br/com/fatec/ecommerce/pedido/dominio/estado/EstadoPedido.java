package br.com.fatec.ecommerce.pedido.dominio.estado;

import br.com.fatec.ecommerce.compartilhado.dominio.EventoDominio;
import br.com.fatec.ecommerce.pedido.dominio.Responsavel;
import br.com.fatec.ecommerce.pedido.dominio.SituacaoPedido;
import br.com.fatec.ecommerce.pedido.dominio.TransicaoInvalidaException;
import java.util.Optional;

/**
 * Situacao do pedido como objeto: cada implementacao aceita apenas as proprias transicoes e devolve o proximo
 * estado; as demais operacoes lancam {@link TransicaoInvalidaException}. Os estados nao guardam dados.
 *
 * <p>Pattern: State - papel: estado (interface). O {@code Pedido} (contexto) delega a este contrato todas as
 * operacoes do ciclo de vida.
 *
 * <p>Problema do enunciado: o pedido tem um ciclo de vida com transicoes validas definidas pela equipe, e o
 * cancelamento so vale em certos estados. Sem o pattern, as regras ficariam em cadeias de condicionais.
 *
 * <p>Ver docs/04-design-patterns.md, secao 3.4 (State: ciclo de vida do pedido).
 */
public interface EstadoPedido {

    /** Situacao persistida que corresponde a este estado. */
    SituacaoPedido situacao();

    EstadoPedido aguardarPagamento();

    EstadoPedido confirmarPagamento();

    EstadoPedido recusarPagamento();

    EstadoPedido iniciarPreparacao();

    EstadoPedido enviar();

    EstadoPedido entregar();

    /** A regra de quem pode cancelar fica no estado (ex.: pedido pago so e cancelado por responsavel administrativo). */
    EstadoPedido cancelar(Responsavel responsavel);

    /** Evento que anuncia a chegada a este estado; vazio quando a chegada nao anuncia nada (estado inicial). */
    Optional<EventoDominio> eventoDeChegada(DadosDaTransicao transicao);
}
