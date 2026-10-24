package br.com.fatec.ecommerce.preco.infra;

import br.com.fatec.ecommerce.preco.aplicacao.PipelinePreco;
import br.com.fatec.ecommerce.preco.dominio.EtapaSubtotal;
import br.com.fatec.ecommerce.preco.dominio.EtapaTotal;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Composicao explicita do pipeline inicial; novas etapas entram entre subtotal e total. */
@Configuration
public class ConfiguracaoPreco {
    @Bean
    PipelinePreco pipelinePreco(br.com.fatec.ecommerce.promocao.dominio.FonteDePromocoes fonte,
            br.com.fatec.ecommerce.promocao.dominio.MotorDePromocoes motor,
            br.com.fatec.ecommerce.promocao.dominio.AvaliadorCupom avaliador) {
        return new PipelinePreco(List.of(new EtapaSubtotal(),
                new br.com.fatec.ecommerce.preco.dominio.EtapaPromocoes(fonte, motor),
                new br.com.fatec.ecommerce.preco.dominio.EtapaCupom(avaliador),
                new br.com.fatec.ecommerce.preco.dominio.EtapaFreteBruto(), new EtapaTotal()));
    }
}
