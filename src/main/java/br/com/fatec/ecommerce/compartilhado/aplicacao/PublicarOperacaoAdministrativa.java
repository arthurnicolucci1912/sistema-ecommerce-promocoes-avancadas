package br.com.fatec.ecommerce.compartilhado.aplicacao;

import br.com.fatec.ecommerce.compartilhado.dominio.*;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/** Resolve o ator do JWT na aplicação. Chamadores fornecem somente snapshots de campos permitidos. */
@Component
public class PublicarOperacaoAdministrativa {
    private final PublicadorEventos eventos;
    private final ObjectMapper mapper;
    public PublicarOperacaoAdministrativa(PublicadorEventos eventos, ObjectMapper mapper) {
        this.eventos = eventos; this.mapper = mapper;
    }
    public Map<String,Object> snapshot(Object dadosPermitidos) {
        return mapper.convertValue(dadosPermitidos, new TypeReference<Map<String,Object>>() {});
    }
    public void publicar(String operacao, String tipo, Long id, Map<String,Object> antes, Map<String,Object> depois) {
        var autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (!(autenticacao instanceof JwtAuthenticationToken) || !autenticacao.isAuthenticated()
                || autenticacao.getAuthorities().stream().noneMatch(a -> Set.of("ROLE_ADMIN", "ROLE_OPERADOR_COMERCIAL", "ROLE_PROMOCOES", "ROLE_ESTOQUE").contains(a.getAuthority()))) return;
        Long ator;
        try { ator = Long.valueOf(autenticacao.getName()); }
        catch (NumberFormatException ex) { throw new DadoInvalidoException("Ator administrativo inválido."); }
        Map<String,Object> detalhes = new LinkedHashMap<>();
        detalhes.put("antes", antes); detalhes.put("depois", depois);
        eventos.publicar(new OperacaoAdministrativa(ator, Instant.now(), operacao, tipo, id, detalhes));
    }
}
