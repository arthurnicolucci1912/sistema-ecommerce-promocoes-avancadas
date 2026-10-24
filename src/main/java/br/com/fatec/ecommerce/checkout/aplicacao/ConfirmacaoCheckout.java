package br.com.fatec.ecommerce.checkout.aplicacao;

import br.com.fatec.ecommerce.compartilhado.dominio.DadoInvalidoException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/** Expectativa comercial do cliente. Nunca e usada como fonte dos valores gravados no pedido. */
public record ConfirmacaoCheckout(@NotNull @PositiveOrZero Long versaoCarrinho,
        @NotEmpty List<@NotNull @Valid Linha> itens, @NotNull List<@NotNull @Valid Desconto> descontos,
        @NotNull @PositiveOrZero @Digits(integer = 12, fraction = 2) BigDecimal subtotal,
        @NotNull @PositiveOrZero @Digits(integer = 12, fraction = 2) BigDecimal frete,
        @NotNull @PositiveOrZero @Digits(integer = 12, fraction = 2) BigDecimal total,
        @NotNull @Valid Endereco endereco, br.com.fatec.ecommerce.promocao.dominio.SituacaoCupom cupom,
        @Valid Entrega opcaoEntrega, @PositiveOrZero @Digits(integer=12, fraction=2) BigDecimal freteLiquido) {
    /** Compatibilidade de construtores internos; a API HTTP exige escolha de entrega. */
    public ConfirmacaoCheckout(Long versaoCarrinho, List<Linha> itens, List<Desconto> descontos,
            BigDecimal subtotal, BigDecimal frete, BigDecimal total, Endereco endereco,
            br.com.fatec.ecommerce.promocao.dominio.SituacaoCupom cupom) {
        this(versaoCarrinho,itens,descontos,subtotal,frete,total,endereco,cupom,null,frete);
    }
    public record Entrega(@NotBlank @Size(max=128) String codigo, @NotBlank @Size(max=80) String fornecedor,
            @NotBlank @Size(max=40) String modalidade, @Positive int prazoDias) {}
    public ConfirmacaoCheckout(Long versaoCarrinho, List<Linha> itens, List<Desconto> descontos,
            BigDecimal subtotal, BigDecimal frete, BigDecimal total, Endereco endereco) {
        this(versaoCarrinho, itens, descontos, subtotal, frete, total, endereco, null);
    }

    public record Linha(@NotBlank String sku, @NotBlank String nome, String descricaoVariacao,
            @Positive int quantidade,
            @NotNull @Positive @Digits(integer = 12, fraction = 2) BigDecimal precoUnitario,
            @NotNull @Positive @Digits(integer = 12, fraction = 2) BigDecimal subtotal) {
        public Linha {
            precoUnitario = normalizar(precoUnitario);
            subtotal = normalizar(subtotal);
        }
    }

    public record Desconto(@NotBlank String nome, @NotBlank String alvo,
            @NotNull @PositiveOrZero @Digits(integer = 12, fraction = 2) BigDecimal valor, String origem, Long referenciaId, String codigo) {
        public Desconto(String nome, String alvo, BigDecimal valor) { this(nome, alvo, valor, null, null, null); }
        public Desconto { valor = normalizar(valor); }
    }

    /** Copia dos dados de entrega; alteracao posterior no cadastro exige nova confirmacao. */
    public record Endereco(String apelido, @NotBlank String cep, @NotBlank String logradouro,
            @NotBlank String numero, String complemento, @NotBlank String bairro,
            @NotBlank String cidade, @NotBlank String uf) { }

    public ConfirmacaoCheckout {
        if (itens != null) {
            itens = itens.stream().sorted(Comparator.comparing(Linha::sku,
                    Comparator.nullsFirst(Comparator.naturalOrder()))).toList();
            if (itens.stream().map(Linha::sku).distinct().count() != itens.size()) {
                throw new DadoInvalidoException("A confirmação não pode repetir SKUs.");
            }
        }
        if (descontos != null) {
            descontos = descontos.stream().sorted(Comparator.comparing(Desconto::nome,
                    Comparator.nullsFirst(Comparator.naturalOrder())).thenComparing(Desconto::alvo,
                    Comparator.nullsFirst(Comparator.naturalOrder())).thenComparing(Desconto::origem,
                    Comparator.nullsFirst(Comparator.naturalOrder())).thenComparing(Desconto::referenciaId,
                    Comparator.nullsFirst(Comparator.naturalOrder())).thenComparing(Desconto::codigo,
                    Comparator.nullsFirst(Comparator.naturalOrder())).thenComparing(Desconto::valor,
                    Comparator.nullsFirst(Comparator.naturalOrder()))).toList();
        }
        subtotal = normalizar(subtotal);
        frete = normalizar(frete);
        total = normalizar(total);
        freteLiquido = normalizar(freteLiquido);
    }

    private static BigDecimal normalizar(BigDecimal valor) {
        return valor == null ? null : valor.stripTrailingZeros();
    }
}
