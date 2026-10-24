package br.com.fatec.ecommerce.notificacao.dominio;

import java.util.Optional;

/** Porta de leitura do endereco de comunicacao do cliente, sem expor entidades cadastrais. */
public interface DestinatariosNotificacao {
    Optional<String> emailDoCliente(Long clienteId);
}
