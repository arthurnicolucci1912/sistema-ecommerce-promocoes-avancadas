package br.com.fatec.ecommerce.estoque.aplicacao;

import br.com.fatec.ecommerce.compartilhado.aplicacao.PublicarOperacaoAdministrativa;
import br.com.fatec.ecommerce.compartilhado.dominio.ConflitoException;
import br.com.fatec.ecommerce.compartilhado.dominio.DadoInvalidoException;
import br.com.fatec.ecommerce.estoque.dominio.SaldoEstoque;
import br.com.fatec.ecommerce.estoque.dominio.SaldoEstoqueRepository;
import br.com.fatec.ecommerce.estoque.dominio.SaldoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>Observer administrativo: publica snapshots permitidos na transação original; falha da trilha reverte a operação.
 *
 * Caso de uso: ajuste administrativo do disponivel (entrada de mercadoria, perda, inventario) somando ou
 * subtraindo uma quantidade, com motivo obrigatorio. Usa a mesma tecnica da reserva: atualizacao condicional
 * {@code disponivel + delta >= 0}; um ajuste que deixaria o disponivel negativo e rejeitado (409). O saldo precisa
 * existir (404 caso contrario): ele nasce com a variacao, por {@code VariacaoCadastrada}. Ajuste zero e invalido.
 */
@Service
@Transactional
public class AjustarEstoque {

    private final PublicarOperacaoAdministrativa auditoria;
    private final SaldoEstoqueRepository saldos;

    public AjustarEstoque(SaldoEstoqueRepository saldos, PublicarOperacaoAdministrativa auditoria) {
        this.auditoria = auditoria;
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
        boolean ajustado;
        try {
            ajustado = saldos.ajustarDisponivel(variacaoId, delta);
        } catch (org.springframework.dao.DataIntegrityViolationException estouro) {
            // disponivel + delta acima do int da coluna: erro de dominio, nao 500
            throw new ConflitoException("O ajuste de " + delta + " estouraria o limite do estoque.");
        }
        if (!ajustado) {
            int disponivel = saldos.disponivelDe(variacaoId).orElseThrow(() -> new SaldoNaoEncontradoException(variacaoId));
            throw new ConflitoException("O ajuste de " + delta + " deixaria o estoque negativo (disponível "
                    + disponivel + ").");
        }
        auditoria.publicar("ESTOQUE_AJUSTADO", "VARIACAO", variacaoId, null,
                java.util.Map.of("quantidade", delta, "motivo", motivo.trim()));
        return saldos.buscarPorVariacaoId(variacaoId).orElseThrow(() -> new SaldoNaoEncontradoException(variacaoId));
    }
}
