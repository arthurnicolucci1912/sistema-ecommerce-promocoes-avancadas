package br.com.fatec.ecommerce.promocao.dominio;

import java.util.List;
/** Strategy alternativa: substituivel sem alterar as promocoes. */
public class PoliticaSomenteMaiorDesconto implements PoliticaConflito {
    @Override public List<DescontoAplicado> resolver(List<DescontoCandidato> elegiveis) {
        return elegiveis.stream().min(PoliticaPadrao.MELHOR).map(c->List.of(PoliticaPadrao.aplicar(c))).orElse(List.of());
    }
}
