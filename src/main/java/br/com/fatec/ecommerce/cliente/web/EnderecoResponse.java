package br.com.fatec.ecommerce.cliente.web;

import br.com.fatec.ecommerce.cliente.dominio.Endereco;

/** Endereço de entrega do cliente; o CEP vai somente com dígitos. */
public record EnderecoResponse(Long id, String apelido, String cep, String logradouro, String numero,
        String complemento, String bairro, String cidade, String uf, boolean principal) {

    static EnderecoResponse de(Endereco e) {
        return new EnderecoResponse(e.getId(), e.getApelido(), e.getCep(), e.getLogradouro(), e.getNumero(),
                e.getComplemento(), e.getBairro(), e.getCidade(), e.getUf(), e.isPrincipal());
    }
}
