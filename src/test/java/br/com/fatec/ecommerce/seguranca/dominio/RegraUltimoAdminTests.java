package br.com.fatec.ecommerce.seguranca.dominio;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fatec.ecommerce.compartilhado.dominio.ConflitoException;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Regra do ultimo ADMIN, sem Spring. */
class RegraUltimoAdminTests {

    private static Usuario admin() {
        return new Usuario("admin@x.com", "h", Set.of(Perfil.ADMIN));
    }

    @Test
    void retirarPerfilAdminDoUnicoAdminAtivoEhConflito() {
        assertThatThrownBy(() -> RegraUltimoAdmin.garantirAdminRemanescente(admin(), Set.of(Perfil.ESTOQUE), true, 1))
                .isInstanceOf(ConflitoException.class);
    }

    @Test
    void inativarOUnicoAdminAtivoEhConflito() {
        assertThatThrownBy(() -> RegraUltimoAdmin.garantirAdminRemanescente(admin(), Set.of(Perfil.ADMIN), false, 1))
                .isInstanceOf(ConflitoException.class);
    }

    @Test
    void comOutroAdminAtivoAOperacaoEhPermitida() {
        assertThatCode(() -> {
            RegraUltimoAdmin.garantirAdminRemanescente(admin(), Set.of(Perfil.ESTOQUE), true, 2);
            RegraUltimoAdmin.garantirAdminRemanescente(admin(), Set.of(Perfil.ADMIN), false, 2);
        }).doesNotThrowAnyException();
    }

    @Test
    void mantendoOPerfilAdminAtivoAOperacaoEhPermitida() {
        assertThatCode(() -> RegraUltimoAdmin.garantirAdminRemanescente(admin(),
                Set.of(Perfil.ADMIN, Perfil.PROMOCOES), true, 1)).doesNotThrowAnyException();
    }

    @Test
    void alvoQueNaoEAdminAtivoNaoEAfetadoPelaRegra() {
        Usuario estoque = new Usuario("e@x.com", "h", Set.of(Perfil.ESTOQUE));
        Usuario adminInativo = admin();
        adminInativo.inativar();
        assertThatCode(() -> {
            RegraUltimoAdmin.garantirAdminRemanescente(estoque, Set.of(Perfil.PROMOCOES), false, 1);
            RegraUltimoAdmin.garantirAdminRemanescente(adminInativo, Set.of(Perfil.ESTOQUE), true, 0);
        }).doesNotThrowAnyException();
    }
}
