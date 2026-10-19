package br.com.fatec.ecommerce.catalogo.web;

import br.com.fatec.ecommerce.catalogo.aplicacao.ComboDetalhe;
import java.math.BigDecimal;
import java.util.List;

/** Combo na vitrine pública: componentes e quantas unidades do combo ainda podem ser montadas (disponibilidade). */
public record ComboResponse(Long id, String sku, String nome, BigDecimal preco, int disponibilidade,
        List<ComponenteResponse> componentes) {

    public record ComponenteResponse(Long itemId, String sku, String nome, int quantidade) {
    }

    static ComboResponse de(ComboDetalhe combo) {
        return new ComboResponse(combo.id(), combo.sku(), combo.nome(), combo.preco().valor(),
                combo.disponibilidade(),
                combo.componentes().stream()
                        .map(c -> new ComponenteResponse(c.itemId(), c.sku(), c.nome(), c.quantidade()))
                        .toList());
    }
}
