package br.com.fatec.ecommerce.estoque.aplicacao;

import static br.com.fatec.ecommerce.catalogo.CatalogoParaTeste.MOMENTO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fatec.ecommerce.TesteDeIntegracao;
import br.com.fatec.ecommerce.catalogo.aplicacao.CadastrarVariacao;
import br.com.fatec.ecommerce.catalogo.dominio.ArvoreDeCategorias;
import br.com.fatec.ecommerce.catalogo.dominio.Categoria;
import br.com.fatec.ecommerce.catalogo.dominio.CategoriaRepository;
import br.com.fatec.ecommerce.catalogo.dominio.Produto;
import br.com.fatec.ecommerce.catalogo.dominio.ProdutoRepository;
import br.com.fatec.ecommerce.catalogo.dominio.VariacaoProduto;
import br.com.fatec.ecommerce.catalogo.dominio.evento.VariacaoCadastrada;
import br.com.fatec.ecommerce.cliente.ClientesParaTeste;
import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import br.com.fatec.ecommerce.estoque.EstoqueParaTeste;
import br.com.fatec.ecommerce.estoque.dominio.ComponenteAReservar;
import br.com.fatec.ecommerce.pedido.PedidosParaTeste;
import br.com.fatec.ecommerce.pedido.aplicacao.ConfirmarReservaDoPedido;
import br.com.fatec.ecommerce.pedido.aplicacao.CriarPedido;
import br.com.fatec.ecommerce.pedido.aplicacao.TransicionarPedido;
import br.com.fatec.ecommerce.pedido.dominio.Responsavel;
import br.com.fatec.ecommerce.pedido.dominio.TransicaoInvalidaException;
import br.com.fatec.ecommerce.seguranca.UsuariosParaTeste;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Observer do estoque: reage ao catalogo (saldo zero da variacao nova) e ao pedido (baixa, liberacao e reposicao),
 * usando os servicos reais do pedido, sem que o modulo de pedidos tenha qualquer alteracao.
 */
@TesteDeIntegracao
class OuvintesDoEstoqueTests {

    private static final long VARIACAO = 701L;
    private static final Responsavel ADMIN = Responsavel.administrativo(1L);

    @Autowired
    CadastrarVariacao cadastrarVariacao;
    @Autowired
    CategoriaRepository categorias;
    @Autowired
    ProdutoRepository produtos;
    @Autowired
    OuvinteDeCatalogoDoEstoque ouvinteDeCatalogo;
    @Autowired
    CriarPedido criarPedido;
    @Autowired
    ConfirmarReservaDoPedido confirmarReserva;
    @Autowired
    TransicionarPedido transicionar;
    @Autowired
    ReservarEstoque reservar;
    @Autowired
    EstoqueParaTeste estoque;
    @Autowired
    UsuariosParaTeste helper;
    @Autowired
    ClientesParaTeste clientesHelper;

    private Long clienteId;

    @BeforeEach
    void preparar() {
        helper.limpar();
        clienteId = clientesHelper.criar("cliente@demo.local", 41).cliente().getId();
        estoque.definirSaldo(VARIACAO, 5, 0);
    }

    /** Pedido real em AGUARDANDO_PAGAMENTO com 2 unidades da variacao reservadas. */
    private Long pedidoAguardandoPagamentoComReserva() {
        Long id = criarPedido.executar(PedidosParaTeste.validos(clienteId)).getId();
        reservar.para(id, List.of(new ComponenteAReservar(VARIACAO, 2)));
        confirmarReserva.executar(id);
        return id;
    }

    @Test
    void variacaoNovaGanhaSaldoZero() {
        Categoria camisetas = categorias.salvar(Categoria.criar("Camisetas", null, new ArvoreDeCategorias(List.of())));
        Long produtoId = produtos.salvar(Produto.criar("Camiseta Básica", null, null, camisetas, MOMENTO)).getId();

        VariacaoProduto variacao = cadastrarVariacao.cadastrar(produtoId, "CAM-M", Dinheiro.de("59.90"),
                Map.of("tamanho", "M"));

        assertThat(estoque.existeSaldo(variacao.getId())).isTrue();
        assertThat(estoque.disponivel(variacao.getId())).isZero();
        assertThat(estoque.reservado(variacao.getId())).isZero();
    }

    @Test
    void ouvinteDeCatalogoNaoAlteraSaldoJaExistente() {
        ouvinteDeCatalogo.ao(new VariacaoCadastrada(VARIACAO, "X", MOMENTO));
        ouvinteDeCatalogo.ao(new VariacaoCadastrada(VARIACAO, "X", MOMENTO));

        assertThat(estoque.disponivel(VARIACAO)).isEqualTo(5);
    }

    @Test
    void cancelamentoAntesDoPagamentoLiberaAReserva() {
        Long id = pedidoAguardandoPagamentoComReserva();
        assertThat(estoque.disponivel(VARIACAO)).isEqualTo(3);

        transicionar.cancelar(id, Responsavel.cliente(2L), "desisti");

        assertThat(estoque.disponivel(VARIACAO)).isEqualTo(5);
        assertThat(estoque.reservado(VARIACAO)).isZero();
        assertThat(estoque.situacoesDaReserva(id)).containsExactly("LIBERADA");
    }

    @Test
    void pagamentoAprovadoBaixaAReserva() {
        Long id = pedidoAguardandoPagamentoComReserva();

        transicionar.aprovarPagamento(id, Responsavel.sistema(), null);

        assertThat(estoque.reservado(VARIACAO)).isZero();
        assertThat(estoque.disponivel(VARIACAO)).isEqualTo(3);
        assertThat(estoque.situacoesDaReserva(id)).containsExactly("BAIXADA");
    }

    @Test
    void cancelamentoDepoisDoPagamentoPeloAdministradorRepoeAsUnidades() {
        Long id = pedidoAguardandoPagamentoComReserva();
        transicionar.aprovarPagamento(id, Responsavel.sistema(), null);

        transicionar.cancelar(id, ADMIN, "fraude");

        assertThat(estoque.disponivel(VARIACAO)).isEqualTo(5);
        assertThat(estoque.reservado(VARIACAO)).isZero();
        assertThat(estoque.situacoesDaReserva(id)).containsExactly("REPOSTA");
    }

    @Test
    void cancelamentoEmPreparacaoRepoeAsUnidades() {
        Long id = pedidoAguardandoPagamentoComReserva();
        transicionar.aprovarPagamento(id, Responsavel.sistema(), null);
        transicionar.iniciarPreparacao(id, ADMIN);

        transicionar.cancelar(id, ADMIN, "sem como enviar");

        assertThat(estoque.disponivel(VARIACAO)).isEqualTo(5);
        assertThat(estoque.situacoesDaReserva(id)).containsExactly("REPOSTA");
    }

    @Test
    void pagamentoRecusadoLiberaAReserva() {
        Long id = pedidoAguardandoPagamentoComReserva();

        transicionar.recusarPagamento(id, Responsavel.sistema(), "cartão recusado");

        assertThat(estoque.disponivel(VARIACAO)).isEqualTo(5);
        assertThat(estoque.reservado(VARIACAO)).isZero();
        assertThat(estoque.situacoesDaReserva(id)).containsExactly("LIBERADA");
    }

    @Test
    void transicaoRejeitadaNaoAlteraOEstoque() {
        Long id = pedidoAguardandoPagamentoComReserva();
        transicionar.aprovarPagamento(id, Responsavel.sistema(), null);
        transicionar.iniciarPreparacao(id, ADMIN);
        transicionar.enviar(id, ADMIN);
        transicionar.entregar(id, ADMIN);

        assertThatThrownBy(() -> transicionar.cancelar(id, ADMIN, "tarde demais"))
                .isInstanceOf(TransicaoInvalidaException.class);

        assertThat(estoque.disponivel(VARIACAO)).isEqualTo(3);
        assertThat(estoque.reservado(VARIACAO)).isZero();
        assertThat(estoque.situacoesDaReserva(id)).containsExactly("BAIXADA");
    }

    @Test
    void pedidoSemReservaNaoQuebraATransicaoDoPedido() {
        Long id = criarPedido.executar(PedidosParaTeste.validos(clienteId)).getId();
        confirmarReserva.executar(id);

        transicionar.aprovarPagamento(id, Responsavel.sistema(), null);
        transicionar.cancelar(id, ADMIN, "teste");

        assertThat(estoque.disponivel(VARIACAO)).isEqualTo(5);
        assertThat(estoque.situacoesDaReserva(id)).isEmpty();
    }
}
