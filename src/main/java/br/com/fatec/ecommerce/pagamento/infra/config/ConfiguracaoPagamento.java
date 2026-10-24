package br.com.fatec.ecommerce.pagamento.infra.config;

import br.com.fatec.ecommerce.pagamento.dominio.*;
import java.util.*;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;

@Configuration
public class ConfiguracaoPagamento {
    @Bean RegistroGateways registroGateways(List<GatewayPagamento> gateways, Environment ambiente) {
        Map<FormaPagamento,String> selecao = new EnumMap<>(FormaPagamento.class);
        for (var forma : FormaPagamento.values()) {
            String id = ambiente.getProperty("ecommerce.pagamento.fornecedores." + forma.name().toLowerCase(Locale.ROOT));
            if (id != null) selecao.put(forma, id);
        }
        return new RegistroGateways(gateways, selecao);
    }
}
