package br.com.fatec.ecommerce.cliente.infra.persistencia;

import br.com.fatec.ecommerce.cliente.dominio.Cliente;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface ClienteJpaRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByUsuarioId(Long usuarioId);

    boolean existsByCpf(String cpf);
}
