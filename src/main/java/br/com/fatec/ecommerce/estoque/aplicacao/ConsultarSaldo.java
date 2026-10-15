package br.com.fatec.ecommerce.estoque.aplicacao;

import br.com.fatec.ecommerce.estoque.dominio.SaldoEstoque;
import br.com.fatec.ecommerce.estoque.dominio.SaldoEstoqueRepository;
import br.com.fatec.ecommerce.estoque.dominio.SaldoNaoEncontradoException;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Consultas de saldo: de uma variacao, de varias (ex.: disponibilidade de um combo) e abaixo de um limite. */
@Service
@Transactional(readOnly = true)
public class ConsultarSaldo {

    private final SaldoEstoqueRepository saldos;

    public ConsultarSaldo(SaldoEstoqueRepository saldos) {
        this.saldos = saldos;
    }

    public SaldoEstoque porVariacao(Long variacaoId) {
        return saldos.buscarPorVariacaoId(variacaoId).orElseThrow(() -> new SaldoNaoEncontradoException(variacaoId));
    }

    /** Saldos existentes, ordenados pela variacao; variacoes sem saldo ficam de fora. */
    public List<SaldoEstoque> porVariacoes(Collection<Long> variacaoIds) {
        return saldos.buscarPorVariacaoIds(variacaoIds);
    }

    /** Saldos com disponivel estritamente menor que o limite, ordenados pela variacao. */
    public List<SaldoEstoque> abaixoDe(int limite) {
        return saldos.listarComDisponivelAbaixoDe(limite);
    }
}
