package br.com.fatec.ecommerce.pedido.dominio;

import java.util.Objects;

/**
 * Responsavel por uma transicao do pedido, registrado no historico. O {@code usuarioId} identifica o usuario
 * autenticado e e nulo quando o responsavel e o {@link TipoResponsavel#SISTEMA}.
 */
public record Responsavel(TipoResponsavel tipo, Long usuarioId) {

    public Responsavel {
        Objects.requireNonNull(tipo, "o tipo do responsavel nao pode ser nulo");
    }

    public static Responsavel cliente(Long usuarioId) {
        return new Responsavel(TipoResponsavel.CLIENTE, usuarioId);
    }

    public static Responsavel administrativo(Long usuarioId) {
        return new Responsavel(TipoResponsavel.ADMINISTRATIVO, usuarioId);
    }

    public static Responsavel sistema() {
        return new Responsavel(TipoResponsavel.SISTEMA, null);
    }

    public boolean ehAdministrativo() {
        return tipo == TipoResponsavel.ADMINISTRATIVO;
    }
}
