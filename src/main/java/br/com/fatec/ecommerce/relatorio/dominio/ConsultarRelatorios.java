package br.com.fatec.ecommerce.relatorio.dominio;

import java.util.List;

/**
 * Porta de leitura dos relatórios gerenciais sobre os snapshots dos pedidos. Os três relatórios usam o mesmo critério
 * de venda (pedido pago no período e não cancelado). Somente consultas: nenhuma operação grava dados.
 */
public interface ConsultarRelatorios {
    ResumoVendas resumoDeVendas(PeriodoRelatorio periodo);

    List<ProdutoMaisVendido> produtosMaisVendidos(FiltroProdutosMaisVendidos filtro);

    List<DescontoPorCampanha> descontosPorCampanha(PeriodoRelatorio periodo);
}
