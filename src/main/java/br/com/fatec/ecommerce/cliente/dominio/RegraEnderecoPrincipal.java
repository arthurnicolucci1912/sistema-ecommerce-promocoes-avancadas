package br.com.fatec.ecommerce.cliente.dominio;

import java.util.List;
import java.util.Optional;

/**
 * Regra do endereco principal (dominio puro, sem Spring): cada cliente tem no maximo um principal ativo.
 * <ul>
 *   <li>o primeiro endereco ativo cadastrado e o principal;</li>
 *   <li>marcar outro como principal desmarca o anterior;</li>
 *   <li>removido o principal, o ativo mais antigo restante assume.</li>
 * </ul>
 * As listas recebidas contem apenas enderecos ativos, em ordem de cadastro (mais antigo primeiro).
 * A regra apenas decide; quem persiste deve gravar o rebaixamento (e dar flush) antes da promocao,
 * por causa do indice unico parcial {@code ux_endereco_principal_ativo}.
 */
public final class RegraEnderecoPrincipal {

    private RegraEnderecoPrincipal() {
    }

    /** Um novo endereco nasce principal quando nenhum dos ativos existentes o e. */
    public static boolean novoEnderecoDeveSerPrincipal(List<Endereco> ativosExistentes) {
        return ativosExistentes.stream().noneMatch(Endereco::isPrincipal);
    }

    /** O principal atual que precisa ser desmarcado para que {@code novoPrincipal} assuma, se houver. */
    public static Optional<Endereco> principalASerSubstituido(List<Endereco> ativos, Endereco novoPrincipal) {
        return ativos.stream().filter(e -> e.isPrincipal() && e != novoPrincipal).findFirst();
    }

    /**
     * Endereco que deve virar principal apos uma remocao: o mais antigo dos ativos restantes, caso nenhum
     * deles seja principal. Vazio se nao sobrou endereco ou se ja existe principal.
     */
    public static Optional<Endereco> sucessorAposRemocao(List<Endereco> ativosRestantes) {
        if (ativosRestantes.isEmpty() || ativosRestantes.stream().anyMatch(Endereco::isPrincipal)) {
            return Optional.empty();
        }
        return Optional.of(ativosRestantes.get(0));
    }
}
