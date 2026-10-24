package br.com.fatec.ecommerce.auditoria.aplicacao;

import br.com.fatec.ecommerce.auditoria.dominio.ConsultarRegistrosAuditoria;
import br.com.fatec.ecommerce.auditoria.dominio.FiltroAuditoria;
import br.com.fatec.ecommerce.auditoria.dominio.RegistroAuditoriaInfo;
import br.com.fatec.ecommerce.compartilhado.dominio.Pagina;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Consulta da trilha restrita a ADMIN também quando chamada fora do controller. */
@Service
@PreAuthorize("hasRole('ADMIN')")
@Transactional(readOnly = true)
public class ConsultarAuditoria {
    private final ConsultarRegistrosAuditoria registros;

    public ConsultarAuditoria(ConsultarRegistrosAuditoria registros) {
        this.registros = registros;
    }

    public Pagina<RegistroAuditoriaInfo> consultar(FiltroAuditoria filtro) {
        return registros.consultar(filtro);
    }
}
