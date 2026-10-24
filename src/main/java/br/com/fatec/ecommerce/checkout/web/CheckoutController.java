package br.com.fatec.ecommerce.checkout.web;

import br.com.fatec.ecommerce.checkout.aplicacao.CheckoutFacade;
import br.com.fatec.ecommerce.checkout.aplicacao.ConfirmacaoCheckout;
import br.com.fatec.ecommerce.checkout.aplicacao.PreviaCheckout;
import br.com.fatec.ecommerce.pedido.web.PedidoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Cliente da Facade de checkout: traduz HTTP e deixa a coordenacao transacional na aplicacao. */
@RestController
@RequestMapping("/api/clientes/me/checkout")
@PreAuthorize("hasRole('CLIENTE')")
@Tag(name = "Checkout", description = "Exige CLIENTE e escolha explicita de entrega. Cotacao e descontos sao revalidados; "
        + "o comprador inicia o pagamento separadamente apos concluir.")
@ApiResponse(responseCode = "400", description = "Dados invalidos", content = @Content(
        schema = @Schema(implementation = ProblemDetail.class)))
@ApiResponse(responseCode = "401", description = "Autenticacao necessaria")
@ApiResponse(responseCode = "403", description = "Perfil sem CLIENTE")
@ApiResponse(responseCode = "404", description = "Endereco inexistente, inativo ou alheio")
@ApiResponse(responseCode = "409", description = "Carrinho vazio, item indisponivel, alteracao comercial "
        + "(divergencias e previaAtualizada), estoque insuficiente ou conflito concorrente",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
public class CheckoutController {
    public record ConsultaFretesRequest(@NotNull @Positive Long enderecoId) { }
    public record FretesResponse(java.util.List<br.com.fatec.ecommerce.entrega.dominio.OpcaoEntrega> opcoes) { }
    public record PreviaCheckoutRequest(@NotNull @Positive Long enderecoId,
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=128) String opcaoEntrega) { }
    public record ConcluirCheckoutRequest(@NotNull @Positive Long enderecoId,
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=128) String opcaoEntrega,
            @NotNull @Valid ConfirmacaoCheckout confirmacao) { }

    private final CheckoutFacade checkout;

    public CheckoutController(CheckoutFacade checkout) { this.checkout = checkout; }

    @PostMapping("/fretes")
    @Operation(summary = "Consulta opcoes de entrega para endereco proprio", description = "Custos brutos e prazos estimados em dias uteis apos aprovacao. Nao reserva nem consome cupom.")
    @ApiResponse(responseCode="503", description="Provedores de entrega indisponiveis; tente novamente")
    public FretesResponse fretes(@Valid @RequestBody ConsultaFretesRequest request) {
        return new FretesResponse(checkout.fretes(request.enderecoId()));
    }

    @PostMapping("/previa")
    @Operation(summary = "Consulta a previa atual sem reservar estoque",
            description = "Guarde confirmacao para reenviar na conclusao. Insuficiencias sao informativas.")
    public PreviaCheckout previa(@Valid @RequestBody PreviaCheckoutRequest request) {
        return checkout.previa(request.enderecoId(), request.opcaoEntrega());
    }

    @PostMapping
    @Operation(summary = "Confirma a compra e reserva o estoque",
            description = "Revalida o detalhamento inteiro. Divergencias retornam 409 para nova confirmacao. "
                    + "Pedido, reserva e esvaziamento do carrinho sao atomicos.")
    @ApiResponse(responseCode = "201", description = "Pedido aguardando pagamento",
            content = @Content(schema = @Schema(implementation = PedidoResponse.class)))
    public ResponseEntity<PedidoResponse> concluir(@Valid @RequestBody ConcluirCheckoutRequest request) {
        var pedido = checkout.concluir(request.enderecoId(), request.opcaoEntrega(), request.confirmacao());
        return ResponseEntity.created(URI.create("/api/clientes/me/pedidos/" + pedido.getId()))
                .body(PedidoResponse.de(pedido));
    }
}
