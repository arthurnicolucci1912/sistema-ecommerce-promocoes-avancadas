package br.com.fatec.ecommerce.auditoria;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import br.com.fatec.ecommerce.TesteDeIntegracao;
import br.com.fatec.ecommerce.auditoria.dominio.RegistrosAuditoria;
import br.com.fatec.ecommerce.catalogo.aplicacao.*;
import br.com.fatec.ecommerce.compartilhado.dominio.*;
import br.com.fatec.ecommerce.estoque.aplicacao.AjustarEstoque;
import br.com.fatec.ecommerce.seguranca.UsuariosParaTeste;
import br.com.fatec.ecommerce.seguranca.aplicacao.GerenciarUsuariosAdministrativos;
import br.com.fatec.ecommerce.seguranca.dominio.*;
import br.com.fatec.ecommerce.promocao.aplicacao.*;
import br.com.fatec.ecommerce.promocao.dominio.*;
import br.com.fatec.ecommerce.cliente.ClientesParaTeste;
import br.com.fatec.ecommerce.pedido.PedidosParaTeste;
import br.com.fatec.ecommerce.pedido.aplicacao.*;
import br.com.fatec.ecommerce.pedido.dominio.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@TesteDeIntegracao
@Import(AuditoriaIntegracaoTests.Extensao.class)
class AuditoriaIntegracaoTests {
    @Autowired UsuariosParaTeste usuarios;
    @Autowired GerenciarCategorias categorias;
    @Autowired GerenciarProdutos produtos;
    @Autowired GerenciarCombos combos;
    @Autowired AjustarEstoque estoque;
    @Autowired GerenciarUsuariosAdministrativos administracao;
    @Autowired GerenciarPromocoes promocoes;
    @Autowired GerenciarCupons cupons;
    @Autowired ClientesParaTeste clientes;
    @Autowired CriarPedido criarPedido;
    @Autowired ConfirmarReservaDoPedido confirmarReserva;
    @Autowired TransicionarPedido transicionar;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transacoes;
    @Autowired PublicadorEventos eventos;
    @Autowired br.com.fatec.ecommerce.compartilhado.aplicacao.PublicarOperacaoAdministrativa publicacao;
    @Autowired NovoInteressado interessado;
    @Autowired MockMvc mvc;
    @MockitoSpyBean RegistrosAuditoria registros;
    Usuario ator;
    @TestConfiguration static class Extensao {
        @Bean NovoInteressado novoInteressado() { return new NovoInteressado(); }
    }
    static class NovoInteressado {
        final List<OperacaoAdministrativa> recebidos = new ArrayList<>();
        @EventListener public void ao(OperacaoAdministrativa e) { recebidos.add(e); }
    }
    @BeforeEach void preparar() {
        SecurityContextHolder.clearContext(); reset(registros); usuarios.limpar(); interessado.recebidos.clear();
        ator = usuarios.criar("auditor@teste.local", Perfil.ADMIN);
        autenticar(ator, "ADMIN");
    }
    @AfterEach void limparContexto() { SecurityContextHolder.clearContext(); }
    void autenticar(Usuario u, String perfil) {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
                Jwt.withTokenValue("teste").header("alg","none").subject(u.getId().toString()).build(),
                List.of(new SimpleGrantedAuthority("ROLE_"+perfil))));
    }
    int quantidade() { return jdbc.queryForObject("select count(*) from auditoria_operacao", Integer.class); }
    String detalhes() { return jdbc.queryForObject("select detalhes::text from auditoria_operacao order by id desc limit 1", String.class); }
    ProdutoAdministrativo produto() {
        var c=categorias.criar("Roupas", null);
        return produtos.cadastrar("Camiseta", "Algodão", "Marca", c.getId(), List.of(new NovaVariacao("CAM", Dinheiro.de("100"), Map.of("cor", "azul"))));
    }
    @Test void categoriaViaJwtResolveAtorRealESnapshotComUmaTrilhaPorChamada() throws Exception {
        SecurityContextHolder.clearContext();
        mvc.perform(post("/api/admin/categorias").header("Authorization", usuarios.bearer(ator))
                .contentType("application/json").content("{\"nome\":\"Roupas\"}")).andExpect(status().isCreated());
        assertThat(quantidade()).isEqualTo(1);
        assertThat(jdbc.queryForObject("select usuario_id from auditoria_operacao", Long.class)).isEqualTo(ator.getId());
        assertThat(detalhes()).contains("Roupas");
        assertThat(interessado.recebidos).hasSize(1);
    }
    @Test void categoriasCriarRenomearMoverAlterarERemoverRejeicaoNaoRegistra() {
        var pai=categorias.criar("Pai",null); var filha=categorias.criar("Filha",null);
        categorias.renomear(filha.getId(),"Nova"); categorias.mover(filha.getId(),pai.getId());
        categorias.alterar(filha.getId(),"Final",null);
        assertThat(quantidade()).isEqualTo(5);
        assertThat(detalhes()).contains("Nova", "Final", pai.getId().toString());
        assertThatThrownBy(() -> categorias.criar("Pai",null)).isInstanceOf(ConflitoException.class);
        assertThat(quantidade()).isEqualTo(5);
        categorias.remover(filha.getId()); assertThat(quantidade()).isEqualTo(6);
    }
    @Test void produtosVariacoesPrecosEInativacoesSemDuplicarCadastroComposto() {
        var p=produto(); assertThat(quantidade()).isEqualTo(2); // categoria + produto com variação
        produtos.alterarDados(p.id(),"Nova camiseta","Descrição","Outra",p.categoriaId());
        produtos.adicionarVariacao(p.id(),new NovaVariacao("CAM2", Dinheiro.de("80"),Map.of()));
        produtos.alterarPrecoDaVariacao(p.variacoes().getFirst().id(), Dinheiro.de("120"));
        assertThat(detalhes()).contains("100", "120");
        produtos.inativarVariacao(p.variacoes().getFirst().id()); produtos.inativarProduto(p.id());
        assertThat(quantidade()).isEqualTo(7);
        assertThatThrownBy(() -> produtos.adicionarVariacao(p.id(),new NovaVariacao("CAM",Dinheiro.de("1"),Map.of())))
                .isInstanceOf(ConflitoException.class);
        assertThat(quantidade()).isEqualTo(7);
    }
    @Test void comboCompostoRegistraPrecoEComponentesUmaUnicaVez() {
        produto();
        var c=combos.cadastrar("KIT","Kit",Dinheiro.de("90"),List.of(new ComponenteDeCombo("CAM",1)));
        int antes=quantidade();
        combos.alterar(c.getId(),Dinheiro.de("170"),List.of(new ComponenteDeCombo("CAM",2)));
        assertThat(quantidade()).isEqualTo(antes+1); assertThat(detalhes()).contains("90","170","quantidade", "CAM");
        combos.alterarPreco(c.getId(),Dinheiro.de("160"));
        combos.substituirComponentes(c.getId(),List.of(new ComponenteDeCombo("CAM",3)));
        combos.inativar(c.getId()); assertThat(quantidade()).isEqualTo(antes+4);
        assertThatThrownBy(() -> combos.alterar(c.getId(),Dinheiro.de("1"),List.of(new ComponenteDeCombo("AUSENTE",1))))
                .isInstanceOf(DadoInvalidoException.class);
        assertThat(quantidade()).isEqualTo(antes+4);
    }
    @Test void ajusteEstoqueComPerfilEstoqueIncluiDeltaEMotivoENegativoNaoRegistra() {
        var p=produto(); var operador=usuarios.criar("estoque@teste.local",Perfil.ESTOQUE); autenticar(operador,"ESTOQUE");
        Long v=p.variacoes().getFirst().id(); estoque.ajustar(v,5,"Inventário");
        assertThat(detalhes()).contains("quantidade", "5", "Inventário");
        assertThat(jdbc.queryForObject("select usuario_id from auditoria_operacao order by id desc limit 1",Long.class)).isEqualTo(operador.getId());
        assertThatThrownBy(() -> estoque.ajustar(v,-6,"Perda")).isInstanceOf(ConflitoException.class);
        assertThat(quantidade()).isEqualTo(3);
    }
    @Test void usuariosNaoIncluemCredenciaisERejeicaoNaoRegistra() {
        var u=administracao.criar("operador@teste.local","SenhaSentinela123",List.of(Perfil.ESTOQUE));
        administracao.alterarPerfis(u.getId(),List.of(Perfil.PROMOCOES)); administracao.inativar(u.getId());
        assertThat(quantidade()).isEqualTo(3);
        assertThat(jdbc.queryForList("select detalhes::text from auditoria_operacao",String.class))
                .allSatisfy(d -> assertThat(d).doesNotContain("SenhaSentinela", "senha", "hash", "token", "Bearer", "$2"));
        assertThatThrownBy(() -> administracao.inativar(ator.getId())).isInstanceOf(ConflitoException.class);
        assertThat(quantidade()).isEqualTo(3);
    }
    DadosCampanha campanha(Long versao, String nome) {
        return new DadosCampanha(versao,nome,"VALOR_FIXO_ACIMA_DE",Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2027-01-01T00:00:00Z"),1,ModoAcumulacao.ACUMULAVEL,Map.of("minimo",100,"valor",10),null);
    }
    DadosCupom cupom(Long versao, String nome) {
        return new DadosCupom(versao,"TESTE",nome,"VALOR_FIXO",Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2027-01-01T00:00:00Z"),true,10,1,Map.of("valor",5),null);
    }
    @Test void campanhasECuponsGuardamVersaoConfiguracaoEUsuarioDePromocoes() {
        var operador=usuarios.criar("promo@teste.local",Perfil.PROMOCOES); autenticar(operador,"PROMOCOES");
        var c=promocoes.criar(campanha(null,"Campanha"));
        c=promocoes.alterar(c.id(),campanha(c.versao(),"Nova campanha")); promocoes.ativo(c.id(),c.versao(),false);
        var u=cupons.criar(cupom(null,"Cupom")); u=cupons.alterar(u.id(),cupom(u.versao(),"Novo cupom"));
        cupons.ativo(u.id(),u.versao(),false);
        assertThat(quantidade()).isEqualTo(6); assertThat(detalhes()).contains("versao","parametros","Novo cupom");
        assertThat(jdbc.queryForList("select distinct usuario_id from auditoria_operacao",Long.class)).containsExactly(operador.getId());
        Long id=c.id(); assertThatThrownBy(() -> promocoes.alterar(id,campanha(-1L,"Conflito"))).isInstanceOf(ConflitoException.class);
        assertThat(quantidade()).isEqualTo(6);
    }
    @Test void rollbackPosteriorReverteOperacaoEAuditoria() {
        new TransactionTemplate(transacoes).executeWithoutResult(tx -> {
            categorias.criar("Revertida",null); assertThat(quantidade()).isEqualTo(1); tx.setRollbackOnly();
        });
        assertThat(quantidade()).isZero(); assertThat(jdbc.queryForObject("select count(*) from categoria",Integer.class)).isZero();
    }
    @Test void falhaNaPersistenciaDeAuditoriaReverteMutacaoOriginal() {
        doThrow(new IllegalStateException("Banco indisponível")).when(registros).registrar(any());
        assertThatThrownBy(() -> categorias.criar("Revertida",null)).hasRootCauseInstanceOf(IllegalStateException.class);
        assertThat(quantidade()).isZero(); assertThat(jdbc.queryForObject("select count(*) from categoria",Integer.class)).isZero();
    }
    @Test void sistemaSemAtorEClienteNaoRecebemIdentidadeFicticia() {
        SecurityContextHolder.clearContext();
        estoqueSemAtor(); assertThat(quantidade()).isZero();
        SecurityContextHolder.getContext().setAuthentication(
                org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated(
                    "carga-de-demonstracao", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        estoqueSemAtor(); assertThat(quantidade()).isZero();
        SecurityContextHolder.clearContext();
        var cliente=clientes.criar("cliente@teste.local",1901);
        var p=criarPedido.executar(PedidosParaTeste.validos(cliente.cliente().getId()));
        autenticar(cliente.usuario(),"CLIENTE");
        transicionar.cancelar(p.getId(),Responsavel.cliente(cliente.usuario().getId()),"Desisti");
        assertThat(quantidade()).isZero();
    }
    void estoqueSemAtor() {
        new TransactionTemplate(transacoes).executeWithoutResult(tx ->
            publicacao.publicar("ESTOQUE_AJUSTADO", "VARIACAO", 999L, null, Map.of("quantidade", 5, "motivo", "Carga dev")));
    }
    @Test void transicoesAdministrativasContemEstadoAnteriorEMotivoSemAuditarPagamento() {
        var cliente=clientes.criar("comprador@teste.local",1902);
        var p=criarPedido.executar(PedidosParaTeste.validos(cliente.cliente().getId()));
        confirmarReserva.executar(p.getId()); transicionar.aprovarPagamento(p.getId(),Responsavel.sistema(),null);
        assertThat(quantidade()).isZero();
        var resp=Responsavel.administrativo(ator.getId());
        transicionar.iniciarPreparacao(p.getId(),resp); transicionar.enviar(p.getId(),resp); transicionar.entregar(p.getId(),resp);
        assertThat(quantidade()).isEqualTo(3); assertThat(detalhes()).contains("ENVIADO","ENTREGUE");
        var outro=criarPedido.executar(PedidosParaTeste.validos(cliente.cliente().getId()));
        confirmarReserva.executar(outro.getId()); transicionar.aprovarPagamento(outro.getId(),Responsavel.sistema(),null);
        transicionar.cancelar(outro.getId(),resp,"Pedido duplicado");
        assertThat(quantidade()).isEqualTo(4); assertThat(detalhes()).contains("PAGO","CANCELADO","Pedido duplicado");
    }
}
