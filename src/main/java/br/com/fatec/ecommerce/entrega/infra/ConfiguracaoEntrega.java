package br.com.fatec.ecommerce.entrega.infra;

import br.com.fatec.ecommerce.entrega.dominio.*;
import java.util.List;
import org.springframework.context.annotation.*;

@Configuration
public class ConfiguracaoEntrega {
    @Bean RegistroProvedoresFrete registroProvedoresFrete(List<ProvedorFrete> provedores) {
        return new RegistroProvedoresFrete(provedores);
    }
}
