package br.com.fatec.ecommerce.auditoria.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.Pagina;

/** Porta de consulta paginada no banco, sem operações de edição ou exclusão. */
public interface ConsultarRegistrosAuditoria {
    Pagina<RegistroAuditoriaInfo> consultar(FiltroAuditoria filtro);
}
