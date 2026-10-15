package br.com.fatec.ecommerce.cliente.web;

import br.com.fatec.ecommerce.cliente.aplicacao.DadosDoCliente;

/** Dados do cliente. Nunca inclui senha nem hash. O CPF vai somente com dígitos. */
public record ClienteResponse(Long id, String nome, String email, String cpf, String telefone) {

    static ClienteResponse de(DadosDoCliente dados) {
        return new ClienteResponse(dados.id(), dados.nome(), dados.email(), dados.cpf(), dados.telefone());
    }
}
