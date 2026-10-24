package br.com.fatec.ecommerce.pagamento;

import static org.assertj.core.api.Assertions.*;
import br.com.fatec.ecommerce.TesteDeIntegracao;
import java.sql.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.postgresql.PostgreSQLContainer;

@TesteDeIntegracao
class MigracaoPagamentoTests {
    @Autowired PostgreSQLContainer postgres;
    @Test void upgradeV5PreservaPedidoPagoECupomSemInventarCobranca() throws Exception {
        String schema="upgrade_pagamento";
        Flyway.configure().dataSource(postgres.getJdbcUrl(),postgres.getUsername(),postgres.getPassword())
                .schemas(schema).defaultSchema(schema).target("5").load().migrate();
        try(var c=DriverManager.getConnection(postgres.getJdbcUrl(),postgres.getUsername(),postgres.getPassword());var s=c.createStatement()) {
            s.execute("SET search_path TO "+schema);
            s.execute("INSERT INTO usuario(id,email,senha_hash) VALUES(901,'legado@teste.local','hash')");
            s.execute("INSERT INTO cliente(id,usuario_id,nome,cpf) VALUES(901,901,'Legado','12345678909')");
            s.execute("INSERT INTO pedido(id,cliente_id,situacao,entrega_cep,entrega_logradouro,entrega_numero,entrega_bairro,entrega_cidade,entrega_uf,subtotal,total_descontos,frete,total,criado_em) VALUES(901,901,'PAGO','01001000','Rua','1','Centro','SP','SP',100,10,0,90,now())");
            s.execute("INSERT INTO pedido_desconto(pedido_id,descricao,origem,alvo,valor,referencia_id,codigo) VALUES(901,'Anterior','CUPOM','ITENS',10,88,'LEGADO')");
        }
        Flyway.configure().dataSource(postgres.getJdbcUrl(),postgres.getUsername(),postgres.getPassword())
                .schemas(schema).defaultSchema(schema).load().migrate();
        try(var c=DriverManager.getConnection(postgres.getJdbcUrl(),postgres.getUsername(),postgres.getPassword());var s=c.createStatement()) {
            s.execute("SET search_path TO "+schema);
            try(var r=s.executeQuery("SELECT total,situacao FROM pedido WHERE id=901")) {assertThat(r.next()).isTrue();assertThat(r.getBigDecimal(1)).isEqualByComparingTo("90");assertThat(r.getString(2)).isEqualTo("PAGO");}
            try(var r=s.executeQuery("SELECT codigo FROM pedido_desconto WHERE pedido_id=901")) {assertThat(r.next()).isTrue();assertThat(r.getString(1)).isEqualTo("LEGADO");}
            for(String tabela:new String[]{"cobranca","confirmacao_pagamento","estorno"})
                try(var r=s.executeQuery("SELECT count(*) FROM "+tabela)) {r.next();assertThat(r.getInt(1)).isZero();}
        }
    }
}
