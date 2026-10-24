package br.com.fatec.ecommerce.notificacao.infra.persistencia;

import br.com.fatec.ecommerce.notificacao.dominio.DestinatariosNotificacao;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Leitura independente da transacao comercial ja encerrada, sem entidades lazy. */
@Repository
public class DestinatariosNotificacaoJdbc implements DestinatariosNotificacao {
    private final JdbcTemplate jdbc;

    public DestinatariosNotificacaoJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public Optional<String> emailDoCliente(Long clienteId) {
        return jdbc.query("SELECT u.email FROM cliente c JOIN usuario u ON u.id = c.usuario_id WHERE c.id = ?",
                (rs, linha) -> rs.getString("email"), clienteId).stream().findFirst();
    }
}
