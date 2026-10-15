package br.com.fatec.ecommerce.pedido.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.DadoInvalidoException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Endereco de entrega copiado para o pedido no momento da compra. E uma copia: nao referencia o endereco do
 * cliente, que pode ser alterado ou inativado depois sem afetar o pedido. Imutavel.
 */
@Embeddable
public class EnderecoEntrega {

    @Column(name = "entrega_apelido", updatable = false)
    private String apelido;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "entrega_cep", nullable = false, updatable = false, length = 8)
    private String cep;

    @Column(name = "entrega_logradouro", nullable = false, updatable = false)
    private String logradouro;

    @Column(name = "entrega_numero", nullable = false, updatable = false)
    private String numero;

    @Column(name = "entrega_complemento", updatable = false)
    private String complemento;

    @Column(name = "entrega_bairro", nullable = false, updatable = false)
    private String bairro;

    @Column(name = "entrega_cidade", nullable = false, updatable = false)
    private String cidade;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "entrega_uf", nullable = false, updatable = false, length = 2)
    private String uf;

    protected EnderecoEntrega() {
    }

    public EnderecoEntrega(String apelido, String cep, String logradouro, String numero, String complemento,
            String bairro, String cidade, String uf) {
        String cepNormalizado = cep == null ? "" : cep.trim().replace("-", "");
        if (!cepNormalizado.matches("\\d{8}")) {
            throw new DadoInvalidoException("Endereço de entrega inválido: CEP deve ter 8 dígitos.");
        }
        if (uf == null || !uf.trim().matches("[A-Za-z]{2}")) {
            throw new DadoInvalidoException("Endereço de entrega inválido: UF deve ter 2 letras.");
        }
        this.apelido = vazioParaNulo(apelido);
        this.cep = cepNormalizado;
        this.logradouro = obrigatorio(logradouro, "logradouro");
        this.numero = obrigatorio(numero, "número");
        this.complemento = vazioParaNulo(complemento);
        this.bairro = obrigatorio(bairro, "bairro");
        this.cidade = obrigatorio(cidade, "cidade");
        this.uf = uf.trim().toUpperCase();
    }

    private static String obrigatorio(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new DadoInvalidoException("Endereço de entrega inválido: " + campo + " é obrigatório.");
        }
        return valor.trim();
    }

    private static String vazioParaNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    public String getApelido() {
        return apelido;
    }

    public String getCep() {
        return cep;
    }

    public String getLogradouro() {
        return logradouro;
    }

    public String getNumero() {
        return numero;
    }

    public String getComplemento() {
        return complemento;
    }

    public String getBairro() {
        return bairro;
    }

    public String getCidade() {
        return cidade;
    }

    public String getUf() {
        return uf;
    }
}
