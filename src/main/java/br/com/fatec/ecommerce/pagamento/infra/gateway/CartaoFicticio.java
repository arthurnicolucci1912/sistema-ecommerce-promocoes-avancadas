package br.com.fatec.ecommerce.pagamento.infra.gateway;

/** API do fornecedor de cartao: centavos, codigo de retorno e autorizacao. */
public class CartaoFicticio {
    public record Autorizacao(String numero, int codigo) {}
    public Autorizacao autorizar(String chave, long centavos, boolean aprovado) {
        if (centavos < 0) throw new IllegalArgumentException("Valor inválido.");
        return new Autorizacao("CARD-" + chave, aprovado ? 0 : 51);
    }
    public Autorizacao devolver(String autorizacao, long centavos, boolean falhar) {
        return new Autorizacao("REF-" + autorizacao, falhar ? 96 : 0);
    }
}
