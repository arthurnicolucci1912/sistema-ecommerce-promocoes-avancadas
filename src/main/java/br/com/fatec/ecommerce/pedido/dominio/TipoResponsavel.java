package br.com.fatec.ecommerce.pedido.dominio;

/** Quem aciona uma transicao do pedido: o proprio cliente, um usuario administrativo ou o sistema. */
public enum TipoResponsavel {
    CLIENTE,
    ADMINISTRATIVO,
    SISTEMA
}
