package br.com.fatec.ecommerce.seguranca.web;

import br.com.fatec.ecommerce.seguranca.dominio.Perfil;
import br.com.fatec.ecommerce.seguranca.dominio.Usuario;
import java.time.Instant;
import java.util.List;

/** Dados publicos de um usuario. Nunca inclui senha nem hash. */
public record UsuarioResponse(Long id, String email, List<Perfil> perfis, boolean ativo, Instant criadoEm) {

    static UsuarioResponse de(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getEmail(),
                usuario.getPerfis().stream().sorted().toList(), usuario.isAtivo(), usuario.getCriadoEm());
    }
}
