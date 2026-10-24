package br.com.fatec.ecommerce.promocao.aplicacao;

import br.com.fatec.ecommerce.promocao.dominio.*;
import java.time.Instant;
import java.util.Map;

public record CampanhaInfo(Long id,Long versao,String nome,String tipo,boolean ativo,Instant inicio,Instant fim,
        Instant criadoEm,int prioridade,ModoAcumulacao modo,Map<String,Object> parametros,CondicaoConfig condicao) {
    public static CampanhaInfo de(PromocaoConfig c) {
        return new CampanhaInfo(c.id(),c.versao(),c.nome(),c.tipo(),c.ativo(),c.inicio(),c.fim(),c.criadoEm(),c.prioridade(),c.modo(),c.parametros(),c.condicao());
    }
}
