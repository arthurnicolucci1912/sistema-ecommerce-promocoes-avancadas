package br.com.fatec.ecommerce.estoque.infra.persistencia;

import br.com.fatec.ecommerce.estoque.dominio.SaldoEstoque;
import br.com.fatec.ecommerce.estoque.dominio.SaldoEstoqueRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** Adaptador JPA da porta {@link SaldoEstoqueRepository}: movimentos por atualizacoes condicionais nativas. */
@Repository
class SaldoEstoqueRepositoryJpa implements SaldoEstoqueRepository {

    private final SaldoEstoqueJpaRepository jpa;

    SaldoEstoqueRepositoryJpa(SaldoEstoqueJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void criarSeAusente(Long variacaoId) {
        jpa.criarSeAusente(variacaoId);
    }

    @Override
    public Optional<SaldoEstoque> buscarPorVariacaoId(Long variacaoId) {
        return jpa.findById(variacaoId);
    }

    @Override
    public List<SaldoEstoque> buscarPorVariacaoIds(Collection<Long> variacaoIds) {
        if (variacaoIds.isEmpty()) {
            return List.of();
        }
        return jpa.findByVariacaoIdInOrderByVariacaoIdAsc(variacaoIds);
    }

    @Override
    public List<SaldoEstoque> listarComDisponivelAbaixoDe(int limite) {
        return jpa.findByDisponivelLessThanOrderByVariacaoIdAsc(limite);
    }

    @Override
    public Optional<Integer> disponivelDe(Long variacaoId) {
        return jpa.disponivelDe(variacaoId);
    }

    @Override
    public boolean reservar(Long variacaoId, int quantidade) {
        return jpa.reservar(variacaoId, quantidade) > 0;
    }

    @Override
    public void devolverReservado(Long variacaoId, int quantidade) {
        jpa.devolverReservado(variacaoId, quantidade);
    }

    @Override
    public void baixarReservado(Long variacaoId, int quantidade) {
        jpa.baixarReservado(variacaoId, quantidade);
    }

    @Override
    public void reporDisponivel(Long variacaoId, int quantidade) {
        jpa.reporDisponivel(variacaoId, quantidade);
    }

    @Override
    public boolean ajustarDisponivel(Long variacaoId, int delta) {
        return jpa.ajustarDisponivel(variacaoId, delta) > 0;
    }
}
