package br.com.fatec.ecommerce.promocao.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.ConflitoException;
import java.util.Map;
/** Rejeicao recuperavel com motivo estavel para API e resumo. */
public class CupomRejeitadoException extends ConflitoException {
    private final String motivo;
    public CupomRejeitadoException(String motivo) { super("Cupom rejeitado: "+motivo+"."); this.motivo=motivo; }
    @Override public String tipo() { return "cupom-rejeitado"; }
    @Override public Map<String,Object> propriedades() { return Map.of("motivo",motivo); }
}
