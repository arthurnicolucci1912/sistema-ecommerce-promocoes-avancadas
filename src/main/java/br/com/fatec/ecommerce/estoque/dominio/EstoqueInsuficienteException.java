package br.com.fatec.ecommerce.estoque.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.ConflitoException;
import java.util.Map;

/** Nao ha disponivel suficiente para a variacao pedida. Resulta em HTTP 409 com a variacao e o disponivel atual. */
public class EstoqueInsuficienteException extends ConflitoException {

    private final Long variacaoId;
    private final int disponivel;

    public EstoqueInsuficienteException(Long variacaoId, int disponivel) {
        super("Estoque insuficiente para a variação " + variacaoId + ": disponível " + disponivel + ".");
        this.variacaoId = variacaoId;
        this.disponivel = disponivel;
    }

    public Long getVariacaoId() {
        return variacaoId;
    }

    public int getDisponivel() {
        return disponivel;
    }

    @Override
    public String tipo() {
        return "estoque-insuficiente";
    }

    @Override
    public String titulo() {
        return "Estoque insuficiente";
    }

    @Override
    public Map<String, Object> propriedades() {
        return Map.of("variacaoId", variacaoId, "disponivel", disponivel);
    }
}
