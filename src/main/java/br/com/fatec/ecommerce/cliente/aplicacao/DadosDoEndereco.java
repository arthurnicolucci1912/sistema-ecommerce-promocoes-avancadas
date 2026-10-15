package br.com.fatec.ecommerce.cliente.aplicacao;

/** Dados de localizacao informados ao cadastrar ou alterar um endereco. */
public record DadosDoEndereco(String apelido, String cep, String logradouro, String numero, String complemento,
        String bairro, String cidade, String uf) {
}
