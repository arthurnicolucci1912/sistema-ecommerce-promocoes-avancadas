package br.com.fatec.ecommerce.infra.demo;

import br.com.fatec.ecommerce.promocao.dominio.*;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Carga dev idempotente. Campanha automatica inicia inativa; DEMO10 e aplicado explicitamente pelo cliente. */
@Component @Profile("dev")
public class PromocoesDeDemonstracao implements ApplicationRunner {
    private final ConfiguracoesPromocionais configuracoes;
    private final JdbcTemplate jdbc;
    private final Clock clock;
    public PromocoesDeDemonstracao(ConfiguracoesPromocionais configuracoes,JdbcTemplate jdbc,Clock clock) {
        this.configuracoes=configuracoes; this.jdbc=jdbc; this.clock=clock;
    }
    @Override @Transactional
    public void run(ApplicationArguments argumentos) {
        var agora=clock.instant();
        if(jdbc.queryForObject("SELECT count(*) FROM promocao_config WHERE nome='DEMO_PRIMEIRA_COMPRA'",Integer.class)==0) {
            var campanha=new PromocaoConfig("DEMO_PRIMEIRA_COMPRA","PERCENTUAL_PRIMEIRA_COMPRA",agora.minusSeconds(1),
                    agora.plus(36500,ChronoUnit.DAYS),10,ModoAcumulacao.ACUMULAVEL,Map.of("percentual",10),null,agora);
            campanha.definirAtivo(false); configuracoes.salvar(campanha);
        }
        if(jdbc.queryForObject("SELECT count(*) FROM cupom WHERE codigo='DEMO10'",Integer.class)==0) {
            configuracoes.salvar(new Cupom("DEMO10","Desconto da demonstração","PERCENTUAL",agora.minusSeconds(1),
                    agora.plus(36500,ChronoUnit.DAYS),true,100,1,Map.of("percentual",10),null,agora));
        }
    }
}
