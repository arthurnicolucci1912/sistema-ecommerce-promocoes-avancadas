package br.com.fatec.ecommerce.pagamento.dominio;

import java.util.*;

/** Resolve provedores registrados sem conhecer APIs concretas. Nao e uma fabrica de cobrancas. */
public final class RegistroGateways {
    private final Map<String, GatewayPagamento> fornecedores = new HashMap<>();
    private final Map<FormaPagamento, GatewayPagamento> selecionados = new EnumMap<>(FormaPagamento.class);
    public RegistroGateways(List<GatewayPagamento> gateways, Map<FormaPagamento, String> selecao) {
        for (var gateway : gateways) {
            String id = gateway.fornecedor();
            if (id == null || id.isBlank() || id.length() > 80 || fornecedores.putIfAbsent(id, gateway) != null)
                throw new IllegalArgumentException("Identidade de fornecedor ausente ou duplicada.");
        }
        for (var forma : FormaPagamento.values()) {
            var candidatos = gateways.stream().filter(g -> g.suporta(forma)).toList();
            var id = selecao.get(forma);
            var escolhido = id == null ? (candidatos.size() == 1 ? candidatos.getFirst() : null) : fornecedores.get(id);
            if (escolhido == null || !escolhido.suporta(forma))
                throw new IllegalArgumentException("Seleção de fornecedor ausente, ambígua ou inválida para " + forma);
            selecionados.put(forma, escolhido);
        }
    }
    public GatewayPagamento para(FormaPagamento forma) { return selecionados.get(Objects.requireNonNull(forma)); }
    public GatewayPagamento fornecedor(String id) {
        var gateway = fornecedores.get(id);
        if (gateway == null) throw new IllegalStateException("Fornecedor da cobrança não está registrado: " + id);
        return gateway;
    }
}
