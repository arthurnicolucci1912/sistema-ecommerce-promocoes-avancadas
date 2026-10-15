package br.com.fatec.ecommerce.pedido.aplicacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fatec.ecommerce.TesteDeIntegracao;
import br.com.fatec.ecommerce.cliente.ClientesParaTeste;
import br.com.fatec.ecommerce.cliente.ClientesParaTeste.ClienteDeTeste;
import br.com.fatec.ecommerce.cliente.aplicacao.DadosDoEndereco;
import br.com.fatec.ecommerce.cliente.aplicacao.GerenciarEnderecos;
import br.com.fatec.ecommerce.cliente.dominio.Endereco;
import br.com.fatec.ecommerce.compartilhado.dominio.DadoInvalidoException;
import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import br.com.fatec.ecommerce.compartilhado.dominio.RecursoNaoEncontradoException;
import br.com.fatec.ecommerce.pedido.CapturaDeEventosDoPedido;
import br.com.fatec.ecommerce.pedido.PedidosParaTeste;
import br.com.fatec.ecommerce.pedido.dominio.DadosNovoPedido;
import br.com.fatec.ecommerce.pedido.dominio.EnderecoEntrega;
import br.com.fatec.ecommerce.pedido.dominio.Pedido;
import br.com.fatec.ecommerce.pedido.dominio.PedidoRepository;
import br.com.fatec.ecommerce.pedido.dominio.SituacaoPedido;
import br.com.fatec.ecommerce.pedido.dominio.TipoResponsavel;
import br.com.fatec.ecommerce.pedido.dominio.evento.PedidoCriado;
import br.com.fatec.ecommerce.seguranca.UsuariosParaTeste;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/** Criacao do pedido a partir do snapshot e imutabilidade do snapshot diante de mudancas posteriores. */
@TesteDeIntegracao
@Import(CapturaDeEventosDoPedido.class)
class CriarPedidoTests {

    @Autowired
    CriarPedido criarPedido;
    @Autowired
    ConfirmarReservaDoPedido confirmarReserva;
    @Autowired
    PedidoRepository pedidos;
    @Autowired
    GerenciarEnderecos enderecos;
    @Autowired
    UsuariosParaTeste helper;
    @Autowired
    ClientesParaTeste clientesHelper;
    @Autowired
    CapturaDeEventosDoPedido captura;
    @Autowired
    JdbcTemplate jdbc;

    ClienteDeTeste cliente;

    @BeforeEach
    void preparar() {
        helper.limpar();
        captura.limpar();
        cliente = clientesHelper.criar("cliente@demo.local", 30);
    }

    private Pedido recarregar(Long id) {
        return pedidos.buscarPorId(id).orElseThrow();
    }

    @Test
    void pedidoCriadoEmAguardandoPagamentoPreservaOSnapshotEPublicaPedidoCriado() {
        DadosNovoPedido dados = PedidosParaTeste.validos(cliente.cliente().getId());

        Pedido criado = criarPedido.executar(dados);

        assertThat(criado.getId()).isNotNull();
        assertThat(criado.getSituacao()).isEqualTo(SituacaoPedido.CRIADO);
        assertThat(captura.recebidos()).isEmpty();

        confirmarReserva.executar(criado.getId());

        Pedido pedido = recarregar(criado.getId());
        assertThat(pedido.getSituacao()).isEqualTo(SituacaoPedido.AGUARDANDO_PAGAMENTO);
        assertThat(pedido.getClienteId()).isEqualTo(cliente.cliente().getId());
        assertThat(pedido.getItens()).hasSize(2);
        assertThat(pedido.getItens()).extracting(i -> i.getSku()).containsExactly("CAM-001-M", "TEN-042");
        assertThat(pedido.getItens()).extracting(i -> i.getNomeProduto()).containsExactly("Camiseta", "Tênis");
        assertThat(pedido.getItens()).extracting(i -> i.getDescricaoVariacao()).containsExactly("Azul, M", "Preto, 42");
        assertThat(pedido.getItens()).extracting(i -> i.getQuantidade()).containsExactly(2, 1);
        assertThat(pedido.getItens()).extracting(i -> i.getPrecoUnitario())
                .containsExactly(Dinheiro.de("100.00"), Dinheiro.de("220.00"));
        assertThat(pedido.getItens()).extracting(i -> i.getTotalItem())
                .containsExactly(Dinheiro.de("200.00"), Dinheiro.de("220.00"));
        assertThat(pedido.getDescontos()).singleElement().satisfies(d -> {
            assertThat(d.getDescricao()).isEqualTo("Promo de inverno");
            assertThat(d.getValor()).isEqualTo(Dinheiro.de("20.00"));
        });
        assertThat(pedido.getSubtotal()).isEqualTo(dados.subtotal());
        assertThat(pedido.getTotalDescontos()).isEqualTo(dados.totalDescontos());
        assertThat(pedido.getFrete()).isEqualTo(dados.frete());
        assertThat(pedido.getTotal()).isEqualTo(dados.total());
        assertThat(pedido.getEntrega()).usingRecursiveComparison().isEqualTo(dados.endereco());
        assertThat(pedido.getHistorico()).extracting(h -> h.getPara())
                .containsExactly(SituacaoPedido.CRIADO, SituacaoPedido.AGUARDANDO_PAGAMENTO);
        assertThat(pedido.getHistorico().get(1).getResponsavel().tipo()).isEqualTo(TipoResponsavel.SISTEMA);

        assertThat(captura.recebidos()).singleElement().isInstanceOfSatisfying(PedidoCriado.class, e -> {
            assertThat(e.pedidoId()).isEqualTo(pedido.getId());
            assertThat(e.clienteId()).isEqualTo(cliente.cliente().getId());
            assertThat(e.de()).isEqualTo(SituacaoPedido.CRIADO);
            assertThat(e.para()).isEqualTo(SituacaoPedido.AGUARDANDO_PAGAMENTO);
            assertThat(e.ocorridoEm()).isNotNull();
        });
    }

    @Test
    void precoAlteradoDepoisDaCompraNaoMudaOPedidoAntigo() {
        Long clienteId = cliente.cliente().getId();
        Pedido antigo = criarPedido.executar(PedidosParaTeste.validos(clienteId));

        Pedido novo = criarPedido.executar(PedidosParaTeste.validos(clienteId, PedidosParaTeste.enderecoPadrao(),
                PedidosParaTeste.itemTenis("250.00")));

        assertThat(recarregar(novo.getId()).getItens().get(1).getPrecoUnitario()).isEqualTo(Dinheiro.de("250.00"));
        Pedido antigoRecarregado = recarregar(antigo.getId());
        assertThat(antigoRecarregado.getItens().get(1).getPrecoUnitario()).isEqualTo(Dinheiro.de("220.00"));
        assertThat(antigoRecarregado.getTotal()).isEqualTo(Dinheiro.de("415.00"));
    }

    @Test
    void enderecoAlteradoOuRemovidoDepoisDaCompraNaoMudaOPedido() {
        Long usuarioId = cliente.usuario().getId();
        Endereco endereco = enderecos.cadastrar(usuarioId, new DadosDoEndereco("Casa", "13010-000",
                "Rua das Flores", "10", "Casa 2", "Centro", "Campinas", "SP"));
        EnderecoEntrega copia = new EnderecoEntrega(endereco.getApelido(), endereco.getCep(),
                endereco.getLogradouro(), endereco.getNumero(), endereco.getComplemento(), endereco.getBairro(),
                endereco.getCidade(), endereco.getUf());
        Pedido pedido = criarPedido.executar(PedidosParaTeste.validos(cliente.cliente().getId(), copia,
                PedidosParaTeste.itemTenis("220.00")));

        enderecos.alterar(usuarioId, endereco.getId(), new DadosDoEndereco("Trabalho", "01001-000",
                "Praça da Sé", "99", null, "Sé", "São Paulo", "SP"));
        enderecos.remover(usuarioId, endereco.getId());

        EnderecoEntrega entrega = recarregar(pedido.getId()).getEntrega();
        assertThat(entrega.getApelido()).isEqualTo("Casa");
        assertThat(entrega.getCep()).isEqualTo("13010000");
        assertThat(entrega.getLogradouro()).isEqualTo("Rua das Flores");
        assertThat(entrega.getNumero()).isEqualTo("10");
        assertThat(entrega.getComplemento()).isEqualTo("Casa 2");
        assertThat(entrega.getBairro()).isEqualTo("Centro");
        assertThat(entrega.getCidade()).isEqualTo("Campinas");
        assertThat(entrega.getUf()).isEqualTo("SP");
        assertThat(jdbc.queryForObject("SELECT ativo FROM endereco WHERE id = ?", Boolean.class, endereco.getId()))
                .isFalse();
    }

    @Test
    void snapshotInconsistenteNaoCriaPedido() {
        DadosNovoPedido invalido = PedidosParaTeste.comTotal(PedidosParaTeste.validos(cliente.cliente().getId()),
                Dinheiro.de("1.00"));

        assertThatThrownBy(() -> criarPedido.executar(invalido)).isInstanceOf(DadoInvalidoException.class);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM pedido", Integer.class)).isZero();
    }

    @Test
    void clienteInexistenteNaoCriaPedido() {
        assertThatThrownBy(() -> criarPedido.executar(PedidosParaTeste.validos(-1L)))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM pedido", Integer.class)).isZero();
    }

    @Test
    void confirmarReservaDePedidoInexistenteLancaNaoEncontrado() {
        assertThatThrownBy(() -> confirmarReserva.executar(-1L)).isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
