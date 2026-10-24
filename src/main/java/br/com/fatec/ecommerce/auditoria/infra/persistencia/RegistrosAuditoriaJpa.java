package br.com.fatec.ecommerce.auditoria.infra.persistencia;

import br.com.fatec.ecommerce.auditoria.dominio.*;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

@Repository
class RegistrosAuditoriaJpa implements RegistrosAuditoria {
    private final EntityManager em;
    RegistrosAuditoriaJpa(EntityManager em) { this.em = em; }
    public void registrar(RegistroAuditoria registro) { em.persist(registro); em.flush(); }
}
