package br.com.fatec.ecommerce.pagamento.web;

import br.com.fatec.ecommerce.pagamento.aplicacao.PagarPedido;
import br.com.fatec.ecommerce.pagamento.dominio.FormaPagamento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController @Validated
@RequestMapping("/api/clientes/me/pedidos/{id}/pagamento")
@Tag(name="Pagamentos do cliente")
public class PagamentosDoClienteController {
    public record IniciarRequest(@NotNull FormaPagamento forma) {}
    private final PagarPedido pagar;
    public PagamentosDoClienteController(PagarPedido pagar) { this.pagar = pagar; }
    @PostMapping
    public ResponseEntity<PagamentoResponse> iniciar(@PathVariable @Positive Long id, @RequestBody @Valid IniciarRequest request) {
        var inicio = pagar.iniciar(id, request.forma());
        var resposta = PagamentoResponse.de(inicio.cobranca(), pagar.consultar(id).estorno());
        return inicio.criada() ? ResponseEntity.created(URI.create("/api/clientes/me/pedidos/" + id + "/pagamento")).body(resposta)
                : ResponseEntity.ok(resposta);
    }
    @GetMapping
    public PagamentoResponse consultar(@PathVariable @Positive Long id) {
        var consulta = pagar.consultar(id);
        return PagamentoResponse.de(consulta.cobranca(), consulta.estorno());
    }
}
