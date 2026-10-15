package br.com.fatec.ecommerce.estoque.aplicacao;

import br.com.fatec.ecommerce.catalogo.dominio.evento.VariacaoCadastrada;
import br.com.fatec.ecommerce.estoque.dominio.SaldoEstoqueRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reage ao catalogo: toda variacao cadastrada ganha saldo zero, sem o estoque consultar o catalogo. Idempotente
 * (um saldo que ja existe nao e alterado). Roda sincrono, na transacao de quem cadastrou a variacao (participa dela).
 *
 * <p>Pattern: Observer - papel: ouvinte (observer) de {@link VariacaoCadastrada}; o catalogo publica o evento sem
 * conhecer o estoque.
 *
 * <p>Ver docs/04-design-patterns.md, secao 3.6 (Observer: eventos de dominio).
 */
@Component
@Transactional
public class OuvinteDeCatalogoDoEstoque {

    private final SaldoEstoqueRepository saldos;

    public OuvinteDeCatalogoDoEstoque(SaldoEstoqueRepository saldos) {
        this.saldos = saldos;
    }

    @EventListener
    public void ao(VariacaoCadastrada evento) {
        saldos.criarSeAusente(evento.variacaoId());
    }
}
