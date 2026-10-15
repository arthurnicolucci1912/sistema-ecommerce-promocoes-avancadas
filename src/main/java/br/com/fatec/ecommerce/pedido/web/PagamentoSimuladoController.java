package br.com.fatec.ecommerce.pedido.web;

import br.com.fatec.ecommerce.pedido.aplicacao.TransicionarPedido;
import br.com.fatec.ecommerce.pedido.dominio.Pedido;
import br.com.fatec.ecommerce.pedido.dominio.Responsavel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * TEMPORARIO: simula o resultado do pagamento enquanto nao ha integracao real. Deve ser removido na etapa 6
 * (change {@code pagamento-idempotencia}), quando o gateway de pagamento passa a aprovar ou recusar o pedido.
 *
 * @deprecated temporario ate a etapa 6 (pagamento-idempotencia)
 */
@Deprecated
@RestController
@RequestMapping("/api/admin/pedidos")
@Tag(name = "Pagamento simulado (temporário)",
        description = "TEMPORÁRIO até a etapa 6 (pagamento-idempotencia). Somente ADMIN.")
public class PagamentoSimuladoController {

    static final String MOTIVO = "pagamento simulado";

    private final TransicionarPedido transicionar;

    public PagamentoSimuladoController(TransicionarPedido transicionar) {
        this.transicionar = transicionar;
    }

    @PostMapping("/{id}/pagamento-simulado")
    @PreAuthorize("hasRole('ADMIN')")
    @Deprecated
    @Operation(summary = "Simula o resultado do pagamento",
            description = "TEMPORÁRIO até a etapa 6 (pagamento-idempotencia). Aprova (aprovado = true) ou recusa "
                    + "(aprovado = false) o pagamento de um pedido em AGUARDANDO_PAGAMENTO. O responsável "
                    + "registrado no histórico é o SISTEMA, com o motivo \"pagamento simulado\". Em outro estado "
                    + "responde 409; um pedido PAGAMENTO_RECUSADO não é reaberto.",
            deprecated = true)
    public PedidoResponse simular(@PathVariable Long id, @Valid @RequestBody PagamentoSimuladoRequest request) {
        Pedido pedido = request.aprovado()
                ? transicionar.aprovarPagamento(id, Responsavel.sistema(), MOTIVO)
                : transicionar.recusarPagamento(id, Responsavel.sistema(), MOTIVO);
        return PedidoResponse.de(pedido);
    }
}
