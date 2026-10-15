package br.com.fatec.ecommerce.estoque.dominio;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Porta de persistencia dos saldos. Cada operacao de movimento e uma atualizacao condicional atomica (uma ida ao
 * banco), que devolve se afetou o saldo; nao ha leitura-modificacao-gravacao.
 */
public interface SaldoEstoqueRepository {

    /** Cria o saldo zerado se ainda nao existir (idempotente). */
    void criarSeAusente(Long variacaoId);

    Optional<SaldoEstoque> buscarPorVariacaoId(Long variacaoId);

    List<SaldoEstoque> buscarPorVariacaoIds(Collection<Long> variacaoIds);

    /** Variacoes com disponivel menor que o limite, ordenadas pela variacao. */
    List<SaldoEstoque> listarComDisponivelAbaixoDe(int limite);

    /** Le o disponivel direto do banco (ignora o cache da sessao); vazio se nao houver saldo. */
    Optional<Integer> disponivelDe(Long variacaoId);

    /** Passa a quantidade do disponivel para o reservado, se houver disponivel suficiente. */
    boolean reservar(Long variacaoId, int quantidade);

    /** Devolve ao disponivel a quantidade que estava reservada (liberacao). */
    void devolverReservado(Long variacaoId, int quantidade);

    /** Tira a quantidade do reservado, sem devolver ao disponivel (baixa). */
    void baixarReservado(Long variacaoId, int quantidade);

    /** Soma a quantidade ao disponivel (reposicao de uma baixa). */
    void reporDisponivel(Long variacaoId, int quantidade);

    /** Soma o delta (positivo ou negativo) ao disponivel, se o resultado nao ficar negativo. */
    boolean ajustarDisponivel(Long variacaoId, int delta);
}
