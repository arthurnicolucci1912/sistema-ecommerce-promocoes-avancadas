package br.com.fatec.ecommerce.infra;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.fatec.ecommerce.TestcontainersConfiguration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@SpringBootTest
class MigracaoV1Tests {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void flywayAplicaV1EmBancoVazioECriaTabelas() {
        List<String> tabelas = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'", String.class);
        assertThat(tabelas).contains("usuario", "usuario_perfil", "cliente", "endereco");

        Boolean sucesso = jdbc.queryForObject(
                "SELECT success FROM flyway_schema_history WHERE version = '1'", Boolean.class);
        assertThat(sucesso).isTrue();
    }

    @Test
    void indiceUnicoParcialGarantePrincipalAtivoUnico() {
        String definicao = jdbc.queryForObject(
                "SELECT indexdef FROM pg_indexes WHERE tablename = 'endereco' "
                        + "AND indexname = 'ux_endereco_principal_ativo'", String.class);
        assertThat(definicao).contains("UNIQUE", "cliente_id").contains("WHERE");
        assertThat(definicao).contains("principal").contains("ativo");
    }

    @Test
    void indiceParcialRejeitaDoisPrincipaisAtivosDoMesmoCliente() {
        jdbc.update("INSERT INTO usuario (email, senha_hash) VALUES ('indice@teste.local', 'x')");
        Long usuarioId = jdbc.queryForObject("SELECT id FROM usuario WHERE email = 'indice@teste.local'", Long.class);
        jdbc.update("INSERT INTO cliente (usuario_id, nome, cpf) VALUES (?, 'Teste', '12345678901')", usuarioId);
        Long clienteId = jdbc.queryForObject("SELECT id FROM cliente WHERE usuario_id = ?", Long.class, usuarioId);
        String insercao = "INSERT INTO endereco (cliente_id, cep, logradouro, numero, bairro, cidade, uf, principal, ativo)"
                + " VALUES (?, '01001000', 'Rua A', '1', 'Centro', 'Sao Paulo', 'SP', ?, ?)";

        jdbc.update(insercao, clienteId, true, true);
        jdbc.update(insercao, clienteId, true, false);
        jdbc.update(insercao, clienteId, false, true);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> jdbc.update(insercao, clienteId, true, true))
                .isInstanceOf(org.springframework.dao.DuplicateKeyException.class);
    }
}
