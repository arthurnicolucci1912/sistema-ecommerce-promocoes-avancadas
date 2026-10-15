package br.com.fatec.ecommerce.catalogo.infra.persistencia;

import br.com.fatec.ecommerce.catalogo.dominio.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

interface CategoriaJpaRepository extends JpaRepository<Categoria, Long> {
}
