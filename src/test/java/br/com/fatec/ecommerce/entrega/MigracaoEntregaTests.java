package br.com.fatec.ecommerce.entrega;

import static org.assertj.core.api.Assertions.*;
import br.com.fatec.ecommerce.TesteDeIntegracao;
import java.sql.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.postgresql.PostgreSQLContainer;

@TesteDeIntegracao
class MigracaoEntregaTests {
    @Autowired PostgreSQLContainer postgres;
    @Test void upgradeV6PreservaPedidoPagoCobrancaEConfirmacaoSemInventarModalidade()throws Exception{
        String schema="upgrade_entrega";
        Flyway.configure().dataSource(postgres.getJdbcUrl(),postgres.getUsername(),postgres.getPassword()).schemas(schema).defaultSchema(schema).target("6").load().migrate();
        try(var c=DriverManager.getConnection(postgres.getJdbcUrl(),postgres.getUsername(),postgres.getPassword());var s=c.createStatement()){
            s.execute("SET search_path TO "+schema);
            s.execute("INSERT INTO usuario(id,email,senha_hash) VALUES(901,'legadoentrega@teste.local','hash')");
            s.execute("INSERT INTO cliente(id,usuario_id,nome,cpf) VALUES(901,901,'Legado','12345678909')");
            s.execute("INSERT INTO pedido(id,cliente_id,situacao,entrega_cep,entrega_logradouro,entrega_numero,entrega_bairro,entrega_cidade,entrega_uf,subtotal,total_descontos,frete,total,criado_em) VALUES(901,901,'PAGO','01001000','Rua','1','Centro','SP','SP',100,0,15,115,now())");
            s.execute("INSERT INTO cobranca(pedido_id,fornecedor,forma,total,id_transacao_externa,situacao,criado_em,atualizado_em) VALUES(901,'pix-simulado','PIX',115,'TX-LEGADO','APROVADO',now(),now())");
            s.execute("INSERT INTO confirmacao_pagamento VALUES('TX-LEGADO','APROVADO',now())");
        }
        Flyway.configure().dataSource(postgres.getJdbcUrl(),postgres.getUsername(),postgres.getPassword()).schemas(schema).defaultSchema(schema).load().migrate();
        try(var c=DriverManager.getConnection(postgres.getJdbcUrl(),postgres.getUsername(),postgres.getPassword());var s=c.createStatement()){
            s.execute("SET search_path TO "+schema);
            try(var r=s.executeQuery("SELECT frete,total,opcao_entrega_codigo,opcao_entrega_prazo_dias FROM pedido WHERE id=901")){
                assertThat(r.next()).isTrue();assertThat(r.getBigDecimal(1)).isEqualByComparingTo("15");assertThat(r.getBigDecimal(2)).isEqualByComparingTo("115");assertThat(r.getString(3)).isNull();assertThat(r.getObject(4)).isNull();
            }
            try(var r=s.executeQuery("SELECT c.total,f.resultado FROM cobranca c JOIN confirmacao_pagamento f USING(id_transacao_externa)")){
                assertThat(r.next()).isTrue();assertThat(r.getBigDecimal(1)).isEqualByComparingTo("115");assertThat(r.getString(2)).isEqualTo("APROVADO");
            }
            assertThatThrownBy(()->s.execute("UPDATE pedido SET opcao_entrega_codigo='parcial' WHERE id=901")).isInstanceOf(SQLException.class);
        }
    }
}
