package br.com.fatec.ecommerce.infra.demo;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.fatec.ecommerce.TesteDeIntegracao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

/** Somente com o perfil "test": o runner de demonstracao nao existe e nenhum usuario de demo foi criado na subida. */
@TesteDeIntegracao
class UsuariosDeDemonstracaoForaDoDevTests {

    @Autowired
    ApplicationContext contexto;
    @Autowired
    Environment ambiente;
    @Autowired
    JdbcTemplate jdbc;

    @Test
    void execucaoForaDeDesenvolvimentoNaoRegistraORunner() {
        assertThat(ambiente.getActiveProfiles()).doesNotContain("dev");
        assertThat(contexto.getBeansOfType(UsuariosDeDemonstracao.class)).isEmpty();
    }

    @Test
    void nenhumClienteDeDemonstracaoFoiCriadoNaSubida() {
        // Outras classes de teste podem ter gravado dados; o runner, se existisse, teria criado o cliente demo
        // com este CPF ficticio e o nome abaixo.
        Long total = jdbc.queryForObject("select count(*) from cliente where nome = 'Cliente Demonstração'",
                Long.class);
        assertThat(total).isZero();
    }

    @Test
    void nenhumPedidoDeDemonstracaoFoiCriadoNaSubida() {
        Long total = jdbc.queryForObject("select count(*) from pedido p join cliente c on c.id = p.cliente_id "
                + "where c.nome = 'Cliente Demonstração'", Long.class);
        assertThat(total).isZero();
    }
}
