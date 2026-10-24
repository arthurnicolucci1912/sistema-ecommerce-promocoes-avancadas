package br.com.fatec.ecommerce.pagamento.infra.gateway;

import java.math.BigDecimal;
import java.util.Map;

/** API PIX distinta: documento em mapa e devolucao booleana. */
public class PixFicticio {
    public Map<String,String> gerar(BigDecimal reais, String referencia) {
        return Map.of("txid", "PIX-" + referencia, "status", "ATIVA");
    }
    public boolean devolver(String txid, BigDecimal reais, boolean falhar) { return !falhar; }
}
