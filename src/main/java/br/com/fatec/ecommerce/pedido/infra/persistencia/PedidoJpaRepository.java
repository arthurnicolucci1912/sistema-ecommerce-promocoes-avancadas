package br.com.fatec.ecommerce.pedido.infra.persistencia;

import br.com.fatec.ecommerce.pedido.dominio.Pedido;
import br.com.fatec.ecommerce.pedido.dominio.SituacaoPedido;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface PedidoJpaRepository extends JpaRepository<Pedido, Long> {

    Optional<Pedido> findByIdAndClienteId(Long id, Long clienteId);

    boolean existsByClienteId(Long clienteId);

    @org.springframework.data.jpa.repository.Query(value = "SELECT EXISTS (SELECT 1 FROM pedido p JOIN pedido_historico h ON h.pedido_id=p.id WHERE p.cliente_id=:clienteId AND h.para='PAGO' AND h.ocorrido_em<=:momento)", nativeQuery = true)
    boolean existePagamentoAprovadoAte(Long clienteId, Instant momento);

    /** Projecoes fechadas: so as colunas do resumo sao lidas, sem as colecoes do agregado. */
    Page<ResumoProjecao> findBy(Pageable pageable);

    Page<ResumoProjecao> findBySituacao(SituacaoPedido situacao, Pageable pageable);

    Page<ResumoProjecao> findByClienteId(Long clienteId, Pageable pageable);

    interface ResumoProjecao {

        Long getId();

        Long getClienteId();

        SituacaoPedido getSituacao();

        BigDecimal getTotal();

        Instant getCriadoEm();
    }
}
