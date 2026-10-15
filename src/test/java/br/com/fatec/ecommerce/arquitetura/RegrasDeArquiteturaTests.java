package br.com.fatec.ecommerce.arquitetura;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.fatec.ecommerce.compartilhado.dominio.PublicadorEventos;
import br.com.fatec.ecommerce.entrega.dominio.ProvedorFrete;
import br.com.fatec.ecommerce.notificacao.dominio.CanalNotificacao;
import br.com.fatec.ecommerce.pagamento.dominio.GatewayPagamento;
import br.com.fatec.ecommerce.preco.dominio.EtapaPreco;
import br.com.fatec.ecommerce.promocao.dominio.CondicaoElegibilidade;
import br.com.fatec.ecommerce.promocao.dominio.PoliticaConflito;
import br.com.fatec.ecommerce.promocao.dominio.Promocao;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Verificacao automatica da separacao em camadas (RNF-03) e dos contratos de design patterns da onda 0. */
@AnalyzeClasses(packages = "br.com.fatec.ecommerce", importOptions = ImportOption.DoNotIncludeTests.class)
class RegrasDeArquiteturaTests {

    private static final List<Class<?>> CONTRATOS_DA_ONDA_0 = List.of(
            Promocao.class,
            CondicaoElegibilidade.class,
            PoliticaConflito.class,
            EtapaPreco.class,
            GatewayPagamento.class,
            ProvedorFrete.class,
            CanalNotificacao.class,
            PublicadorEventos.class);

    @ArchTest
    static final ArchRule dominioNaoDependeDeCamadasExternasNemDoSpring = noClasses()
            .that().resideInAPackage("..dominio..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..web..",
                    "..infra..",
                    "..aplicacao..",
                    "org.springframework.web..",
                    "org.springframework.stereotype..",
                    "org.springframework.context..")
            .because("o dominio nao conhece web, infraestrutura, aplicacao nem o Spring (RNF-03)");

    @ArchTest
    static final ArchRule webNaoDependeDeInfra = noClasses()
            .that().resideInAPackage("..web..")
            .should().dependOnClassesThat().resideInAPackage("..infra..")
            .because("a camada web fala com a aplicacao, nunca diretamente com a infraestrutura");

    @ArchTest
    static final ArchRule compartilhadoNaoDependeDeModulosDeNegocio = noClasses()
            .that().resideInAPackage("br.com.fatec.ecommerce.compartilhado..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "br.com.fatec.ecommerce.seguranca..",
                    "br.com.fatec.ecommerce.cliente..",
                    "br.com.fatec.ecommerce.pedido..",
                    "br.com.fatec.ecommerce.promocao..",
                    "br.com.fatec.ecommerce.preco..",
                    "br.com.fatec.ecommerce.pagamento..",
                    "br.com.fatec.ecommerce.entrega..",
                    "br.com.fatec.ecommerce.notificacao..",
                    "br.com.fatec.ecommerce.infra.demo..")
            .because("o compartilhado e a base de todos os modulos: a dependencia so vai dos modulos para ele");

    @ArchTest
    static final ArchRule contratosDaOnda0SoTemImplementacaoNaInfra = noClasses()
            .that().areNotInterfaces()
            .and().resideOutsideOfPackage("..infra..")
            .should().implement(assignaveisAoContrato())
            .because("os contratos da onda 0 sao implementados por adaptadores/estrategias das etapas dos donos, "
                    + "e a fundacao nao traz implementacao concreta (so o adaptador de PublicadorEventos, em infra)");

    @Test
    void contratosDaOnda0SaoInterfaces() {
        JavaClasses classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("br.com.fatec.ecommerce");

        for (Class<?> contrato : CONTRATOS_DA_ONDA_0) {
            assertThat(classes.get(contrato).isInterface())
                    .as("%s deve ser uma interface", contrato.getSimpleName())
                    .isTrue();
        }
    }

    private static DescribedPredicate<JavaClass> assignaveisAoContrato() {
        DescribedPredicate<JavaClass> predicado = assignableTo(CONTRATOS_DA_ONDA_0.get(0));
        for (Class<?> contrato : CONTRATOS_DA_ONDA_0.subList(1, CONTRATOS_DA_ONDA_0.size())) {
            predicado = predicado.or(assignableTo(contrato));
        }
        return predicado.as("algum contrato da onda 0");
    }
}
