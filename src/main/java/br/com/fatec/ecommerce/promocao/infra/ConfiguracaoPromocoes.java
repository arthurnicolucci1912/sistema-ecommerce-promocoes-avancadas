package br.com.fatec.ecommerce.promocao.infra;

import br.com.fatec.ecommerce.promocao.dominio.*;
import java.time.Clock;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.*;

/** Composicao dos participantes Strategy/Factory/Composite; dominio permanece sem Spring. */
@Configuration
public class ConfiguracaoPromocoes {
    @Bean @ConditionalOnMissingBean(Clock.class) Clock relogioComercial() { return Clock.systemUTC(); }
    @Bean CondicaoRegistry condicoes(ObjectProvider<CriadorCondicao> extras) {
        var lista=new ArrayList<>(CriadoresDeCondicao.padrao()); extras.orderedStream().forEach(lista::add); return new CondicaoRegistry(lista);
    }
    @Bean PromocaoFactory leveXPagueYFactory() { return new LeveXPagueYFactory(); }
    @Bean PromocaoFactory percentualPorCategoriaFactory() { return new PercentualPorCategoriaFactory(); }
    @Bean PromocaoFactory valorFixoAcimaDeFactory() { return new ValorFixoAcimaDeFactory(); }
    @Bean PromocaoFactory percentualPrimeiraCompraFactory() { return new PercentualPrimeiraCompraFactory(); }
    @Bean PromocaoFactory freteGratisFactory() { return new FreteGratisFactory(); }
    @Bean PromocaoFactoryRegistry fabricas(List<PromocaoFactory> lista,CondicaoRegistry condicoes) { return new PromocaoFactoryRegistry(lista,condicoes); }
    @Bean BeneficioCupomRegistry beneficios(ObjectProvider<BeneficioCupomFactory> extras) {
        var lista=new ArrayList<>(BeneficioCupomRegistry.padrao()); extras.orderedStream().forEach(lista::add); return new BeneficioCupomRegistry(lista);
    }
    @Bean @ConditionalOnMissingBean(PoliticaConflito.class) PoliticaConflito politicaPromocional() { return new PoliticaPadrao(); }
    @Bean MotorDePromocoes motor(PoliticaConflito politica) { return new MotorDePromocoes(politica); }
    @Bean AvaliadorCupom avaliador(FonteDeCupons fonte,CondicaoRegistry condicoes,BeneficioCupomRegistry beneficios) {
        return new AvaliadorCupom(fonte,condicoes,beneficios);
    }
}
