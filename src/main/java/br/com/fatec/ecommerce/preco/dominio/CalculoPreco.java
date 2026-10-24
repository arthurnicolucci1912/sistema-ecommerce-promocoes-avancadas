package br.com.fatec.ecommerce.preco.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import br.com.fatec.ecommerce.promocao.dominio.ContextoPreco;
import br.com.fatec.ecommerce.promocao.dominio.DescontoAplicado;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Chain of Responsibility: contexto mutavel do calculo, passado de handler em handler ({@link EtapaPreco}).
 * No fim, acumula o detalhamento (subtotal, descontos, frete e total) que o cliente enxerga.
 *
 * <p>Contrato provisorio (onda 0): mudancas via PR revisado pelos consumidores e registradas em docs/03-arquitetura.md.
 */
public class CalculoPreco {

    private final ContextoPreco contexto;
    private Dinheiro subtotal = Dinheiro.zero();
    private final List<DescontoAplicado> descontos = new ArrayList<>();
    private Dinheiro frete = Dinheiro.zero();
    private Dinheiro total = Dinheiro.zero();
    private br.com.fatec.ecommerce.promocao.dominio.SituacaoCupom cupom;

    public br.com.fatec.ecommerce.promocao.dominio.SituacaoCupom cupom() { return cupom; }
    public void definirCupom(br.com.fatec.ecommerce.promocao.dominio.SituacaoCupom cupom) { this.cupom = cupom; }

    public CalculoPreco(ContextoPreco contexto) {
        this.contexto = contexto;
    }

    public ContextoPreco contexto() {
        return contexto;
    }

    public Dinheiro subtotal() {
        return subtotal;
    }

    public void definirSubtotal(Dinheiro subtotal) {
        this.subtotal = subtotal;
    }

    public List<DescontoAplicado> descontos() {
        return Collections.unmodifiableList(descontos);
    }

    public void adicionarDesconto(DescontoAplicado desconto) {
        descontos.add(desconto);
    }

    public Dinheiro frete() {
        return frete;
    }

    public void definirFrete(Dinheiro frete) {
        this.frete = frete;
    }

    public Dinheiro freteLiquido() {
        Dinheiro abatimentos = descontos.stream().filter(d -> d.alvo() == br.com.fatec.ecommerce.promocao.dominio.AlvoDesconto.FRETE)
                .map(d -> d.valor()).reduce(Dinheiro.zero(), Dinheiro::somar);
        Dinheiro liquido = frete.subtrair(abatimentos);
        return liquido.ehNegativo() ? Dinheiro.zero() : liquido;
    }

    public Dinheiro total() {
        return total;
    }

    public void definirTotal(Dinheiro total) {
        this.total = total;
    }
}
