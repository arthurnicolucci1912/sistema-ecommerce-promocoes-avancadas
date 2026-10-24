package br.com.fatec.ecommerce.promocao.aplicacao;

import br.com.fatec.ecommerce.promocao.dominio.*;
import java.time.Instant;
import java.util.Map;

public record CupomInfo(Long id,Long versao,String codigo,String nome,String tipo,boolean ativo,Instant inicio,
        Instant fim,Instant criadoEm,boolean acumulavel,int limiteTotal,Integer limiteCliente,int usosAtivos,
        Map<String,Object> parametros,CondicaoConfig condicao) {
    public static CupomInfo de(Cupom c,int usos) {
        return new CupomInfo(c.id(),c.versao(),c.codigo(),c.nome(),c.tipo(),c.ativo(),c.inicio(),c.fim(),c.criadoEm(),c.acumulavel(),c.limiteTotal(),c.limiteCliente(),usos,c.parametros(),c.condicao());
    }
}
