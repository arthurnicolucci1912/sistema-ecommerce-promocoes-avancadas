package br.com.fatec.ecommerce.promocao.dominio;

/** Estado da avaliacao; nao e um consumo nem uma reserva de uso. */
public record SituacaoCupom(String codigo, Long cupomId, Long versao, String motivo) {
    public boolean valido() { return motivo==null; }
}
