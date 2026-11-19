package br.com.fatec.ecommerce.infra.demo;

import br.com.fatec.ecommerce.promocao.aplicacao.ValidarConfiguracaoComercial;
import br.com.fatec.ecommerce.promocao.dominio.*;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carga dev idempotente de campanhas e cupom de demonstracao (dados ficticios). Roda depois do runner de usuarios e
 * catalogo ({@code @Order}), cujos SKUs e categorias as campanhas referenciam.
 *
 * <p>Cada campanha e validada pelo mesmo caminho da API administrativa: o {@link PromocaoFactoryRegistry} (Factory
 * Method) materializa o tipo e {@link ValidarConfiguracaoComercial} confere SKU e categoria. Os modos e prioridades
 * variam de proposito para que um carrinho de exemplo exercite varias promocoes e a politica de conflito:
 * {@code DEMO_LEVE3_PAGUE2_CAMISETA} (acumulavel, prioridade 30), {@code DEMO_CAMISETAS_15} (nao acumulavel, 20),
 * {@code DEMO_ACIMA_DE_200_MENOS_30} (nao acumulavel, 10) e {@code DEMO_FRETE_GRATIS_ACIMA_DE_200} (acumulavel, 5).
 * {@code DEMO_PRIMEIRA_COMPRA} inicia inativa; DEMO10 e aplicado explicitamente pelo cliente.
 */
@Component @Profile("dev") @Order(2)
public class PromocoesDeDemonstracao implements ApplicationRunner {
    private static final Logger LOG = LoggerFactory.getLogger(PromocoesDeDemonstracao.class);
    private static final String CATEGORIA_CAMISETAS = "Camisetas";

    private final ConfiguracoesPromocionais configuracoes;
    private final PromocaoFactoryRegistry fabricas;
    private final ValidarConfiguracaoComercial validar;
    private final JdbcTemplate jdbc;
    private final Clock clock;
    public PromocoesDeDemonstracao(ConfiguracoesPromocionais configuracoes,PromocaoFactoryRegistry fabricas,
            ValidarConfiguracaoComercial validar,JdbcTemplate jdbc,Clock clock) {
        this.configuracoes=configuracoes; this.fabricas=fabricas; this.validar=validar; this.jdbc=jdbc; this.clock=clock;
    }
    @Override @Transactional
    public void run(ApplicationArguments argumentos) {
        var agora=clock.instant(); var fim=agora.plus(36500,ChronoUnit.DAYS);
        if(!existeCampanha("DEMO_PRIMEIRA_COMPRA")) {
            var campanha=new PromocaoConfig("DEMO_PRIMEIRA_COMPRA","PERCENTUAL_PRIMEIRA_COMPRA",agora.minusSeconds(1),
                    fim,10,ModoAcumulacao.ACUMULAVEL,Map.of("percentual",10),null,agora);
            campanha.definirAtivo(false); configuracoes.salvar(campanha);
        }
        if(jdbc.queryForObject("SELECT count(*) FROM cupom WHERE codigo='DEMO10'",Integer.class)==0) {
            configuracoes.salvar(new Cupom("DEMO10","Desconto da demonstração","PERCENTUAL",agora.minusSeconds(1),
                    fim,true,100,1,Map.of("percentual",10),null,agora));
        }
        semearCampanhasDeCatalogo(agora,fim);
    }
    /**
     * Campanhas que referenciam SKU/categoria do catalogo de demonstracao; sem o catalogo, ficam para a proxima
     * subida. O id da categoria vai como {@code int}: o JSON da configuracao devolve inteiros pequenos como
     * {@code Integer}, e um {@code Long} nos parametros faria o Hibernate ver a campanha como alterada a cada
     * flush e incrementar a versao otimista sem mudanca real.
     */
    private void semearCampanhasDeCatalogo(Instant agora,Instant fim) {
        var camisetas=jdbc.query("SELECT id FROM categoria WHERE nome=?",(rs,i)->rs.getLong(1),CATEGORIA_CAMISETAS).stream().findFirst();
        if(camisetas.isEmpty()) { LOG.warn("Catálogo de demonstração ausente: campanhas de demonstração por SKU/categoria não criadas."); return; }
        var inicio=agora.minusSeconds(1);
        campanha("DEMO_LEVE3_PAGUE2_CAMISETA","LEVE_X_PAGUE_Y",inicio,fim,30,ModoAcumulacao.ACUMULAVEL,
                Map.of("sku",CatalogoDeDemonstracao.SKU_CAMISETA_M,"leve",3,"pague",2),null,agora);
        campanha("DEMO_CAMISETAS_15","PERCENTUAL_CATEGORIA",inicio,fim,20,ModoAcumulacao.NAO_ACUMULAVEL,
                Map.of("categoriaId",Math.toIntExact(camisetas.get()),"percentual",15),null,agora);
        campanha("DEMO_ACIMA_DE_200_MENOS_30","VALOR_FIXO_ACIMA_DE",inicio,fim,10,ModoAcumulacao.NAO_ACUMULAVEL,
                Map.of("minimo",200,"valor",30),null,agora);
        campanha("DEMO_FRETE_GRATIS_ACIMA_DE_200","FRETE_GRATIS",inicio,fim,5,ModoAcumulacao.ACUMULAVEL,Map.of(),
                new CondicaoConfig("VALOR_MINIMO",Map.of("minimo",200),List.of()),agora);
    }
    private void campanha(String nome,String tipo,Instant inicio,Instant fim,int prioridade,ModoAcumulacao modo,
            Map<String,Object> parametros,CondicaoConfig condicao,Instant agora) {
        if(existeCampanha(nome)) { return; }
        var config=new PromocaoConfig(nome,tipo,inicio,fim,prioridade,modo,parametros,condicao,agora);
        fabricas.criar(config); validar.referencias(config.parametros(),config.condicao());
        configuracoes.salvar(config);
        LOG.info("Campanha de demonstração criada: {} ({})",nome,tipo);
    }
    private boolean existeCampanha(String nome) {
        return jdbc.queryForObject("SELECT count(*) FROM promocao_config WHERE nome=?",Integer.class,nome)>0;
    }
}
