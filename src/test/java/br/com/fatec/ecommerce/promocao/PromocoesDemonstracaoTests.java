package br.com.fatec.ecommerce.promocao;

import static org.assertj.core.api.Assertions.*;
import br.com.fatec.ecommerce.TesteDeIntegracao;
import br.com.fatec.ecommerce.infra.demo.PromocoesDeDemonstracao;
import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import br.com.fatec.ecommerce.promocao.aplicacao.ConfirmarUsoCupom;
import br.com.fatec.ecommerce.promocao.dominio.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@TesteDeIntegracao @ActiveProfiles({"dev","test"})
class PromocoesDemonstracaoTests {
    @Autowired PromocoesDeDemonstracao carga;
    @Autowired JdbcTemplate jdbc;
    @Autowired ConfirmarUsoCupom usos;
    @Autowired ConfiguracoesPromocionais configuracoes;
    @Autowired PromocaoFactoryRegistry fabricas;
    @Autowired MotorDePromocoes motor;
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
    @Test void campanhasDeDemonstracaoDosQuatroTiposExistemUmaVezCadaERepeticaoNaoDuplica() {
        carga.run(null);
        var nomes=List.of("DEMO_LEVE3_PAGUE2_CAMISETA","DEMO_CAMISETAS_15","DEMO_ACIMA_DE_200_MENOS_30","DEMO_FRETE_GRATIS_ACIMA_DE_200");
        for(var nome:nomes) {
            assertThat(jdbc.queryForObject("SELECT count(*) FROM promocao_config WHERE nome=? AND ativo",Integer.class,nome)).as(nome).isEqualTo(1);
        }
        // Criadas ja no estado final, uma unica gravacao: sem atualizacoes redundantes (versao otimista continua em 0).
        assertThat(jdbc.queryForList("SELECT versao FROM promocao_config WHERE nome LIKE 'DEMO!_%' ESCAPE '!'",Long.class)).containsOnly(0L);
        assertThat(jdbc.queryForList("SELECT tipo FROM promocao_config WHERE nome LIKE 'DEMO!_%' ESCAPE '!' AND ativo",String.class))
                .containsExactlyInAnyOrder("LEVE_X_PAGUE_Y","PERCENTUAL_CATEGORIA","VALOR_FIXO_ACIMA_DE","FRETE_GRATIS");
    }
    /** Carrinho de exemplo do docs/10-execucao.md: 3 camisetas M + 1 caneca (subtotal 214,60) com frete bruto de 25,00. */
    @Test void carrinhoDeExemploDisparaVariasPromocoesEResolveOConflitoPelaPoliticaPadrao() {
        long camisetas=jdbc.queryForObject("SELECT id FROM categoria WHERE nome='Camisetas'",Long.class);
        long cozinha=jdbc.queryForObject("SELECT id FROM categoria WHERE nome='Cozinha'",Long.class);
        var itens=List.of(
                new ItemPreco(1L,"CAM-BAS-M",Set.of(camisetas),3,Dinheiro.de("59.90")),
                new ItemPreco(2L,"CAN-CER-300",Set.of(cozinha),1,Dinheiro.de("34.90")));
        var contexto=new ContextoPreco(itens,1L,false,"SP",Dinheiro.de("214.60"),clockAgora(),null,Set.of("CLIENTE"),Dinheiro.de("25.00"));
        var promocoes=new java.util.ArrayList<Promocao>();
        for(var nome:jdbc.queryForList("SELECT nome FROM promocao_config WHERE nome LIKE 'DEMO!_%' ESCAPE '!' AND ativo",String.class)) {
            long id=jdbc.queryForObject("SELECT id FROM promocao_config WHERE nome=?",Long.class,nome);
            promocoes.add(fabricas.criar(configuracoes.promocao(id).orElseThrow()));
        }
        var aplicados=motor.calcular(contexto,promocoes);
        // Acumulaveis (leve 3 pague 2 = 59,90 e frete gratis = 25,00) + a melhor nao acumulavel (R$ 30 vence 15% = 26,96).
        assertThat(aplicados).extracting(DescontoAplicado::nome).containsExactlyInAnyOrder(
                "DEMO_LEVE3_PAGUE2_CAMISETA","DEMO_ACIMA_DE_200_MENOS_30","DEMO_FRETE_GRATIS_ACIMA_DE_200");
        assertThat(aplicados).extracting(DescontoAplicado::nome,d->d.valor().toString()).contains(
                org.assertj.core.groups.Tuple.tuple("DEMO_LEVE3_PAGUE2_CAMISETA","59.90"),
                org.assertj.core.groups.Tuple.tuple("DEMO_ACIMA_DE_200_MENOS_30","30.00"),
                org.assertj.core.groups.Tuple.tuple("DEMO_FRETE_GRATIS_ACIMA_DE_200","25.00"));
    }
    private java.time.Instant clockAgora() { return java.time.Instant.now(); }
}
