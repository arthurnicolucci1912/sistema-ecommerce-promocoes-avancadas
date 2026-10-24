package br.com.fatec.ecommerce.carrinho.web;

import br.com.fatec.ecommerce.carrinho.aplicacao.GerenciarCarrinho;
import br.com.fatec.ecommerce.carrinho.aplicacao.ResumoCarrinho;
import br.com.fatec.ecommerce.compartilhado.dominio.DadoInvalidoException;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import org.springframework.web.bind.annotation.*;

/** API do carrinho proprio; nunca recebe clienteId no caminho ou corpo. */
@RestController
@RequestMapping("/api/clientes/me/carrinho")
@Tag(name = "Carrinho do cliente")
public class CarrinhoDoClienteController {
    public record IncluirItem(@NotBlank @Size(max = 100) String sku,
            @NotNull @Positive @Schema(implementation = Integer.class, type = "integer", format = "int32", minimum = "1") BigDecimal quantidade) { }
    public record AlterarQuantidade(
            @NotNull @Positive @Schema(implementation = Integer.class, type = "integer", format = "int32", minimum = "1") BigDecimal quantidade) { }

    private final GerenciarCarrinho carrinho;
    public record AplicarCupom(@NotBlank String codigo) { }

    public CarrinhoDoClienteController(GerenciarCarrinho carrinho) { this.carrinho = carrinho; }

    @GetMapping
    public ResumoCarrinho consultar() { return carrinho.consultar(); }

    @PutMapping("/cupom")
    public ResumoCarrinho aplicarCupom(@Valid @RequestBody AplicarCupom pedido) { return carrinho.aplicarCupom(pedido.codigo()); }

    @DeleteMapping("/cupom")
    public ResumoCarrinho removerCupom() { return carrinho.removerCupom(); }

    @PostMapping("/itens")
    public ResumoCarrinho incluir(@Valid @RequestBody IncluirItem pedido) {
        return carrinho.incluir(pedido.sku(), inteiro(pedido.quantidade()));
    }

    @PutMapping("/itens/{sku}")
    public ResumoCarrinho alterar(@PathVariable String sku, @Valid @RequestBody AlterarQuantidade pedido) {
        return carrinho.alterarQuantidade(sku, inteiro(pedido.quantidade()));
    }

    @DeleteMapping("/itens/{sku}")
    public ResumoCarrinho remover(@PathVariable String sku) { return carrinho.remover(sku); }

    private static int inteiro(BigDecimal valor) {
        try { return valor.intValueExact(); }
        catch (ArithmeticException erro) { throw new DadoInvalidoException("A quantidade deve ser um inteiro positivo até 2147483647."); }
    }
}
