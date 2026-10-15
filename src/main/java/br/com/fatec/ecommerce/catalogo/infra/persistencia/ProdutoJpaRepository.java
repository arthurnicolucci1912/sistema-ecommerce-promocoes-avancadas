package br.com.fatec.ecommerce.catalogo.infra.persistencia;

import br.com.fatec.ecommerce.catalogo.dominio.Produto;
import org.springframework.data.jpa.repository.JpaRepository;

interface ProdutoJpaRepository extends JpaRepository<Produto, Long> {
}
