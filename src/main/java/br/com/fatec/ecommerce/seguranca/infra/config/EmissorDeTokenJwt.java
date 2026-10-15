package br.com.fatec.ecommerce.seguranca.infra.config;

import br.com.fatec.ecommerce.seguranca.aplicacao.EmissorDeToken;
import br.com.fatec.ecommerce.seguranca.aplicacao.TokenEmitido;
import br.com.fatec.ecommerce.seguranca.dominio.Perfil;
import br.com.fatec.ecommerce.seguranca.dominio.Usuario;
import java.time.Instant;
import java.util.List;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

/** Emite JWT HS256 com os claims {@code sub} (id), {@code email}, {@code perfis}, {@code iat} e {@code exp}. */
@Component
class EmissorDeTokenJwt implements EmissorDeToken {

    private final JwtEncoder encoder;
    private final JwtProperties propriedades;

    EmissorDeTokenJwt(JwtEncoder encoder, JwtProperties propriedades) {
        this.encoder = encoder;
        this.propriedades = propriedades;
    }

    @Override
    public TokenEmitido emitir(Usuario usuario) {
        Instant agora = Instant.now();
        List<String> perfis = usuario.getPerfis().stream().map(Perfil::name).sorted().toList();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(String.valueOf(usuario.getId()))
                .claim("email", usuario.getEmail())
                .claim(ConfiguracaoSeguranca.CLAIM_PERFIS, perfis)
                .issuedAt(agora)
                .expiresAt(agora.plus(propriedades.expiracao()))
                .build();
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return new TokenEmitido(token, propriedades.expiracao().toSeconds());
    }
}
