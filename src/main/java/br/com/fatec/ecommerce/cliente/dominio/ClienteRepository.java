package br.com.fatec.ecommerce.cliente.dominio;

import java.util.Optional;

/** Porta de persistencia de clientes (implementada em {@code infra.persistencia}). */
public interface ClienteRepository {

    Cliente salvar(Cliente cliente);

    /** O cliente e localizado sempre pelo id do usuario autenticado. */
    Optional<Cliente> buscarPorUsuarioId(Long usuarioId);

    boolean existePorCpf(Cpf cpf);

    /** Usado por outros modulos (ex.: pedido) para confirmar que o cliente existe, sem carregar seus dados. */
    boolean existePorId(Long id);
}
