package br.com.fatec.ecommerce.carrinho.aplicacao;

import br.com.fatec.ecommerce.carrinho.dominio.Carrinho;
import br.com.fatec.ecommerce.catalogo.aplicacao.ItemVendavelInfo;
import br.com.fatec.ecommerce.compartilhado.dominio.DadoInvalidoException;
import br.com.fatec.ecommerce.estoque.aplicacao.ConsultarSaldo;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Composite: consumidor da visao uniforme do catalogo, sem distinguir variacao, kit ou novo tipo.
 * Agrega componentes compartilhados antes de consultar saldos; nunca reserva estoque (RN-17).
 */
@Service
public class ConsultarDisponibilidadeDoCarrinho {
    public record Insuficiencia(Long variacaoId, long solicitada, int disponivel) { }

    private final ConsultarSaldo saldos;

    public ConsultarDisponibilidadeDoCarrinho(ConsultarSaldo saldos) { this.saldos = saldos; }

    public List<Insuficiencia> consultar(Carrinho carrinho, Map<String, ItemVendavelInfo> catalogo) {
        Map<Long, Long> demanda = new LinkedHashMap<>();
        try {
            for (var linha : carrinho.getItens()) {
                var item = catalogo.get(linha.getSku());
                if (item != null) {
                    for (var componente : item.componentes()) {
                        long quantidade = Math.multiplyExact((long) linha.getQuantidade(), componente.quantidade());
                        demanda.merge(componente.variacaoId(), quantidade, Math::addExact);
                    }
                }
            }
        } catch (ArithmeticException erro) {
            throw new DadoInvalidoException("A demanda de componentes excede o limite permitido.");
        }
        if (demanda.isEmpty()) { return List.of(); }
        Map<Long, Integer> disponiveis = new LinkedHashMap<>();
        saldos.porVariacoes(demanda.keySet()).forEach(s -> disponiveis.put(s.getVariacaoId(), s.getDisponivel()));
        return demanda.entrySet().stream()
                .filter(e -> e.getValue() > disponiveis.getOrDefault(e.getKey(), 0))
                .map(e -> new Insuficiencia(e.getKey(), e.getValue(), disponiveis.getOrDefault(e.getKey(), 0)))
                .toList();
    }
}
