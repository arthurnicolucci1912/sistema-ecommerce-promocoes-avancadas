package br.com.fatec.ecommerce.pedido.web;

import br.com.fatec.ecommerce.compartilhado.web.PaginaResponse;
import br.com.fatec.ecommerce.pedido.aplicacao.CancelarPedidoDoCliente;
import br.com.fatec.ecommerce.pedido.aplicacao.ConsultarPedidos;
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

@RestController
@RequestMapping("/api/clientes/me/pedidos")
@PreAuthorize("hasRole('CLIENTE')")
@Tag(name = "Pedidos do cliente",
        description = "Exige o perfil CLIENTE. O pedido é sempre buscado pelo cliente do token: pedido inexistente "
                + "ou de outro cliente responde 404.")
public class PedidosDoClienteController {

    private final ConsultarPedidos consultar;
    private final CancelarPedidoDoCliente cancelar;

    public PedidosDoClienteController(ConsultarPedidos consultar, CancelarPedidoDoCliente cancelar) {
        this.consultar = consultar;
        this.cancelar = cancelar;
    }

    @GetMapping
    @Operation(summary = "Lista os próprios pedidos",
            description = "Paginado (page a partir de 0; size padrão 20, máximo 100), do mais recente para o mais "
                    + "antigo. Devolve um resumo de cada pedido: id, situação, total e data de criação.")
    public PaginaResponse<ResumoPedidoResponse> listar(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + ConsultarPedidos.TAMANHO_PADRAO) int size) {
        return PaginaResponse.de(consultar.listarDoCliente(UsuarioDoToken.id(jwt), page, size),
                ResumoPedidoResponse::de);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta um pedido",
            description = "Itens, descontos, frete, totais, endereço de entrega, situação e histórico cronológico "
                    + "com o responsável de cada transição.")
    public PedidoResponse consultar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return PedidoResponse.de(consultar.consultarDoCliente(UsuarioDoToken.id(jwt), id));
    }

    @PostMapping("/{id}/cancelamento")
    @Operation(summary = "Cancela o próprio pedido",
            description = "Motivo obrigatório (400 sem ele). O cliente só cancela pedido em CRIADO, "
                    + "AGUARDANDO_PAGAMENTO ou PAGAMENTO_RECUSADO; nos demais estados responde 409 com "
                    + "situacaoAtual e operacao.")
    public PedidoResponse cancelar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
            @Valid @RequestBody MotivoRequest request) {
        return PedidoResponse.de(cancelar.executar(UsuarioDoToken.id(jwt), id, request.motivo()));
    }
}
