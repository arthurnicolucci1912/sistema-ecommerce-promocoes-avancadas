package br.com.fatec.ecommerce.catalogo.infra.persistencia;

import br.com.fatec.ecommerce.compartilhado.dominio.ConflitoException;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Converte a violacao de uma restricao de unicidade do catalogo (SKU e nome de categoria por pai) em
 * {@link ConflitoException} (409). Cobre a corrida entre duas requisicoes que passam juntas pela checagem previa.
 * Qualquer outra violacao de integridade e relancada como esta.
 */
final class ViolacoesDeUnicidade {

    private static final String RESTRICAO_SKU = "uk_item_vendavel_sku";
    private static final String INDICE_NOME_DE_CATEGORIA = "ux_categoria_pai_nome";

    private ViolacoesDeUnicidade() {
    }

    static RuntimeException deSku(DataIntegrityViolationException erro) {
        return converter(erro, RESTRICAO_SKU, "Já existe um item do catálogo com esse SKU.");
    }

    static RuntimeException deNomeDeCategoria(DataIntegrityViolationException erro) {
        return converter(erro, INDICE_NOME_DE_CATEGORIA, "Já existe uma categoria com esse nome no mesmo nível.");
    }

    private static RuntimeException converter(DataIntegrityViolationException erro, String restricao,
            String mensagem) {
        for (Throwable causa = erro; causa != null; causa = causa.getCause()) {
            if (causa instanceof ConstraintViolationException violacao
                    && restricao.equalsIgnoreCase(violacao.getConstraintName())) {
                return new ConflitoException(mensagem);
            }
            if (causa.getMessage() != null && causa.getMessage().contains(restricao)) {
                return new ConflitoException(mensagem);
            }
        }
        return erro;
    }
}
