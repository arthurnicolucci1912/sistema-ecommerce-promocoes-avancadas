package br.com.fatec.ecommerce.promocao.dominio;

import java.util.*;
import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
/** Contexto da Strategy: seleciona com a politica injetada e limita valores efetivos por alvo. */
public class MotorDePromocoes {
    private final PoliticaConflito politica;
    public MotorDePromocoes(PoliticaConflito politica) { this.politica=politica; }
    public List<DescontoAplicado> calcular(ContextoPreco contexto,List<Promocao> promocoes) {
        var candidatos=promocoes.stream().flatMap(p->p.avaliar(contexto).stream()).toList();
        Map<Long,DescontoCandidato> porId=new HashMap<>(); candidatos.forEach(c->porId.put(c.promocaoId(),c));
        var escolhidos=politica.resolver(candidatos).stream().sorted((a,b)->PoliticaPadrao.PRIORIDADE.compare(porId.get(a.referenciaId()),porId.get(b.referenciaId()))).toList();
        Map<AlvoDesconto,Dinheiro> saldo=new EnumMap<>(AlvoDesconto.class);
        saldo.put(AlvoDesconto.ITENS,contexto.subtotal()); saldo.put(AlvoDesconto.FRETE,contexto.freteBruto());
        var aplicados=new ArrayList<DescontoAplicado>();
        for(var d:escolhidos) {
            var valor=d.valor().menorQue(saldo.get(d.alvo())) ? d.valor() : saldo.get(d.alvo());
            if(valor.maiorQue(Dinheiro.zero())) {
                aplicados.add(new DescontoAplicado(d.referenciaId(),d.nome(),valor,d.alvo(),d.origem(),d.codigo(),d.modo()));
                saldo.put(d.alvo(),saldo.get(d.alvo()).subtrair(valor));
            }
        }
        return List.copyOf(aplicados);
    }
}
