package br.com.fatec.ecommerce.estoque.aplicacao;

import br.com.fatec.ecommerce.compartilhado.dominio.ConflitoException;
import br.com.fatec.ecommerce.compartilhado.dominio.DadoInvalidoException;
import br.com.fatec.ecommerce.estoque.dominio.SaldoEstoque;
import br.com.fatec.ecommerce.estoque.dominio.SaldoEstoqueRepository;
import br.com.fatec.ecommerce.estoque.dominio.SaldoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: ajuste administrativo do disponivel (entrada de mercadoria, perda, inventario) somando ou
 * subtraindo uma quantidade, com motivo obrigatorio. Usa a mesma tecnica da reserva: atualizacao condicional
 * {@code disponivel + delta >= 0}; um ajuste que deixaria o disponivel negativo e rejeitado (409). O saldo precisa
 * existir (404 caso contrario): ele nasce com a variacao, por {@code VariacaoCadastrada}. Ajuste zero e invalido.
 */
@Service
@Transactional
public class AjustarEstoque {

    private final SaldoEstoqueRepository saldos;

    public AjustarEstoque(SaldoEstoqueRepository saldos) {
        this.saldos = saldos;
    }

    /** Devolve o saldo apos o ajuste. */
    public SaldoEstoque ajustar(Long variacaoId, int delta, String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new DadoInvalidoException("O motivo do ajuste é obrigatório.");
        }
        if (delta == 0) {
            throw new DadoInvalidoException("A quantidade do ajuste não pode ser zero.");
        }
        if (!saldos.ajustarDisponivel(variacaoId, delta)) {
            int disponivel = saldos.disponivelDe(variacaoId).orElseThrow(() -> new SaldoNaoEncontradoException(variacaoId));
            throw new ConflitoException("O ajuste de " + delta + " deixaria o estoque negativo (disponível "
                    + disponivel + ").");
        }
        return saldos.buscarPorVariacaoId(variacaoId).orElseThrow(() -> new SaldoNaoEncontradoException(variacaoId));
    }
}
