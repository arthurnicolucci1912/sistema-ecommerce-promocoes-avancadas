package br.com.fatec.ecommerce.catalogo.web;

import br.com.fatec.ecommerce.catalogo.dominio.Combo;
import java.math.BigDecimal;

/** Visão administrativa do combo (inclusive inativo); os componentes são consultados em {@code /api/catalogo/combos}. */
public record ComboAdministrativoResponse(Long id, String sku, String nome, BigDecimal preco, boolean ativo) {

    static ComboAdministrativoResponse de(Combo combo) {
        return new ComboAdministrativoResponse(combo.getId(), combo.getSku(), combo.getNome(),
                combo.preco().valor(), combo.isAtivo());
    }
}
