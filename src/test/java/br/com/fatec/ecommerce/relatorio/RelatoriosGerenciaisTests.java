package br.com.fatec.ecommerce.relatorio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.fatec.ecommerce.TesteDeIntegracao;
import br.com.fatec.ecommerce.cliente.ClientesParaTeste;
import br.com.fatec.ecommerce.pedido.PedidosPersistidosParaTeste;
import br.com.fatec.ecommerce.pedido.dominio.SituacaoPedido;
import br.com.fatec.ecommerce.relatorio.aplicacao.ConsultarRelatoriosGerenciais;
import br.com.fatec.ecommerce.relatorio.dominio.ConsultarRelatorios;
import br.com.fatec.ecommerce.relatorio.dominio.FiltroProdutosMaisVendidos;
import br.com.fatec.ecommerce.relatorio.dominio.PeriodoRelatorio;
import br.com.fatec.ecommerce.seguranca.UsuariosParaTeste;
import br.com.fatec.ecommerce.seguranca.dominio.Perfil;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * T-RL-01: relatórios (vendas, produtos, campanhas) sobre pedidos conhecidos. Os pedidos são semeados por SQL com
 * datas e referências de campanha controladas; um teste usa pedidos reais criados pelos serviços do pedido.
 *
 * <p>Período P = 2026-03-01T00:00:00Z a 2026-03-31T23:59:59Z. Vendas: A (415,00, pago em 10/03), B (200,00, pago
 * exatamente no início de P), C (100,00, pago exatamente no fim de P) e D (84,99, pago em 20/03). Não são vendas:
 * E (pago e depois cancelado), F (pago em fevereiro e cancelado em março), G (aguardando pagamento), H (pago em abril),
 * I (cancelado sem ser pago) e J (cancelado em abril).
 */
@TesteDeIntegracao
@Import(PedidosPersistidosParaTeste.class)
class RelatoriosGerenciaisTests {
    static final String BASE = "/api/admin/relatorios";
    static final Instant INICIO = Instant.parse("2026-03-01T00:00:00Z");
    static final Instant FIM = Instant.parse("2026-03-31T23:59:59Z");
    static final Instant PAGO_A = Instant.parse("2026-03-10T12:00:00Z");

    @Autowired UsuariosParaTeste usuarios;
    @Autowired ClientesParaTeste clientes;
    @Autowired PedidosPersistidosParaTeste pedidosReais;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired ConsultarRelatoriosGerenciais servico;
    @MockitoSpyBean ConsultarRelatorios leitura;
    Long clienteId;
    String admin;
    String operador;

    @BeforeEach void preparar() {
        SecurityContextHolder.clearContext();
        usuarios.limpar();
        clienteId = clientes.criar("cliente-relatorio@teste.local", 41).cliente().getId();
        admin = usuarios.bearer(usuarios.criar("admin-relatorio@teste.local", Perfil.ADMIN));
        operador = usuarios.bearer(usuarios.criar("operador-relatorio@teste.local", Perfil.OPERADOR_COMERCIAL));
        semear();
        clearInvocations(leitura);
    }

    @AfterEach void limpar() { SecurityContextHolder.clearContext(); }

    // ---- dados -------------------------------------------------------------------------------------------------

    void semear() {
        long a = pedido("ENTREGUE", "415.00", "CRIADO@2026-03-10T11:00:00Z", "PAGO@" + PAGO_A, "ENTREGUE@2026-03-12T10:00:00Z");
        item(a, "CAM-001-M", "Camiseta", "Azul, M", 2, "200.00");
        item(a, "TEN-042", "Tênis", "Preto, 42", 1, "220.00");
        desconto(a, "Promo de inverno", "PROMOCAO", "ITENS", "20.00", 11L, null);
        desconto(a, "Frete grátis", "PROMOCAO", "FRETE", "15.00", 12L, null);

        long b = pedido("EM_PREPARACAO", "200.00", "PAGO@" + INICIO, "EM_PREPARACAO@2026-03-02T10:00:00Z");
        item(b, "CAM-001-M", "Camiseta", "Azul, M", 1, "100.00");
        item(b, "MOU-001", "Mouse", null, 2, "60.00");
        desconto(b, "Promo de inverno", "PROMOCAO", "ITENS", "15.00", 11L, null);
        desconto(b, "Cupom de boas-vindas", "CUPOM", "ITENS", "10.00", 21L, "BEMVINDO");
        desconto(b, "Cupom amigo", "CUPOM", "ITENS", "10.00", 22L, "AMIGO");

        long c = pedido("ENVIADO", "100.00", "PAGO@" + FIM, "ENVIADO@2026-04-01T10:00:00Z");
        item(c, "MOU-001", "Mouse", null, 3, "90.00");
        desconto(c, "Desconto legado", "PROMOCAO", "ITENS", "5.00", null, null);

        long d = pedido("ENTREGUE", "84.99", "PAGO@2026-03-20T09:00:00Z", "ENTREGUE@2026-03-25T09:00:00Z");
        item(d, "CAB-010", "Cabo", "1m", 1, "84.99");
        desconto(d, "Desconto legado", "PROMOCAO", "ITENS", "5.00", null, null);

        // Fora do critério de venda (itens e descontos grandes para denunciar qualquer inclusão indevida).
        naoVenda(pedido("CANCELADO", "300.00", "PAGO@2026-03-12T10:00:00Z", "CANCELADO@2026-03-15T10:00:00Z"), 11L, "50.00");
        naoVenda(pedido("CANCELADO", "150.00", "PAGO@2026-02-20T10:00:00Z", "CANCELADO@2026-03-05T10:00:00Z"), 11L, "40.00");
        naoVenda(pedido("AGUARDANDO_PAGAMENTO", "70.00", "AGUARDANDO_PAGAMENTO@2026-03-10T10:00:00Z"), 11L, "30.00");
        naoVenda(pedido("ENTREGUE", "500.00", "PAGO@2026-04-02T10:00:00Z", "ENTREGUE@2026-04-03T10:00:00Z"), 11L, "100.00");
        naoVenda(pedido("CANCELADO", "60.00", "CRIADO@2026-03-18T09:00:00Z", "CANCELADO@2026-03-18T10:00:00Z"), 99L, "9.00");
        naoVenda(pedido("CANCELADO", "80.00", "CRIADO@2026-04-05T09:00:00Z", "CANCELADO@2026-04-05T10:00:00Z"), 99L, "8.00");
    }

    void naoVenda(long pedidoId, Long referencia, String valor) {
        item(pedidoId, "EXC-999", "Excluído", null, 100, "999.00");
        desconto(pedidoId, referencia == 99L ? "Campanha só de excluídos" : "Promo de inverno", "PROMOCAO", "ITENS", valor, referencia, null);
    }

    /** Cada transição no formato {@code PARA@instante}; a situação informada é a atual do pedido. */
    long pedido(String situacao, String total, String... transicoes) {
        Long id = jdbc.queryForObject("""
                INSERT INTO pedido (cliente_id, situacao, entrega_cep, entrega_logradouro, entrega_numero, entrega_bairro,
                    entrega_cidade, entrega_uf, subtotal, total_descontos, frete, total, criado_em)
                VALUES (?, ?, '13010000', 'Rua', '1', 'Centro', 'Campinas', 'SP', ?, 0, 0, ?, now()) RETURNING id
                """, Long.class, clienteId, situacao, new BigDecimal(total), new BigDecimal(total));
        for (String transicao : transicoes) {
            String[] partes = transicao.split("@");
            jdbc.update("INSERT INTO pedido_historico (pedido_id, para, ocorrido_em, responsavel_tipo) VALUES (?, ?, ?, 'SISTEMA')",
                    id, partes[0], Timestamp.from(Instant.parse(partes[1])));
        }
        return id;
    }

    void item(long pedidoId, String sku, String nome, String variacao, int quantidade, String totalItem) {
        jdbc.update("INSERT INTO pedido_item (pedido_id, sku, nome_produto, descricao_variacao, quantidade, preco_unitario, total_item)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?)", pedidoId, sku, nome, variacao, quantidade,
                new BigDecimal(totalItem).divide(BigDecimal.valueOf(quantidade), 2, java.math.RoundingMode.HALF_EVEN), new BigDecimal(totalItem));
    }

    void desconto(long pedidoId, String descricao, String origem, String alvo, String valor, Long referencia, String codigo) {
        jdbc.update("INSERT INTO pedido_desconto (pedido_id, descricao, origem, alvo, valor, referencia_id, codigo) VALUES (?, ?, ?, ?, ?, ?, ?)",
                pedidoId, descricao, origem, alvo, new BigDecimal(valor), referencia, codigo);
    }

    JsonNode obter(String rota, String bearer, String... parametros) throws Exception {
        return mapper.readTree(mvc.perform(requisicao(rota, bearer, parametros)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    MockHttpServletRequestBuilder requisicao(String rota, String bearer, String... parametros) {
        var request = get(BASE + rota).header("Authorization", bearer);
        for (int i = 0; i < parametros.length; i += 2) request.param(parametros[i], parametros[i + 1]);
        return request;
    }

    JsonNode obterPeriodo(String rota, String... extras) throws Exception {
        String[] todos = new String[extras.length + 4];
        todos[0] = "inicio"; todos[1] = INICIO.toString(); todos[2] = "fim"; todos[3] = FIM.toString();
        System.arraycopy(extras, 0, todos, 4, extras.length);
        return obter(rota, admin, todos);
    }

    static void valor(JsonNode no, String campo, String esperado) {
        assertThat(no.get(campo).decimalValue()).as(campo).isEqualByComparingTo(esperado);
    }

    // ---- vendas ------------------------------------------------------------------------------------------------

    @Test void resumoDeVendasBateComOsPedidosSemeados() throws Exception {
        var r = obterPeriodo("/vendas");
        valor(r, "valorTotalVendas", "799.99");
        assertThat(r.get("quantidadePedidos").asLong()).isEqualTo(4);
        valor(r, "ticketMedio", "200.00"); // 799,99 / 4 = 199,9975
        valor(r, "totalDescontos", "80.00");
        assertThat(r.get("quantidadeCancelamentos").asLong()).isEqualTo(3);
    }

    @Test void pedidoPagoEDepoisCanceladoPedidoNaoPagoEPagoForaDoPeriodoNaoSaoVendas() throws Exception {
        // Se E (300), F (150), G (70) ou H (500) entrassem, o total passaria de 799,99.
        valor(obterPeriodo("/vendas"), "valorTotalVendas", "799.99");
        var dia10 = obter("/vendas", admin, "inicio", "2026-03-10T00:00:00Z", "fim", "2026-03-10T23:59:59Z");
        assertThat(dia10.get("quantidadePedidos").asLong()).isEqualTo(1); // G, aguardando, não conta
        valor(dia10, "valorTotalVendas", "415.00");
    }

    @Test void pontasDoPeriodoSaoInclusivasEInicioIgualAoFimEAceito() throws Exception {
        var soInicio = obter("/vendas", admin, "inicio", INICIO.toString(), "fim", INICIO.toString());
        assertThat(soInicio.get("quantidadePedidos").asLong()).isEqualTo(1);
        valor(soInicio, "valorTotalVendas", "200.00");
        var soFim = obter("/vendas", admin, "inicio", FIM.toString(), "fim", FIM.toString());
        assertThat(soFim.get("quantidadePedidos").asLong()).isEqualTo(1);
        valor(soFim, "valorTotalVendas", "100.00");
        var antes = obter("/vendas", admin, "inicio", "2026-02-01T00:00:00Z", "fim", INICIO.minusMillis(1).toString());
        assertThat(antes.get("quantidadePedidos").asLong()).isZero(); // F pago em fevereiro, hoje cancelado
        var comFuso = obter("/vendas", admin, "inicio", "2026-02-28T21:00:00-03:00", "fim", "2026-03-31T20:59:59-03:00");
        assertThat(comFuso.get("quantidadePedidos").asLong()).isEqualTo(4);
    }

    @Test void cancelamentosContamTransicoesNoPeriodoPagasOuNao() throws Exception {
        var abril = obter("/vendas", admin, "inicio", "2026-04-01T00:00:00Z", "fim", "2026-04-30T23:59:59Z");
        assertThat(abril.get("quantidadeCancelamentos").asLong()).isEqualTo(1); // J
        assertThat(abril.get("quantidadePedidos").asLong()).isEqualTo(1); // H pago em abril
        valor(abril, "valorTotalVendas", "500.00");
        var fevereiro = obter("/vendas", admin, "inicio", "2026-02-01T00:00:00Z", "fim", "2026-02-28T23:59:59Z");
        assertThat(fevereiro.get("quantidadeCancelamentos").asLong()).isZero();
    }

    @Test void periodoSemPedidosDevolveZerosEListasVazias() throws Exception {
        String[] vazio = {"inicio", "2025-01-01T00:00:00Z", "fim", "2025-01-31T23:59:59Z"};
        var r = obter("/vendas", operador, vazio);
        valor(r, "valorTotalVendas", "0"); valor(r, "ticketMedio", "0"); valor(r, "totalDescontos", "0");
        assertThat(r.get("quantidadePedidos").asLong()).isZero();
        assertThat(r.get("quantidadeCancelamentos").asLong()).isZero();
        assertThat(obter("/produtos-mais-vendidos", operador, vazio).size()).isZero();
        assertThat(obter("/campanhas", operador, vazio).size()).isZero();
    }

    @Test void pedidosReaisCriadosPelosServicosDoPedidoEntramNosRelatorios() throws Exception {
        usuarios.limpar();
        admin = usuarios.bearer(usuarios.criar("admin-relatorio@teste.local", Perfil.ADMIN));
        clienteId = clientes.criar("cliente-relatorio@teste.local", 41).cliente().getId();
        Long pago = pedidosReais.criar(clienteId, SituacaoPedido.PAGO);
        Long cancelado = pedidosReais.criar(clienteId, SituacaoPedido.CANCELADO);
        jdbc.update("UPDATE pedido_historico SET ocorrido_em = ? WHERE pedido_id = ? AND para = 'PAGO'", Timestamp.from(PAGO_A), pago);
        jdbc.update("UPDATE pedido_historico SET ocorrido_em = ? WHERE pedido_id = ? AND para = 'CANCELADO'",
                Timestamp.from(PAGO_A.plusSeconds(60)), cancelado);
        var vendas = obterPeriodo("/vendas");
        valor(vendas, "valorTotalVendas", "415.00"); // 2 x 100 + 220 - 20 + 15 frete
        assertThat(vendas.get("quantidadePedidos").asLong()).isEqualTo(1);
        valor(vendas, "ticketMedio", "415.00");
        valor(vendas, "totalDescontos", "20.00");
        assertThat(vendas.get("quantidadeCancelamentos").asLong()).isEqualTo(1);
        var produtos = obterPeriodo("/produtos-mais-vendidos");
        assertThat(produtos.get(0).get("sku").asString()).isEqualTo("CAM-001-M");
        assertThat(produtos.get(0).get("quantidadeVendida").asLong()).isEqualTo(2);
        var campanhas = obterPeriodo("/campanhas");
        assertThat(campanhas.size()).isEqualTo(1);
        assertThat(campanhas.get(0).get("descricao").asString()).isEqualTo("Promo de inverno");
        assertThat(campanhas.get(0).get("referenciaId").isNull()).isTrue();
        valor(campanhas.get(0), "valorConcedido", "20.00");
    }

    // ---- produtos ----------------------------------------------------------------------------------------------

    @Test void produtosSaoSomadosEntrePedidosOrdenadosPorQuantidadeESku() throws Exception {
        var r = obterPeriodo("/produtos-mais-vendidos");
        assertThat(r.size()).isEqualTo(4);
        assertThat(r.valueStream().map(n -> n.get("sku").asString()).toList())
                .containsExactly("MOU-001", "CAM-001-M", "CAB-010", "TEN-042"); // 5, 3, 1 e 1 (empate: SKU asc)
        assertThat(r.get(0).get("quantidadeVendida").asLong()).isEqualTo(5);
        valor(r.get(0), "valorVendido", "150.00");
        assertThat(r.get(0).get("nomeProduto").asString()).isEqualTo("Mouse");
        assertThat(r.get(0).get("descricaoVariacao").isNull()).isTrue();
        assertThat(r.get(1).get("quantidadeVendida").asLong()).isEqualTo(3);
        valor(r.get(1), "valorVendido", "300.00");
        assertThat(r.get(1).get("descricaoVariacao").asString()).isEqualTo("Azul, M");
        valor(r.get(2), "valorVendido", "84.99");
        valor(r.get(3), "valorVendido", "220.00");
        assertThat(r.valueStream().map(n -> n.get("sku").asString())).doesNotContain("EXC-999");
    }

    @Test void limiteRestringeAListaEPadraoEDezSemDescartarOrdem() throws Exception {
        var dois = obterPeriodo("/produtos-mais-vendidos", "limite", "2");
        assertThat(dois.valueStream().map(n -> n.get("sku").asString()).toList()).containsExactly("MOU-001", "CAM-001-M");
        assertThat(obterPeriodo("/produtos-mais-vendidos", "limite", "1").size()).isEqualTo(1);
        assertThat(obterPeriodo("/produtos-mais-vendidos", "limite", "50").size()).isEqualTo(4);
        for (int i = 0; i < 12; i++) { // 12 SKUs novos em uma venda: o padrão devolve apenas 10
            long extra = pedido("ENTREGUE", "10.00", "PAGO@2026-03-21T09:00:00Z");
            item(extra, String.format("ZZZ-%02d", i), "Extra " + i, null, 1, "10.00");
        }
        assertThat(obterPeriodo("/produtos-mais-vendidos").size()).isEqualTo(10);
    }

    // ---- campanhas ---------------------------------------------------------------------------------------------

    @Test void campanhasAgrupamPorReferenciaOuDescricaoELegadoEOrdenamPorUsoValorEDescricao() throws Exception {
        var r = obterPeriodo("/campanhas");
        assertThat(r.size()).isEqualTo(5);
        // Promo de inverno (ref 11): 2 pedidos, 35,00. Legado (sem referência): 2 pedidos, 10,00.
        assertThat(r.get(0).get("descricao").asString()).isEqualTo("Promo de inverno");
        assertThat(r.get(0).get("origem").asString()).isEqualTo("PROMOCAO");
        assertThat(r.get(0).get("referenciaId").asLong()).isEqualTo(11);
        assertThat(r.get(0).get("quantidadePedidos").asLong()).isEqualTo(2);
        valor(r.get(0), "valorConcedido", "35.00");
        assertThat(r.get(1).get("descricao").asString()).isEqualTo("Desconto legado");
        assertThat(r.get(1).get("referenciaId").isNull()).isTrue();
        assertThat(r.get(1).get("quantidadePedidos").asLong()).isEqualTo(2);
        valor(r.get(1), "valorConcedido", "10.00");
        // 1 pedido cada: Frete grátis (15,00, desconto sobre frete) e depois os cupons de 10,00 por descrição.
        assertThat(r.get(2).get("descricao").asString()).isEqualTo("Frete grátis");
        valor(r.get(2), "valorConcedido", "15.00");
        assertThat(r.get(3).get("descricao").asString()).isEqualTo("Cupom amigo");
        assertThat(r.get(3).get("origem").asString()).isEqualTo("CUPOM");
        assertThat(r.get(3).get("codigo").asString()).isEqualTo("AMIGO");
        assertThat(r.get(4).get("descricao").asString()).isEqualTo("Cupom de boas-vindas");
        assertThat(r.get(4).get("codigo").asString()).isEqualTo("BEMVINDO");
        assertThat(r.valueStream().map(n -> n.get("descricao").asString())).doesNotContain("Campanha só de excluídos");
        BigDecimal soma = r.valueStream().map(n -> n.get("valorConcedido").decimalValue()).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(soma).isEqualByComparingTo(obterPeriodo("/vendas").get("totalDescontos").decimalValue()); // coerência
    }

    @Test void campanhaEmPedidoComPromocaoECupomApareceEmLinhasSeparadas() throws Exception {
        var r = obter("/campanhas", admin, "inicio", INICIO.toString(), "fim", INICIO.toString()); // só o pedido B
        assertThat(r.valueStream().map(n -> n.get("descricao").asString()).toList())
                .containsExactly("Promo de inverno", "Cupom amigo", "Cupom de boas-vindas");
    }

    // ---- validação ---------------------------------------------------------------------------------------------

    @Test void periodoEParametrosInvalidosRetornam400SemConsultar() throws Exception {
        for (String rota : List.of("/vendas", "/produtos-mais-vendidos", "/campanhas")) {
            for (String[] parametros : List.<String[]>of(
                    new String[] {}, new String[] {"inicio", INICIO.toString()}, new String[] {"fim", FIM.toString()},
                    new String[] {"inicio", "", "fim", FIM.toString()},
                    new String[] {"inicio", "ontem", "fim", FIM.toString()},
                    new String[] {"inicio", INICIO.toString(), "fim", "2026-03-31"},
                    new String[] {"inicio", INICIO.toString(), "fim", "2026-03-31T23:59:59"},
                    new String[] {"inicio", FIM.toString(), "fim", INICIO.toString()})) {
                mvc.perform(requisicao(rota, admin, parametros)).andExpect(status().isBadRequest());
            }
        }
        for (String limite : List.of("0", "-1", "51", "abc", "", "1.5", "99999999999")) {
            mvc.perform(requisicao("/produtos-mais-vendidos", admin, "inicio", INICIO.toString(), "fim", FIM.toString(), "limite", limite))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(leitura);
    }

    // ---- segurança ---------------------------------------------------------------------------------------------

    @Test void somenteAdminEOperadorComercialConsultamVendasEProdutos() throws Exception {
        String[] periodo = {"inicio", INICIO.toString(), "fim", FIM.toString()};
        mvc.perform(get(BASE + "/vendas").param("inicio", INICIO.toString()).param("fim", FIM.toString())).andExpect(status().isUnauthorized());
        mvc.perform(get(BASE + "/produtos-mais-vendidos").param("inicio", INICIO.toString()).param("fim", FIM.toString()))
                .andExpect(status().isUnauthorized());
        for (String rota : List.of("/vendas", "/produtos-mais-vendidos")) {
            mvc.perform(requisicao(rota, admin, periodo)).andExpect(status().isOk());
            mvc.perform(requisicao(rota, operador, periodo)).andExpect(status().isOk());
            for (Perfil perfil : List.of(Perfil.PROMOCOES, Perfil.ESTOQUE, Perfil.CLIENTE)) {
                String token = usuarios.bearer(usuarios.criar(perfil.name() + rota.length() + "@teste.local", perfil));
                mvc.perform(requisicao(rota, token, periodo)).andExpect(status().isForbidden());
            }
        }
    }

    @Test void campanhasTambemAceitamPromocoesMasNaoEstoqueNemCliente() throws Exception {
        String[] periodo = {"inicio", INICIO.toString(), "fim", FIM.toString()};
        mvc.perform(get(BASE + "/campanhas").param("inicio", INICIO.toString()).param("fim", FIM.toString())).andExpect(status().isUnauthorized());
        mvc.perform(requisicao("/campanhas", admin, periodo)).andExpect(status().isOk());
        mvc.perform(requisicao("/campanhas", operador, periodo)).andExpect(status().isOk());
        String promocoes = usuarios.bearer(usuarios.criar("promocoes-relatorio@teste.local", Perfil.PROMOCOES));
        assertThat(obter("/campanhas", promocoes, periodo).size()).isEqualTo(5);
        for (Perfil perfil : List.of(Perfil.ESTOQUE, Perfil.CLIENTE)) {
            String token = usuarios.bearer(usuarios.criar(perfil.name() + "-campanhas@teste.local", perfil));
            mvc.perform(requisicao("/campanhas", token, periodo)).andExpect(status().isForbidden());
        }
    }

    @Test void aAplicacaoTambemNegaAcessoForaDaApi() {
        var periodo = new PeriodoRelatorio(INICIO, FIM);
        var filtro = new FiltroProdutosMaisVendidos(periodo, 10);
        assertThatThrownBy(() -> servico.vendas(periodo)).isInstanceOf(AuthenticationCredentialsNotFoundException.class);
        autenticarComo("PROMOCOES");
        assertThat(servico.campanhas(periodo)).hasSize(5);
        assertThatThrownBy(() -> servico.vendas(periodo)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> servico.produtosMaisVendidos(filtro)).isInstanceOf(AccessDeniedException.class);
        autenticarComo("ESTOQUE");
        assertThatThrownBy(() -> servico.campanhas(periodo)).isInstanceOf(AccessDeniedException.class);
        autenticarComo("OPERADOR_COMERCIAL");
        assertThat(servico.vendas(periodo).quantidadePedidos()).isEqualTo(4);
    }

    @Test void apiNaoOfereceEscritaECadaConsultaNaoAlteraOsDados() throws Exception {
        int pedidos = jdbc.queryForObject("select count(*) from pedido", Integer.class);
        for (String rota : List.of("/vendas", "/produtos-mais-vendidos", "/campanhas")) {
            for (var request : List.of(post(BASE + rota), put(BASE + rota), patch(BASE + rota), delete(BASE + rota))) {
                mvc.perform(request.header("Authorization", admin).contentType("application/json").content("{}"))
                        .andExpect(status().isMethodNotAllowed());
            }
            obterPeriodo(rota);
        }
        assertThat(jdbc.queryForObject("select count(*) from pedido", Integer.class)).isEqualTo(pedidos);
    }

    @Test void openApiDescreveRotasParametrosERespostas() throws Exception {
        var paths = mapper.readTree(mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("paths");
        Map<String, List<String>> esperados = Map.of(
                BASE + "/vendas", List.of("inicio", "fim"),
                BASE + "/produtos-mais-vendidos", List.of("inicio", "fim", "limite"),
                BASE + "/campanhas", List.of("inicio", "fim"));
        esperados.forEach((rota, parametros) -> {
            var operacao = paths.get(rota).get("get");
            assertThat(paths.get(rota).has("post")).isFalse();
            assertThat(operacao.get("responses").propertyNames()).contains("200", "400", "401", "403");
            assertThat(operacao.get("parameters").valueStream().map(n -> n.get("name").asString()).toList())
                    .containsExactlyInAnyOrderElementsOf(parametros);
        });
    }

    void autenticarComo(String perfil) {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
                Jwt.withTokenValue("teste").header("alg", "none").subject("1").build(),
                List.of(new SimpleGrantedAuthority("ROLE_" + perfil))));
    }
}
