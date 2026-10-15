package br.com.fatec.ecommerce.cliente.aplicacao;

/** Visao do cliente com o e-mail da conta; o CPF vai sem mascara. Nunca carrega senha. */
public record DadosDoCliente(Long id, String nome, String email, String cpf, String telefone) {
}
