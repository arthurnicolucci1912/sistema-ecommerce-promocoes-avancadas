package br.com.fatec.ecommerce.promocao.dominio;

import java.util.*;
/** Strategy: D4 - exclusiva ou acumulaveis e a melhor nao acumulavel, com desempate estavel. */
public class PoliticaPadrao implements PoliticaConflito {
    static final Comparator<DescontoCandidato> PRIORIDADE=Comparator.comparingInt(DescontoCandidato::prioridade).reversed()
        .thenComparing(DescontoCandidato::criadoEm).thenComparing(DescontoCandidato::promocaoId,Comparator.nullsLast(Comparator.naturalOrder()));
    static final Comparator<DescontoCandidato> MELHOR=Comparator.comparing(DescontoCandidato::valor).reversed().thenComparing(PRIORIDADE);
    @Override public List<DescontoAplicado> resolver(List<DescontoCandidato> elegiveis) {
        var exclusiva=elegiveis.stream().filter(c->c.modo()==ModoAcumulacao.EXCLUSIVA).min(MELHOR);
        if(exclusiva.isPresent()) { return List.of(aplicar(exclusiva.orElseThrow())); }
        var selecionadas=new ArrayList<>(elegiveis.stream().filter(c->c.modo()==ModoAcumulacao.ACUMULAVEL).toList());
        elegiveis.stream().filter(c->c.modo()==ModoAcumulacao.NAO_ACUMULAVEL).min(MELHOR).ifPresent(selecionadas::add);
        return selecionadas.stream().sorted(PRIORIDADE).map(PoliticaPadrao::aplicar).toList();
    }
    static DescontoAplicado aplicar(DescontoCandidato c) {
        return new DescontoAplicado(c.promocaoId(),c.nome(),c.valor(),c.alvo(),OrigemBeneficio.PROMOCAO,null,c.modo());
    }
}
