package br.com.fatec.ecommerce.cliente.infra.persistencia;

import br.com.fatec.ecommerce.cliente.dominio.Cliente;
import br.com.fatec.ecommerce.cliente.dominio.ClienteRepository;
import br.com.fatec.ecommerce.cliente.dominio.Cpf;
import br.com.fatec.ecommerce.compartilhado.dominio.ConflitoException;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

/** Adaptador JPA da porta {@link ClienteRepository}. */
@Repository
class ClienteRepositoryJpa implements ClienteRepository {

    private final ClienteJpaRepository jpa;

    ClienteRepositoryJpa(ClienteJpaRepository jpa) {
        this.jpa = jpa;
    }

    /** Um cadastro concorrente com o mesmo CPF passa pela verificacao previa e cai na restricao unica do banco. */
    @Override
    public Cliente salvar(Cliente cliente) {
        try {
            return jpa.saveAndFlush(cliente);
        } catch (DataIntegrityViolationException e) {
            throw new ConflitoException("CPF já cadastrado.");
        }
    }

    @Override
    public Optional<Cliente> buscarPorUsuarioId(Long usuarioId) {
        return jpa.findByUsuarioId(usuarioId);
    }

    @Override
    public boolean existePorCpf(Cpf cpf) {
        return jpa.existsByCpf(cpf.digitos());
    }

    @Override
    public boolean existePorId(Long id) {
        return jpa.existsById(id);
    }
}
