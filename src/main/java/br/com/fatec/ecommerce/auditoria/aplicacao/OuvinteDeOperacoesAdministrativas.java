package br.com.fatec.ecommerce.auditoria.aplicacao;

import br.com.fatec.ecommerce.auditoria.dominio.*;
import br.com.fatec.ecommerce.compartilhado.dominio.OperacaoAdministrativa;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;

/** Observer síncrono: auditoria confirma ou reverte junto da operação original. */
@Component
public class OuvinteDeOperacoesAdministrativas {
    private final RegistrosAuditoria registros;
    public OuvinteDeOperacoesAdministrativas(RegistrosAuditoria registros) { this.registros = registros; }
    @EventListener @Transactional(propagation=Propagation.MANDATORY)
    public void registrar(OperacaoAdministrativa evento) { registros.registrar(new RegistroAuditoria(evento)); }
}
