package br.com.fatec.ecommerce.promocao;

import static org.assertj.core.api.Assertions.*;
import br.com.fatec.ecommerce.TesteDeIntegracao;
import java.sql.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Upgrade de uma base V4 real contendo selecao e desconto historico. */
@TesteDeIntegracao
class MigracaoPromocionalTests {
    @Autowired PostgreSQLContainer postgres;
    @Test void upgradeDaV4PreservaCarrinhoPedidoEDescontosAnteriores() throws Exception {
        String schema="upgrade_promocional";
        Flyway.configure().dataSource(postgres.getJdbcUrl(),postgres.getUsername(),postgres.getPassword())
                .schemas(schema).defaultSchema(schema).target("4").load().migrate();
        try(var conexao=DriverManager.getConnection(postgres.getJdbcUrl(),postgres.getUsername(),postgres.getPassword()); var sql=conexao.createStatement()) {
            sql.execute("SET search_path TO "+schema);
            sql.execute("INSERT INTO usuario(id,email,senha_hash) VALUES(701,'upgrade@teste.local','hash')");
            sql.execute("INSERT INTO cliente(id,usuario_id,nome,cpf) VALUES(701,701,'Cliente legado','12345678909')");
            sql.execute("INSERT INTO carrinho(id,cliente_id) VALUES(701,701)");
            sql.execute("INSERT INTO item_carrinho VALUES(701,'ANTIGO',999,'Item anterior',100,1)");
            sql.execute("INSERT INTO pedido(id,cliente_id,situacao,entrega_cep,entrega_logradouro,entrega_numero,entrega_bairro,entrega_cidade,entrega_uf,subtotal,total_descontos,frete,total,criado_em) VALUES(701,701,'AGUARDANDO_PAGAMENTO','01001000','Rua','1','Centro','SP','SP',100,10,0,90,now())");
            sql.execute("INSERT INTO pedido_desconto(pedido_id,descricao,origem,alvo,valor) VALUES(701,'Anterior','PROMOCAO','ITENS',10)");
        }
        Flyway.configure().dataSource(postgres.getJdbcUrl(),postgres.getUsername(),postgres.getPassword())
                .schemas(schema).defaultSchema(schema).load().migrate();
        try(var conexao=DriverManager.getConnection(postgres.getJdbcUrl(),postgres.getUsername(),postgres.getPassword()); var sql=conexao.createStatement()) {
            sql.execute("SET search_path TO "+schema);
            try(var rows=sql.executeQuery("SELECT cupom_codigo FROM carrinho WHERE id=701")) { assertThat(rows.next()).isTrue(); assertThat(rows.getString(1)).isNull(); }
            try(var rows=sql.executeQuery("SELECT quantidade,preco_referencia FROM item_carrinho WHERE carrinho_id=701")) { assertThat(rows.next()).isTrue(); assertThat(rows.getInt(1)).isEqualTo(1); assertThat(rows.getBigDecimal(2)).isEqualByComparingTo("100"); }
            try(var rows=sql.executeQuery("SELECT valor,referencia_id,codigo FROM pedido_desconto WHERE pedido_id=701")) { assertThat(rows.next()).isTrue(); assertThat(rows.getBigDecimal(1)).isEqualByComparingTo("10"); assertThat(rows.getObject(2)).isNull(); assertThat(rows.getString(3)).isNull(); }
            try(var rows=sql.executeQuery("SELECT total FROM pedido WHERE id=701")) { assertThat(rows.next()).isTrue(); assertThat(rows.getBigDecimal(1)).isEqualByComparingTo("90"); }
        }
    }
}
