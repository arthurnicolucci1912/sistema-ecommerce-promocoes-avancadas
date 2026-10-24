package br.com.fatec.ecommerce.promocao.dominio;

import java.time.Instant;
import java.util.Optional;
import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;

/** Strategy: metadados/elegibilidade comuns; cada subclasse calcula somente seu beneficio. */
public abstract class PromocaoBase implements Promocao {
    protected final PromocaoConfig config;
    private final CondicaoElegibilidade condicao;
    protected PromocaoBase(PromocaoConfig config, CondicaoElegibilidade condicao) { this.config=config; this.condicao=condicao; }
    @Override public Long id() { return config.id(); }
    @Override public String nome() { return config.nome(); }
    @Override public int prioridade() { return config.prioridade(); }
    @Override public ModoAcumulacao modo() { return config.modo(); }
    @Override public boolean vigenteEm(Instant instante) { return config.vigenteEm(instante); }
    protected AlvoDesconto alvo() { return AlvoDesconto.ITENS; }
    protected abstract Dinheiro beneficio(ContextoPreco contexto);
    @Override public Optional<DescontoCandidato> avaliar(ContextoPreco contexto) {
        if(!vigenteEm(contexto.momento()) || !condicao.avaliar(contexto)) { return Optional.empty(); }
        Dinheiro valor=beneficio(contexto);
        if(!valor.maiorQue(Dinheiro.zero())) { return Optional.empty(); }
        return Optional.of(new DescontoCandidato(id(),nome(),valor,alvo(),modo(),prioridade(),config.criadoEm()));
    }
}
