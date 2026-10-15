package br.com.fatec.ecommerce.pedido.infra.persistencia;

import br.com.fatec.ecommerce.compartilhado.dominio.ConflitoException;
import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import br.com.fatec.ecommerce.compartilhado.dominio.Pagina;
import br.com.fatec.ecommerce.pedido.dominio.Pedido;
import br.com.fatec.ecommerce.pedido.dominio.PedidoRepository;
import br.com.fatec.ecommerce.pedido.dominio.ResumoPedido;
import br.com.fatec.ecommerce.pedido.dominio.SituacaoPedido;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

/** Adaptador JPA da porta {@link PedidoRepository}. */
@Repository
class PedidoRepositoryJpa implements PedidoRepository {

    private final PedidoJpaRepository jpa;

    PedidoRepositoryJpa(PedidoJpaRepository jpa) {
        this.jpa = jpa;
    }

    /**
     * O flush imediato faz a disputa pelo mesmo pedido (lock otimista, coluna {@code versao}) aparecer aqui:
     * a segunda gravacao a partir da mesma versao vira {@link ConflitoException} (409).
     */
    @Override
    public Pedido salvar(Pedido pedido) {
        try {
            return jpa.saveAndFlush(pedido);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new ConflitoException("O pedido foi alterado por outra operação. Consulte-o e tente novamente.");
        }
    }

    @Override
    public Optional<Pedido> buscarPorId(Long id) {
        return jpa.findById(id);
    }

    @Override
    public Optional<Pedido> buscarPorIdECliente(Long id, Long clienteId) {
        return jpa.findByIdAndClienteId(id, clienteId);
    }

    @Override
    public Pagina<ResumoPedido> listarResumos(SituacaoPedido situacao, int pagina, int tamanho) {
        PageRequest pedido = maisRecentesPrimeiro(pagina, tamanho);
        return resumos(situacao == null ? jpa.findBy(pedido) : jpa.findBySituacao(situacao, pedido));
    }

    @Override
    public Pagina<ResumoPedido> listarResumosDoCliente(Long clienteId, int pagina, int tamanho) {
        return resumos(jpa.findByClienteId(clienteId, maisRecentesPrimeiro(pagina, tamanho)));
    }

    @Override
    public boolean existeDoCliente(Long clienteId) {
        return jpa.existsByClienteId(clienteId);
    }

    private static PageRequest maisRecentesPrimeiro(int pagina, int tamanho) {
        return PageRequest.of(pagina, tamanho, Sort.by(Sort.Order.desc("criadoEm"), Sort.Order.desc("id")));
    }

    private static Pagina<ResumoPedido> resumos(Page<PedidoJpaRepository.ResumoProjecao> pagina) {
        return new Pagina<>(pagina.getContent().stream()
                .map(p -> new ResumoPedido(p.getId(), p.getClienteId(), p.getSituacao(), Dinheiro.de(p.getTotal()),
                        p.getCriadoEm()))
                .toList(), pagina.getNumber(), pagina.getSize(), pagina.getTotalElements());
    }
}
