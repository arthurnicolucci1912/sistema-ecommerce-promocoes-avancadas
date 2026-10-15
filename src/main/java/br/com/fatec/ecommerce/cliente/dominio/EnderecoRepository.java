package br.com.fatec.ecommerce.cliente.dominio;

import java.util.List;
import java.util.Optional;

/** Porta de persistencia de enderecos (implementada em {@code infra.persistencia}). */
public interface EnderecoRepository {

    /** Grava e faz flush imediato (a ordem das escritas importa para o indice do endereco principal). */
    Endereco salvar(Endereco endereco);

    /** Enderecos ativos do cliente, do mais antigo para o mais novo. */
    List<Endereco> listarAtivos(Long clienteId);

    /** Busca sempre pelo par (id, clienteId); endereco inativo ou de outro cliente nao e encontrado. */
    Optional<Endereco> buscarAtivo(Long id, Long clienteId);
}
