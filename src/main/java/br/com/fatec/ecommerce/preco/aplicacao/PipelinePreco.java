package br.com.fatec.ecommerce.preco.aplicacao;

import br.com.fatec.ecommerce.preco.dominio.CalculoPreco;
import br.com.fatec.ecommerce.preco.dominio.EtapaPreco;
import br.com.fatec.ecommerce.promocao.dominio.ContextoPreco;
import java.util.List;

/** Chain of Responsibility: coordenador; a ordem explicita permite inserir handlers sem altera-los. */
public class PipelinePreco {
    private final List<EtapaPreco> etapas;

    public PipelinePreco(List<EtapaPreco> etapas) {
        this.etapas = List.copyOf(etapas);
    }

    public CalculoPreco calcular(ContextoPreco contexto) {
        CalculoPreco calculo = new CalculoPreco(contexto);
        etapas.forEach(etapa -> etapa.processar(calculo));
        return calculo;
    }
}
