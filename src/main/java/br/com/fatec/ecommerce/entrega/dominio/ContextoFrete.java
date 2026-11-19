package br.com.fatec.ecommerce.entrega.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import java.math.BigDecimal;

/**
 * Dados de que um provedor precisa para atender e cotar um frete.
 *
 * <p>Contrato provisorio (onda 0): mudancas via PR revisado pelos consumidores e registradas em docs/03-arquitetura.md.
 *
 * @param uf       unidade federativa (regiao) de destino
 * @param pesoKg   peso total conhecido, em quilogramas; zero quando nao ha peso cadastrado (o catalogo ainda nao
 *                 informa peso fisico e nenhum peso e inventado: zero fica abaixo da franquia e nao gera adicional)
 * @param subtotal valor dos itens
 * @param quantidadeItens soma das unidades dos itens da compra
 */
public record ContextoFrete(String uf, BigDecimal pesoKg, Dinheiro subtotal, int quantidadeItens) {
    public ContextoFrete {
        uf = uf == null ? null : uf.trim().toUpperCase(java.util.Locale.ROOT);
        RegiaoEntrega.da(uf);
        if (pesoKg == null || pesoKg.signum() < 0 || subtotal == null || subtotal.ehNegativo() || quantidadeItens < 0)
            throw new br.com.fatec.ecommerce.compartilhado.dominio.DadoInvalidoException("Contexto de frete inválido.");
    }
}
