package br.com.fatec.ecommerce.auditoria.dominio;

/** Porta de escrita somente por inclusão, sem edição ou exclusão. */
public interface RegistrosAuditoria {
    void registrar(RegistroAuditoria registro);
}
