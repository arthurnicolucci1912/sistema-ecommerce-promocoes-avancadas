package br.com.fatec.ecommerce.entrega.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.*;
import java.util.*;

/** Enumera provedores pelo Target comum; nao conhece APIs externas nem casos particulares de transporte. */
public final class RegistroProvedoresFrete {
    private final List<ProvedorFrete> provedores;
    public RegistroProvedoresFrete(List<ProvedorFrete> provedores) {
        this.provedores = List.copyOf(provedores);
        Set<String> codigos = new HashSet<>();
        for (var p : provedores) {
            if (p.fornecedor() == null || !p.fornecedor().matches("[a-z0-9_-]{1,80}")
                    || p.modalidade() == null || !p.modalidade().matches("[A-Z0-9_-]{1,40}")
                    || !codigos.add(p.fornecedor() + ":" + p.modalidade()))
                throw new IllegalArgumentException("Identidade de opção de entrega inválida ou duplicada.");
        }
    }
    public List<OpcaoEntrega> cotar(ContextoFrete contexto) {
        List<OpcaoEntrega> opcoes = new ArrayList<>();
        boolean falhou = false;
        for (var p : provedores) {
            try {
                if (!p.atende(contexto)) continue;
                var cotacao = p.cotar(contexto);
                if (cotacao == null || !p.modalidade().equals(cotacao.modalidade()))
                    throw new IllegalStateException("Cotação incompatível com o provedor.");
                opcoes.add(new OpcaoEntrega(p.fornecedor(), cotacao));
            } catch (RuntimeException erro) {
                falhou = true;
                System.getLogger(getClass().getName()).log(System.Logger.Level.WARNING,
                        "Falha ao cotar serviço de entrega {0}:{1}", p.fornecedor(), p.modalidade());
            }
        }
        if (opcoes.isEmpty() && falhou) throw new ServicoIndisponivelException("Não foi possível cotar a entrega. Tente novamente.");
        return opcoes.stream().sorted(Comparator.comparing(OpcaoEntrega::custoBruto)
                .thenComparingInt(OpcaoEntrega::prazoDias).thenComparing(OpcaoEntrega::codigo)).toList();
    }
    public OpcaoEntrega selecionar(ContextoFrete contexto, String codigo) {
        var opcoes = cotar(contexto);
        return opcoes.stream().filter(o -> o.codigo().equals(codigo)).findFirst().orElseThrow(() ->
                new ConflitoException("A opção de entrega não está disponível. Escolha uma opção atual e consulte nova prévia.") {
                    @Override public Map<String,Object> propriedades() { return Map.of("opcoesEntrega", opcoes); }
                });
    }
}
