package br.com.fatec.ecommerce.promocao;

import static org.assertj.core.api.Assertions.*;
import br.com.fatec.ecommerce.TesteDeIntegracao;
import br.com.fatec.ecommerce.infra.demo.PromocoesDeDemonstracao;
import br.com.fatec.ecommerce.promocao.aplicacao.ConfirmarUsoCupom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@TesteDeIntegracao @ActiveProfiles({"dev","test"})
class PromocoesDemonstracaoTests {
    @Autowired PromocoesDeDemonstracao carga;
    @Autowired JdbcTemplate jdbc;
    @Autowired ConfirmarUsoCupom usos;
    @Test void repeticaoDaCargaNaoDuplicaNemRestauraUtilizacoes() {
        long id=jdbc.queryForObject("SELECT id FROM cupom WHERE codigo='DEMO10'",Long.class);
        long versao=jdbc.queryForObject("SELECT versao FROM cupom WHERE id=?",Long.class,id);
        long cliente=jdbc.queryForObject("SELECT id FROM cliente LIMIT 1",Long.class);
        usos.executar(id,versao,880001L,cliente);
        try {
            carga.run(null); carga.run(null);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM cupom WHERE codigo='DEMO10'",Integer.class)).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM promocao_config WHERE nome='DEMO_PRIMEIRA_COMPRA'",Integer.class)).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT ativo FROM promocao_config WHERE nome='DEMO_PRIMEIRA_COMPRA'",Boolean.class)).isFalse();
            assertThat(jdbc.queryForObject("SELECT usos_ativos FROM saldo_cupom WHERE cupom_id=?",Integer.class,id)).isEqualTo(1);
        } finally { usos.devolver(880001L); }
    }
}
