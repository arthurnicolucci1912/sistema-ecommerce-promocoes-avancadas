package br.com.fatec.ecommerce.estoque.infra.persistencia;

import br.com.fatec.ecommerce.estoque.dominio.SaldoEstoque;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SaldoEstoqueJpaRepository extends JpaRepository<SaldoEstoque, Long> {

    List<SaldoEstoque> findByVariacaoIdInOrderByVariacaoIdAsc(Collection<Long> variacaoIds);

    List<SaldoEstoque> findByDisponivelLessThanOrderByVariacaoIdAsc(int limite);

    @Query(value = "select disponivel from saldo_estoque where variacao_id = :v", nativeQuery = true)
    Optional<Integer> disponivelDe(@Param("v") Long variacaoId);

    @Modifying
    @Query(value = "insert into saldo_estoque (variacao_id, disponivel, reservado) values (:v, 0, 0) "
            + "on conflict (variacao_id) do nothing", nativeQuery = true)
    int criarSeAusente(@Param("v") Long variacaoId);

    @Modifying
    @Query(value = "update saldo_estoque set disponivel = disponivel - :q, reservado = reservado + :q, "
            + "atualizado_em = now() where variacao_id = :v and disponivel >= :q", nativeQuery = true)
    int reservar(@Param("v") Long variacaoId, @Param("q") int quantidade);

    @Modifying
    @Query(value = "update saldo_estoque set disponivel = disponivel + :q, reservado = reservado - :q, "
            + "atualizado_em = now() where variacao_id = :v", nativeQuery = true)
    int devolverReservado(@Param("v") Long variacaoId, @Param("q") int quantidade);

    @Modifying
    @Query(value = "update saldo_estoque set reservado = reservado - :q, atualizado_em = now() "
            + "where variacao_id = :v", nativeQuery = true)
    int baixarReservado(@Param("v") Long variacaoId, @Param("q") int quantidade);

    @Modifying
    @Query(value = "update saldo_estoque set disponivel = disponivel + :q, atualizado_em = now() "
            + "where variacao_id = :v", nativeQuery = true)
    int reporDisponivel(@Param("v") Long variacaoId, @Param("q") int quantidade);

    @Modifying
    @Query(value = "update saldo_estoque set disponivel = disponivel + :delta, atualizado_em = now() "
            + "where variacao_id = :v and disponivel + :delta >= 0", nativeQuery = true)
    int ajustarDisponivel(@Param("v") Long variacaoId, @Param("delta") int delta);
}
