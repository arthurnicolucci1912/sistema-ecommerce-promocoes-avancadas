package br.com.fatec.ecommerce.promocao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import br.com.fatec.ecommerce.promocao.dominio.*;
import br.com.fatec.ecommerce.promocao.infra.ConfiguracaoPromocoes;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** Strategy de conflito escolhida por propriedade; contexto leve, sem banco: so a configuracao do motor. */
class PoliticaConflitoSelecionavelTests {
    private final ApplicationContextRunner contexto = new ApplicationContextRunner()
            .withUserConfiguration(ConfiguracaoPromocoes.class)
            .withBean(FonteDeCupons.class, () -> mock(FonteDeCupons.class));

    @Test
    void semPropriedadeVigoraAPoliticaPadrao() {
        contexto.run(c -> assertThat(c.getBean(PoliticaConflito.class)).isInstanceOf(PoliticaPadrao.class));
    }

    @Test
    void padraoExplicitoRegistraAPoliticaPadrao() {
        contexto.withPropertyValues("ecommerce.promocoes.politica-conflito=padrao")
                .run(c -> assertThat(c.getBean(PoliticaConflito.class)).isInstanceOf(PoliticaPadrao.class));
    }

    @Test
    void maiorDescontoRegistraAPoliticaAlternativaEUmaUnicaPolitica() {
        contexto.withPropertyValues("ecommerce.promocoes.politica-conflito=maior-desconto").run(c -> {
            assertThat(c.getBeansOfType(PoliticaConflito.class)).hasSize(1);
            assertThat(c.getBean(PoliticaConflito.class)).isInstanceOf(PoliticaSomenteMaiorDesconto.class);
            assertThat(c.getBean(MotorDePromocoes.class)).isNotNull();
        });
    }
}
