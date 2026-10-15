package br.com.fatec.ecommerce.seguranca.dominio;

/** Perfis de acesso do sistema. Um usuario pode acumular mais de um perfil administrativo. */
public enum Perfil {
    CLIENTE,
    ADMIN,
    OPERADOR_COMERCIAL,
    ESTOQUE,
    PROMOCOES;

    /** Todo perfil, exceto {@link #CLIENTE}, e administrativo. */
    public boolean ehAdministrativo() {
        return this != CLIENTE;
    }
}
