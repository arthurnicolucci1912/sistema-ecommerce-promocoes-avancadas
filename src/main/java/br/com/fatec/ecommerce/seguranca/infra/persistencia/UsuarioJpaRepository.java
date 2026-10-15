package br.com.fatec.ecommerce.seguranca.infra.persistencia;

import br.com.fatec.ecommerce.seguranca.dominio.Perfil;
import br.com.fatec.ecommerce.seguranca.dominio.Usuario;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface UsuarioJpaRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("select u from Usuario u where u.id in (select u2.id from Usuario u2 join u2.perfis p where p <> :cliente) order by u.id")
    List<Usuario> findAdministrativos(@Param("cliente") Perfil cliente);

    /** Trava as linhas dos ADMIN ativos (ordem por id evita deadlock entre transacoes). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.ativo = true and :admin member of u.perfis order by u.id")
    List<Usuario> findAdminsAtivosParaAtualizacao(@Param("admin") Perfil admin);

    @Query("select count(u) from Usuario u where u.ativo = true and :admin member of u.perfis")
    long contarAdminsAtivos(@Param("admin") Perfil admin);
}
