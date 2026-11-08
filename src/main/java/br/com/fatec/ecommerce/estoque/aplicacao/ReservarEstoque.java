package br.com.fatec.ecommerce.estoque.aplicacao;

import br.com.fatec.ecommerce.compartilhado.dominio.ConflitoException;
import br.com.fatec.ecommerce.compartilhado.dominio.DadoInvalidoException;
import br.com.fatec.ecommerce.estoque.dominio.ComponenteAReservar;
import br.com.fatec.ecommerce.estoque.dominio.EstoqueInsuficienteException;
import br.com.fatec.ecommerce.estoque.dominio.ReservaEstoque;
import br.com.fatec.ecommerce.estoque.dominio.ReservaEstoqueRepository;
import br.com.fatec.ecommerce.estoque.dominio.SaldoEstoqueRepository;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: reserva, para um pedido, as quantidades de um conjunto de componentes de estoque, tudo ou nada
 * (RN-21). As quantidades sao somadas por variacao e processadas em ordem de variacao (evita deadlock entre
 * pedidos que disputam as mesmas variacoes). Cada variacao usa uma atualizacao condicional atomica
 * ({@code disponivel >= quantidade}); se alguma nao afetar linha, lanca {@link EstoqueInsuficienteException} e a
 * transacao desfaz as reservas anteriores. Um pedido so pode reservar uma vez. Roda na transacao do chamador
 * (o checkout), se houver.
 */
@Service
@Transactional
public class ReservarEstoque {

    private final SaldoEstoqueRepository saldos;
    private final ReservaEstoqueRepository reservas;

    public ReservarEstoque(SaldoEstoqueRepository saldos, ReservaEstoqueRepository reservas) {
        this.saldos = saldos;
        this.reservas = reservas;
    }

    public void para(Long pedidoId, Collection<ComponenteAReservar> componentes) {
        if (pedidoId == null) {
            throw new DadoInvalidoException("O pedido da reserva é obrigatório.");
        }
        if (componentes == null || componentes.isEmpty()) {
            throw new DadoInvalidoException("A reserva precisa de ao menos um componente.");
        }
        if (reservas.existePorPedido(pedidoId)) {
            throw new ConflitoException("O pedido " + pedidoId + " já tem reserva de estoque.");
        }
        Map<Long, Integer> porVariacao = new TreeMap<>();
        try {
            componentes.forEach(c -> porVariacao.merge(c.variacaoId(), c.quantidade(), Math::addExact));
        } catch (ArithmeticException estouro) {
            throw new DadoInvalidoException("A quantidade total da reserva excede o limite permitido.");
        }

        Instant agora = Instant.now();
        porVariacao.forEach((variacaoId, quantidade) -> {
            if (!saldos.reservar(variacaoId, quantidade)) {
                throw new EstoqueInsuficienteException(variacaoId, saldos.disponivelDe(variacaoId).orElse(0));
            }
            reservas.salvar(ReservaEstoque.ativa(pedidoId, new ComponenteAReservar(variacaoId, quantidade), agora));
        });
    }
}
