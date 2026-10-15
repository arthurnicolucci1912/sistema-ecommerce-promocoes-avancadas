package br.com.fatec.ecommerce.preco.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import br.com.fatec.ecommerce.promocao.dominio.ContextoPreco;
import br.com.fatec.ecommerce.promocao.dominio.DescontoAplicado;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Estado mutavel do calculo de preco, passado de etapa em etapa pelo pipeline ({@link EtapaPreco}).
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

    public Dinheiro total() {
        return total;
    }

    public void definirTotal(Dinheiro total) {
        this.total = total;
    }
}
