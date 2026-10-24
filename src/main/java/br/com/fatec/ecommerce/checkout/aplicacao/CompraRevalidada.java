package br.com.fatec.ecommerce.checkout.aplicacao;

import br.com.fatec.ecommerce.carrinho.dominio.Carrinho;
import br.com.fatec.ecommerce.estoque.dominio.ComponenteAReservar;
import br.com.fatec.ecommerce.pedido.dominio.DadosNovoPedido;
import java.util.List;

/** Dados derivados de uma unica revalidacao, compartilhados pela previa e pela conclusao. */
record CompraRevalidada(Carrinho carrinho, PreviaCheckout previa, DadosNovoPedido pedido,
        List<ComponenteAReservar> componentes) {
    CompraRevalidada { componentes = List.copyOf(componentes); }
}
