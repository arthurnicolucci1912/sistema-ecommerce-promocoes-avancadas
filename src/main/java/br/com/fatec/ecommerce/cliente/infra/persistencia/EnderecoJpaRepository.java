package br.com.fatec.ecommerce.cliente.infra.persistencia;

import br.com.fatec.ecommerce.cliente.dominio.Endereco;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface EnderecoJpaRepository extends JpaRepository<Endereco, Long> {

    List<Endereco> findByClienteIdAndAtivoTrueOrderByCriadoEmAscIdAsc(Long clienteId);

    Optional<Endereco> findByIdAndClienteIdAndAtivoTrue(Long id, Long clienteId);
}
