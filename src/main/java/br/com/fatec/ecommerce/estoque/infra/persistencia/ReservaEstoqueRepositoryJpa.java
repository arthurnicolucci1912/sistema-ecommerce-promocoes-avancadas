package br.com.fatec.ecommerce.estoque.infra.persistencia;

import br.com.fatec.ecommerce.compartilhado.dominio.ConflitoException;
import br.com.fatec.ecommerce.estoque.dominio.ReservaEstoque;
import br.com.fatec.ecommerce.estoque.dominio.ReservaEstoqueRepository;
import br.com.fatec.ecommerce.estoque.dominio.SituacaoReserva;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

/** Adaptador JPA da porta {@link ReservaEstoqueRepository}. */
@Repository
class ReservaEstoqueRepositoryJpa implements ReservaEstoqueRepository {

    private final ReservaEstoqueJpaRepository jpa;

    ReservaEstoqueRepositoryJpa(ReservaEstoqueJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public ReservaEstoque salvar(ReservaEstoque reserva) {
        try {
            return jpa.saveAndFlush(reserva);
        } catch (DataIntegrityViolationException e) {
            throw new ConflitoException("O pedido já tem reserva de estoque para esta variação.");
        }
    }

    @Override
    public boolean existePorPedido(Long pedidoId) {
        return jpa.existsByPedidoId(pedidoId);
    }

    @Override
    public List<ReservaEstoque> listarPorPedidoESituacao(Long pedidoId, SituacaoReserva situacao) {
        return jpa.findByPedidoIdAndSituacaoOrderByVariacaoIdAsc(pedidoId, situacao);
    }

    @Override
    public boolean transitar(Long reservaId, SituacaoReserva de, SituacaoReserva para) {
        return jpa.transitar(reservaId, de.name(), para.name()) > 0;
    }
}
