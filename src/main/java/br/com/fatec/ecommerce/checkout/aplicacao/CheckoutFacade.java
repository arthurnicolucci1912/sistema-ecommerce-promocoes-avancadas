package br.com.fatec.ecommerce.checkout.aplicacao;

import br.com.fatec.ecommerce.carrinho.dominio.CarrinhoRepository;
import br.com.fatec.ecommerce.cliente.dominio.ClienteRepository;
import br.com.fatec.ecommerce.compartilhado.dominio.RecursoNaoEncontradoException;
import br.com.fatec.ecommerce.estoque.aplicacao.ReservarEstoque;
import br.com.fatec.ecommerce.pedido.aplicacao.ConfirmarReservaDoPedido;
import br.com.fatec.ecommerce.pedido.aplicacao.CriarPedido;
import br.com.fatec.ecommerce.pedido.dominio.Pedido;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * Pattern Facade: coordena revalidacao, pedido, reserva e consumo da selecao numa unica transacao.
 * O cliente HTTP conhece esta operacao; pedido e estoque continuam independentes entre si.
 * Ver docs/04-design-patterns.md, secao 3.9.
 */
@Service
@Validated
@PreAuthorize("hasRole('CLIENTE')")
@Transactional
public class CheckoutFacade {
    private final ClienteRepository clientes;
    private final RevalidarCheckout revalidar;
    private final CriarPedido criar;
    private final ReservarEstoque reservar;
    private final ConfirmarReservaDoPedido confirmar;
    private final CarrinhoRepository carrinhos;
    private final br.com.fatec.ecommerce.promocao.aplicacao.ConfirmarUsoCupom usoCupom;

    public CheckoutFacade(ClienteRepository clientes, RevalidarCheckout revalidar, CriarPedido criar,
            ReservarEstoque reservar, ConfirmarReservaDoPedido confirmar, CarrinhoRepository carrinhos,
            br.com.fatec.ecommerce.promocao.aplicacao.ConfirmarUsoCupom usoCupom) {
        this.usoCupom = usoCupom;
        this.clientes = clientes;
        this.revalidar = revalidar;
        this.criar = criar;
        this.reservar = reservar;
        this.confirmar = confirmar;
        this.carrinhos = carrinhos;
    }

    @Transactional(readOnly = true)
    public PreviaCheckout previa(@NotNull @Positive Long enderecoId) {
        return revalidar.executar(clienteId(), enderecoId).previa();
    }

    @Transactional(readOnly = true)
    public java.util.List<br.com.fatec.ecommerce.entrega.dominio.OpcaoEntrega> fretes(@NotNull @Positive Long enderecoId) {
        return revalidar.cotar(clienteId(), enderecoId);
    }

    @Transactional(readOnly = true)
    public PreviaCheckout previa(@NotNull @Positive Long enderecoId,
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=128) String opcaoEntrega) {
        return revalidar.executar(clienteId(), enderecoId, opcaoEntrega).previa();
    }

    public Pedido concluir(@NotNull @Positive Long enderecoId, @NotNull @Valid ConfirmacaoCheckout confirmacao) {
        return concluir(enderecoId, confirmacao == null || confirmacao.opcaoEntrega() == null ? null : confirmacao.opcaoEntrega().codigo(), confirmacao);
    }

    public Pedido concluir(@NotNull @Positive Long enderecoId,
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=128) String opcaoEntrega,
            @NotNull @Valid ConfirmacaoCheckout confirmacao) {
        if (opcaoEntrega == null || opcaoEntrega.isBlank())
            throw new br.com.fatec.ecommerce.compartilhado.dominio.DadoInvalidoException("Escolha uma opção de entrega.");
        var compra = revalidar.executar(clienteId(), enderecoId, opcaoEntrega);
        ConferirConfirmacao.conferir(confirmacao, compra.previa());
        var cupom = compra.previa().confirmacao().cupom();
        if (cupom != null && !cupom.valido()) {
            throw new br.com.fatec.ecommerce.promocao.dominio.CupomRejeitadoException(cupom.motivo()) {
                @Override public java.util.Map<String,Object> propriedades() {
                    return java.util.Map.of("motivo",cupom.motivo(),"previaAtualizada",compra.previa());
                }
            };
        }
        var pedido = criar.executar(compra.pedido());
        reservar.para(pedido.getId(), compra.componentes());
        if (cupom != null && compra.pedido().descontos().stream().anyMatch(d -> d.origem() == br.com.fatec.ecommerce.pedido.dominio.OrigemDesconto.CUPOM)) {
            usoCupom.executar(cupom.cupomId(), cupom.versao(), pedido.getId(), compra.carrinho().getClienteId());
        }
        var confirmado = confirmar.executar(pedido.getId());
        compra.carrinho().esvaziar();
        carrinhos.salvar(compra.carrinho());
        return confirmado;
    }

    private Long clienteId() {
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return clientes.buscarPorUsuarioId(Long.valueOf(jwt.getSubject()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado.")).getId();
    }
}
