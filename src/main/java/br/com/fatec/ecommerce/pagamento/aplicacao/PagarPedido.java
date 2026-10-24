package br.com.fatec.ecommerce.pagamento.aplicacao;

import br.com.fatec.ecommerce.cliente.dominio.ClienteRepository;
import br.com.fatec.ecommerce.compartilhado.dominio.*;
import br.com.fatec.ecommerce.pagamento.dominio.*;
import br.com.fatec.ecommerce.pedido.dominio.*;
import java.time.Clock;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@PreAuthorize("hasRole('CLIENTE')")
public class PagarPedido {
    public record Inicio(Cobranca cobranca, boolean criada) {}
    public record Consulta(Cobranca cobranca, Estorno estorno) {}
    private final ClienteRepository clientes;
    private final PedidoRepository pedidos;
    private final PagamentosRepository pagamentos;
    private final RegistroGateways gateways;
    private final ConfirmarPagamento confirmar;
    private final Clock clock;
    public PagarPedido(ClienteRepository clientes, PedidoRepository pedidos, PagamentosRepository pagamentos,
            RegistroGateways gateways, ConfirmarPagamento confirmar, Clock clock) {
        this.clientes=clientes; this.pedidos=pedidos; this.pagamentos=pagamentos;
        this.gateways=gateways; this.confirmar=confirmar; this.clock=clock;
    }
    public Inicio iniciar(Long pedidoId, FormaPagamento forma) {
        if (forma == null) throw new DadoInvalidoException("Informe a forma de pagamento.");
        conferirProprietario(pedidoId);
        var pedido = pedidos.bloquearPorId(pedidoId).orElseThrow();
        var existente = pagamentos.buscarPorPedido(pedidoId);
        if (existente.isPresent()) {
            if (existente.get().forma() != forma) throw new ConflitoException("O pedido já possui cobrança em outra forma de pagamento.");
            return new Inicio(existente.get(), false);
        }
        if (pedido.getSituacao() != SituacaoPedido.AGUARDANDO_PAGAMENTO)
            throw new ConflitoException("O pedido não está aguardando pagamento.");
        var gateway = gateways.para(forma);
        var resultado = gateway.cobrar(new SolicitacaoCobranca(pedidoId, forma, pedido.getTotal()));
        if (resultado == null || resultado.situacao() == null) throw new IllegalStateException("Resultado inválido do fornecedor.");
        var momento = clock.instant();
        var cobranca = new Cobranca(pedidoId, gateway.fornecedor(), forma, pedido.getTotal(), resultado.idTransacaoExterna(),
                ResultadoCobranca.Situacao.PENDENTE, momento, momento);
        pagamentos.criar(cobranca);
        if (resultado.situacao() != ResultadoCobranca.Situacao.PENDENTE)
            cobranca = confirmar.executar(cobranca.idTransacaoExterna(), resultado.situacao());
        return new Inicio(cobranca, true);
    }
    @Transactional(readOnly=true)
    public Consulta consultar(Long pedidoId) {
        conferirProprietario(pedidoId);
        return new Consulta(pagamentos.buscarPorPedido(pedidoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cobrança não encontrada.")),
                pagamentos.buscarEstorno(pedidoId).orElse(null));
    }
    private void conferirProprietario(Long pedidoId) {
        var jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long clienteId = clientes.buscarPorUsuarioId(Long.valueOf(jwt.getSubject()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado.")).getId();
        pedidos.buscarPorIdECliente(pedidoId, clienteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado."));
    }
}
