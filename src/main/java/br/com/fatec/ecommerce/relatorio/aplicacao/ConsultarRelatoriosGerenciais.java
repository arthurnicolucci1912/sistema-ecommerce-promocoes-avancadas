package br.com.fatec.ecommerce.relatorio.aplicacao;

import br.com.fatec.ecommerce.relatorio.dominio.ConsultarRelatorios;
import br.com.fatec.ecommerce.relatorio.dominio.DescontoPorCampanha;
import br.com.fatec.ecommerce.relatorio.dominio.FiltroProdutosMaisVendidos;
import br.com.fatec.ecommerce.relatorio.dominio.PeriodoRelatorio;
import br.com.fatec.ecommerce.relatorio.dominio.ProdutoMaisVendido;
import br.com.fatec.ecommerce.relatorio.dominio.ResumoVendas;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de UC-A10 (somente leitura). Vendas e produtos: ADMIN e OPERADOR_COMERCIAL; campanhas também
 * PROMOCOES. A restrição vale também quando o serviço é chamado fora do controller. Sem pattern próprio: não há
 * ponto de variação (ver docs/04-design-patterns.md).
 */
@Service
@PreAuthorize("hasAnyRole('ADMIN','OPERADOR_COMERCIAL')")
@Transactional(readOnly = true)
public class ConsultarRelatoriosGerenciais {
    private final ConsultarRelatorios relatorios;

    public ConsultarRelatoriosGerenciais(ConsultarRelatorios relatorios) {
        this.relatorios = relatorios;
    }

    public ResumoVendas vendas(PeriodoRelatorio periodo) {
        return relatorios.resumoDeVendas(periodo);
    }

    public List<ProdutoMaisVendido> produtosMaisVendidos(FiltroProdutosMaisVendidos filtro) {
        return relatorios.produtosMaisVendidos(filtro);
    }

    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR_COMERCIAL','PROMOCOES')")
    public List<DescontoPorCampanha> campanhas(PeriodoRelatorio periodo) {
        return relatorios.descontosPorCampanha(periodo);
    }
}
