package br.com.fatec.ecommerce.estoque.infra.persistencia;

import br.com.fatec.ecommerce.estoque.dominio.ReservaEstoque;
import br.com.fatec.ecommerce.estoque.dominio.SituacaoReserva;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ReservaEstoqueJpaRepository extends JpaRepository<ReservaEstoque, Long> {

    boolean existsByPedidoId(Long pedidoId);

    List<ReservaEstoque> findByPedidoIdAndSituacaoOrderByVariacaoIdAsc(Long pedidoId, SituacaoReserva situacao);

    @Modifying
    @Query(value = "update reserva_estoque set situacao = :para, atualizado_em = now() "
            + "where id = :id and situacao = :de", nativeQuery = true)
    int transitar(@Param("id") Long reservaId, @Param("de") String de, @Param("para") String para);
}
