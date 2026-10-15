package br.com.fatec.ecommerce.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fatec.ecommerce.TesteDeIntegracao;
import br.com.fatec.ecommerce.seguranca.UsuariosParaTeste;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/** A migracao V2 cria as tabelas do pedido e o banco rejeita valores fora das restricoes (ddl-auto=validate passa). */
@TesteDeIntegracao
class MigracaoV2Tests {

    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    UsuariosParaTeste helper;

    long clienteId;
    long pedidoId;

    @BeforeEach
    void preparar() {
        helper.limpar();
        jdbc.update("INSERT INTO usuario (email, senha_hash) VALUES ('v2@teste.local', 'x')");
        long usuarioId = jdbc.queryForObject("SELECT id FROM usuario WHERE email = 'v2@teste.local'", Long.class);
        jdbc.update("INSERT INTO cliente (usuario_id, nome, cpf) VALUES (?, 'Teste', '12345678901')", usuarioId);
        clienteId = jdbc.queryForObject("SELECT id FROM cliente WHERE usuario_id = ?", Long.class, usuarioId);
        pedidoId = inserirPedido("CRIADO");
    }

    private long inserirPedido(String situacao) {
        return jdbc.queryForObject("INSERT INTO pedido (cliente_id, situacao, entrega_cep, entrega_logradouro, "
                + "entrega_numero, entrega_bairro, entrega_cidade, entrega_uf, subtotal, total_descontos, frete, total, "
                + "criado_em) VALUES (?, ?, '13010000', 'Rua A', '1', 'Centro', 'Campinas', 'SP', 10, 0, 0, 10, now()) "
                + "RETURNING id", Long.class, clienteId, situacao);
    }

    @Test
    void flywayAplicaV2ECriaAsTabelasDoPedido() {
        List<String> tabelas = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'", String.class);
        assertThat(tabelas).contains("pedido", "pedido_item", "pedido_desconto", "pedido_historico");
        assertThat(jdbc.queryForObject("SELECT success FROM flyway_schema_history WHERE version = '2'",
                Boolean.class)).isTrue();
    }

    @Test
    void pedidoTemColunaDeVersaoComValorInicialZero() {
        assertThat(jdbc.queryForObject("SELECT versao FROM pedido WHERE id = ?", Long.class, pedidoId)).isZero();
    }

    @Test
    void situacaoForaDosOitoValoresERejeitada() {
        assertThatThrownBy(() -> inserirPedido("INEXISTENTE")).isInstanceOf(DataIntegrityViolationException.class);
        for (String situacao : List.of("CRIADO", "AGUARDANDO_PAGAMENTO", "PAGO", "PAGAMENTO_RECUSADO",
                "EM_PREPARACAO", "ENVIADO", "ENTREGUE", "CANCELADO")) {
            inserirPedido(situacao);
        }
    }

    @Test
    void pedidoExigeClienteExistente() {
        assertThatThrownBy(() -> jdbc.update("INSERT INTO pedido (cliente_id, situacao, entrega_cep, "
                + "entrega_logradouro, entrega_numero, entrega_bairro, entrega_cidade, entrega_uf, subtotal, "
                + "total_descontos, frete, total, criado_em) VALUES (-1, 'CRIADO', '13010000', 'Rua A', '1', 'Centro', "
                + "'Campinas', 'SP', 10, 0, 0, 10, now())")).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void itemExigeQuantidadeMaiorQueZero() {
        String insercao = "INSERT INTO pedido_item (pedido_id, sku, nome_produto, quantidade, preco_unitario, "
                + "total_item) VALUES (?, 'SKU', 'Produto', ?, 10, 10)";
        jdbc.update(insercao, pedidoId, 1);

        assertThatThrownBy(() -> jdbc.update(insercao, pedidoId, 0)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(insercao, pedidoId, -2)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void descontoRestringeOrigemEAlvo() {
        String insercao = "INSERT INTO pedido_desconto (pedido_id, descricao, origem, alvo, valor) "
                + "VALUES (?, 'Desc', ?, ?, 5)";
        jdbc.update(insercao, pedidoId, "PROMOCAO", "ITENS");
        jdbc.update(insercao, pedidoId, "CUPOM", "FRETE");

        assertThatThrownBy(() -> jdbc.update(insercao, pedidoId, "OUTRA", "ITENS"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(insercao, pedidoId, "CUPOM", "TOTAL"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void historicoAceitaOrigemNulaERestringeSituacoesETipoDeResponsavel() {
        String insercao = "INSERT INTO pedido_historico (pedido_id, de, para, ocorrido_em, responsavel_tipo) "
                + "VALUES (?, ?, ?, now(), ?)";
        jdbc.update(insercao, pedidoId, null, "CRIADO", "SISTEMA");
        jdbc.update(insercao, pedidoId, "CRIADO", "AGUARDANDO_PAGAMENTO", "CLIENTE");
        jdbc.update(insercao, pedidoId, "PAGO", "CANCELADO", "ADMINISTRATIVO");

        assertThatThrownBy(() -> jdbc.update(insercao, pedidoId, "X", "CRIADO", "SISTEMA"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(insercao, pedidoId, "CRIADO", "X", "SISTEMA"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(insercao, pedidoId, "CRIADO", "PAGO", "ROBO"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
