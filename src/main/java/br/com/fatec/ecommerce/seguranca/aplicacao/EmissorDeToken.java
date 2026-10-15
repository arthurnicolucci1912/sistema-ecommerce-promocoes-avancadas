package br.com.fatec.ecommerce.seguranca.aplicacao;

import br.com.fatec.ecommerce.seguranca.dominio.Usuario;

/** Porta de emissao de token de acesso (implementada em {@code infra.config}). */
public interface EmissorDeToken {

    TokenEmitido emitir(Usuario usuario);
}
