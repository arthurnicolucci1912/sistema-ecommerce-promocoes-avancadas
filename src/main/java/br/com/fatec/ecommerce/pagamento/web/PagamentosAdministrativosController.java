package br.com.fatec.ecommerce.pagamento.web;

import br.com.fatec.ecommerce.pagamento.aplicacao.*;
import br.com.fatec.ecommerce.pagamento.dominio.ResultadoCobranca;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController @Validated @PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/admin/pagamentos")
@Tag(name="Integração de pagamentos simulados", description="Confirmações e recuperação de estorno restritas ao ADMIN.")
public class PagamentosAdministrativosController {
    public record ConfirmacaoRequest(@NotBlank @Size(max=160) String idTransacaoExterna, @NotNull ResultadoCobranca.Situacao situacao) {}
    private final ConfirmarPagamento confirmar;
    private final EstornarPagamento estornar;
    public PagamentosAdministrativosController(ConfirmarPagamento confirmar, EstornarPagamento estornar) {
        this.confirmar = confirmar; this.estornar = estornar;
    }
    @PostMapping("/confirmacoes")
    public PagamentoResponse confirmar(@RequestBody @Valid ConfirmacaoRequest request) {
        return PagamentoResponse.de(confirmar.executar(request.idTransacaoExterna(), request.situacao()), null);
    }
    @PostMapping("/{pedidoId}/estorno/tentativas")
    public PagamentoResponse.EstornoResponse tentar(@PathVariable @Positive Long pedidoId) {
        return PagamentoResponse.EstornoResponse.de(estornar.executar(pedidoId, true));
    }
}
