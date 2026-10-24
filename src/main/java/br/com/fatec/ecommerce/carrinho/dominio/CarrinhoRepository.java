package br.com.fatec.ecommerce.carrinho.dominio;

import java.util.Optional;

/** Porta de persistencia do agregado, sempre localizado pelo cliente proprietario. */
public interface CarrinhoRepository {
    Optional<Carrinho> buscarPorClienteId(Long clienteId);
    Carrinho salvar(Carrinho carrinho);
}
