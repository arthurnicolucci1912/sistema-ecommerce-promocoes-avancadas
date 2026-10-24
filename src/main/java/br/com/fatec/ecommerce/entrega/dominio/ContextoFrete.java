package br.com.fatec.ecommerce.entrega.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import java.math.BigDecimal;

/**
 * Dados de que um provedor precisa para atender e cotar um frete.
 *
 * <p>Contrato provisorio (onda 0): mudancas via PR revisado pelos consumidores e registradas em docs/03-arquitetura.md.
 *
 * @param uf       unidade federativa (regiao) de destino
 * @param pesoKg   peso total conhecido, em quilogramas; zero quando nao calculado (simuladores regionais ignoram peso)
 * @param subtotal valor dos itens
 */
public record ContextoFrete(String uf, BigDecimal pesoKg, Dinheiro subtotal) {
    public ContextoFrete {
        uf = uf == null ? null : uf.trim().toUpperCase(java.util.Locale.ROOT);
        RegiaoEntrega.da(uf);
        if (pesoKg == null || pesoKg.signum() < 0 || subtotal == null || subtotal.ehNegativo())
            throw new br.com.fatec.ecommerce.compartilhado.dominio.DadoInvalidoException("Contexto de frete inválido.");
    }
}
