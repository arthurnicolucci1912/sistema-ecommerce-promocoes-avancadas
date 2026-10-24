package br.com.fatec.ecommerce.carrinho.infra.persistencia;

import br.com.fatec.ecommerce.carrinho.dominio.Carrinho;
import br.com.fatec.ecommerce.carrinho.dominio.CarrinhoRepository;
import br.com.fatec.ecommerce.compartilhado.dominio.ConflitoException;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

interface CarrinhoJpaRepository extends JpaRepository<Carrinho, Long> {
    Optional<Carrinho> findByClienteId(Long clienteId);
}

/** Adaptador JPA; conflitos de primeira criacao ou versao pedem releitura pelo cliente. */
@Repository
class CarrinhoRepositoryJpa implements CarrinhoRepository {
    private final CarrinhoJpaRepository jpa;

    CarrinhoRepositoryJpa(CarrinhoJpaRepository jpa) { this.jpa = jpa; }

    @Override
    public Optional<Carrinho> buscarPorClienteId(Long clienteId) {
        return jpa.findByClienteId(clienteId);
    }

    @Override
    public Carrinho salvar(Carrinho carrinho) {
        try {
            return jpa.saveAndFlush(carrinho);
        } catch (OptimisticLockingFailureException erro) {
            throw new ConflitoException("O carrinho mudou em outra requisição. Consulte novamente.");
        } catch (DataIntegrityViolationException erro) {
            if (erro.getMostSpecificCause().getMessage().contains("uk_carrinho_cliente")) {
                throw new ConflitoException("O carrinho foi criado em outra requisição. Consulte novamente.");
            }
            throw erro;
        }
    }
}
