package br.com.fatec.ecommerce.cliente.infra.persistencia;

import br.com.fatec.ecommerce.cliente.dominio.Endereco;
import br.com.fatec.ecommerce.cliente.dominio.EnderecoRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** Adaptador JPA da porta {@link EnderecoRepository}. */
@Repository
class EnderecoRepositoryJpa implements EnderecoRepository {

    private final EnderecoJpaRepository jpa;

    EnderecoRepositoryJpa(EnderecoJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Endereco salvar(Endereco endereco) {
        return jpa.saveAndFlush(endereco);
    }

    @Override
    public List<Endereco> listarAtivos(Long clienteId) {
        return jpa.findByClienteIdAndAtivoTrueOrderByCriadoEmAscIdAsc(clienteId);
    }

    @Override
    public Optional<Endereco> buscarAtivo(Long id, Long clienteId) {
        return jpa.findByIdAndClienteIdAndAtivoTrue(id, clienteId);
    }
}
