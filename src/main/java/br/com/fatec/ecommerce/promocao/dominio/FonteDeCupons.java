package br.com.fatec.ecommerce.promocao.dominio;

import java.util.Optional;
/** Porta de leitura sem consumo; a confirmacao transacional pertence ao checkout. */
public interface FonteDeCupons {
    record Disponivel(Cupom cupom, int usosAtivos, long usosCliente) { }
    Optional<Disponivel> porCodigo(String codigo, Long clienteId);
}
