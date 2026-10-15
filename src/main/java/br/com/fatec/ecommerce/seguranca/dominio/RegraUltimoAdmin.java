package br.com.fatec.ecommerce.seguranca.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.ConflitoException;
import java.util.Collection;

/** Regra: o sistema nunca pode ficar sem nenhum usuario {@link Perfil#ADMIN} ativo. */
public final class RegraUltimoAdmin {

    private RegraUltimoAdmin() {
    }

    /**
     * Verifica se a mudanca no {@code alvo} deixaria o sistema sem ADMIN ativo.
     *
     * @param alvo        usuario ainda no estado anterior a mudanca
     * @param perfisFinais perfis que o usuario terminara tendo
     * @param ativoFinal  se o usuario terminara ativo
     * @param adminsAtivos quantidade de ADMIN ativos antes da mudanca (incluindo o alvo, se for o caso)
     * @throws ConflitoException se a mudanca removeria o ultimo ADMIN ativo
     */
    public static void garantirAdminRemanescente(Usuario alvo, Collection<Perfil> perfisFinais, boolean ativoFinal,
            long adminsAtivos) {
        boolean seraAdminAtivo = ativoFinal && perfisFinais.contains(Perfil.ADMIN);
        if (alvo.ehAdminAtivo() && !seraAdminAtivo && adminsAtivos <= 1) {
            throw new ConflitoException(
                    "Operação não permitida: o sistema precisa ter ao menos um ADMIN ativo.");
        }
    }
}
