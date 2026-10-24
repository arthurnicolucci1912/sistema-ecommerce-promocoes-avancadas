package br.com.fatec.ecommerce.promocao.dominio;

import java.util.*;
import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
/** Servico de dominio: valida elegibilidade e calcula o beneficio sem consumir utilizacao. */
public class AvaliadorCupom {
    public record Resultado(SituacaoCupom situacao, DescontoAplicado desconto) { }
    private final FonteDeCupons fonte;
    private final CondicaoRegistry condicoes;
    private final BeneficioCupomRegistry beneficios;
    public AvaliadorCupom(FonteDeCupons fonte,CondicaoRegistry condicoes,BeneficioCupomRegistry beneficios) {
        this.fonte=fonte; this.condicoes=condicoes; this.beneficios=beneficios;
    }
    public Resultado avaliar(ContextoPreco contexto,List<DescontoAplicado> promocoes) {
        String codigo=contexto.cupomCodigo(); if(codigo==null) { return new Resultado(null,null); }
        var disponivel=fonte.porCodigo(codigo,contexto.clienteId());
        if(disponivel.isEmpty()) { return rejeitar(codigo,null,"INEXISTENTE"); }
        var d=disponivel.orElseThrow(); var c=d.cupom();
        if(!c.ativo()) { return rejeitar(codigo,c,"INATIVO"); }
        if(!c.vigenteEm(contexto.momento())) { return rejeitar(codigo,c,"FORA_DA_VIGENCIA"); }
        if(d.usosAtivos()>=c.limiteTotal()) { return rejeitar(codigo,c,"ESGOTADO"); }
        if(c.limiteCliente()!=null && d.usosCliente()>=c.limiteCliente()) { return rejeitar(codigo,c,"LIMITE_CLIENTE"); }
        if(!condicoes.criar(c.condicao()).avaliar(contexto) || promocoes.stream().anyMatch(p->p.modo()==ModoAcumulacao.EXCLUSIVA)
                || (!c.acumulavel() && !promocoes.isEmpty())) { return rejeitar(codigo,c,"INCOMPATIVEL"); }
        Dinheiro itens=contexto.subtotal(),frete=contexto.freteBruto();
        for(var p:promocoes) { if(p.alvo()==AlvoDesconto.ITENS) { itens=itens.subtrair(p.valor()); } else { frete=frete.subtrair(p.valor()); } }
        var beneficio=beneficios.criar(c); var valor=beneficio.calcular(itens,frete);
        var aplicado=valor.maiorQue(Dinheiro.zero()) ? new DescontoAplicado(c.id(),c.nome(),valor,beneficio.alvo(),OrigemBeneficio.CUPOM,c.codigo(),ModoAcumulacao.ACUMULAVEL) : null;
        return new Resultado(new SituacaoCupom(codigo,c.id(),c.versao(),null),aplicado);
    }
    private Resultado rejeitar(String codigo,Cupom cupom,String motivo) {
        return new Resultado(new SituacaoCupom(codigo,cupom==null?null:cupom.id(),cupom==null?null:cupom.versao(),motivo),null);
    }
}
