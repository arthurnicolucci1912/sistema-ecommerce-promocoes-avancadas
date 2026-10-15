package br.com.fatec.ecommerce.pedido.web;

import br.com.fatec.ecommerce.compartilhado.web.PaginaResponse;
import br.com.fatec.ecommerce.pedido.aplicacao.ConsultarPedidos;
import br.com.fatec.ecommerce.pedido.aplicacao.TransicionarPedido;
import br.com.fatec.ecommerce.pedido.dominio.Responsavel;
import br.com.fatec.ecommerce.pedido.dominio.SituacaoPedido;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gestão administrativa de pedidos. Os perfis de cada rota são conferidos na cadeia de filtros
 * ({@code ConfiguracaoSeguranca}) e novamente aqui; as regras de transição e de cancelamento ficam nos estados do
 * pedido, não neste controller.
 */
@RestController
@RequestMapping("/api/admin/pedidos")
@Tag(name = "Pedidos (administração)",
        description = "Consulta: ADMIN e OPERADOR_COMERCIAL. Preparação, envio e entrega: ADMIN e ESTOQUE. "
                + "Cancelamento: ADMIN e OPERADOR_COMERCIAL. Transição não permitida no estado atual responde 409 "
                + "com situacaoAtual e operacao.")
public class PedidosAdministrativosController {

    private final ConsultarPedidos consultar;
    private final TransicionarPedido transicionar;

    public PedidosAdministrativosController(ConsultarPedidos consultar, TransicionarPedido transicionar) {
        this.consultar = consultar;
        this.transicionar = transicionar;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR_COMERCIAL')")
    @Operation(summary = "Lista pedidos de qualquer cliente",
            description = "Filtro opcional por situacao. Paginado (page a partir de 0; size padrão 20, máximo 100), "
                    + "do mais recente para o mais antigo.")
    public PaginaResponse<ResumoPedidoResponse> listar(@RequestParam(required = false) SituacaoPedido situacao,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + ConsultarPedidos.TAMANHO_PADRAO) int size) {
        return PaginaResponse.de(consultar.listar(situacao, page, size), ResumoPedidoResponse::de);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR_COMERCIAL')")
    @Operation(summary = "Consulta o detalhe de um pedido")
    public PedidoResponse consultar(@PathVariable Long id) {
        return PedidoResponse.de(consultar.consultar(id));
    }

    @PostMapping("/{id}/preparacao")
    @PreAuthorize("hasAnyRole('ADMIN', 'ESTOQUE')")
    @Operation(summary = "Inicia a preparação", description = "Leva um pedido PAGO a EM_PREPARACAO.")
    public PedidoResponse iniciarPreparacao(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return PedidoResponse.de(transicionar.iniciarPreparacao(id, responsavel(jwt)));
    }

    @PostMapping("/{id}/envio")
    @PreAuthorize("hasAnyRole('ADMIN', 'ESTOQUE')")
    @Operation(summary = "Registra o envio", description = "Leva um pedido EM_PREPARACAO a ENVIADO.")
    public PedidoResponse enviar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return PedidoResponse.de(transicionar.enviar(id, responsavel(jwt)));
    }

    @PostMapping("/{id}/entrega")
    @PreAuthorize("hasAnyRole('ADMIN', 'ESTOQUE')")
    @Operation(summary = "Registra a entrega", description = "Leva um pedido ENVIADO a ENTREGUE.")
    public PedidoResponse entregar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return PedidoResponse.de(transicionar.entregar(id, responsavel(jwt)));
    }

    @PostMapping("/{id}/cancelamento")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR_COMERCIAL')")
    @Operation(summary = "Cancela um pedido",
            description = "Motivo obrigatório (400 sem ele). Cancela qualquer pedido em estado que admite "
                    + "cancelamento, inclusive PAGO e EM_PREPARACAO; ENVIADO, ENTREGUE e CANCELADO respondem 409.")
    public PedidoResponse cancelar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
            @Valid @RequestBody MotivoRequest request) {
        return PedidoResponse.de(transicionar.cancelar(id, responsavel(jwt), request.motivo()));
    }

    /** D5: qualquer perfil administrativo age como ADMINISTRATIVO. */
    private static Responsavel responsavel(Jwt jwt) {
        return Responsavel.administrativo(UsuarioDoToken.id(jwt));
    }
}
