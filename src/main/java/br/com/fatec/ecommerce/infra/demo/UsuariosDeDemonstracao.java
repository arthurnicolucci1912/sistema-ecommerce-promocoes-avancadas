package br.com.fatec.ecommerce.infra.demo;

import br.com.fatec.ecommerce.catalogo.dominio.ItemVendavel;
import br.com.fatec.ecommerce.catalogo.dominio.ItemVendavelRepository;
import br.com.fatec.ecommerce.catalogo.dominio.VariacaoProduto;
import br.com.fatec.ecommerce.cliente.aplicacao.CadastrarCliente;
import br.com.fatec.ecommerce.cliente.aplicacao.DadosDoEndereco;
import br.com.fatec.ecommerce.cliente.aplicacao.GerenciarEnderecos;
import br.com.fatec.ecommerce.cliente.dominio.ClienteRepository;
import br.com.fatec.ecommerce.cliente.dominio.Endereco;
import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import br.com.fatec.ecommerce.estoque.aplicacao.ReservarEstoque;
import br.com.fatec.ecommerce.estoque.dominio.ComponenteAReservar;
import br.com.fatec.ecommerce.pedido.aplicacao.ConfirmarReservaDoPedido;
import br.com.fatec.ecommerce.pedido.aplicacao.CriarPedido;
import br.com.fatec.ecommerce.pedido.dominio.DadosNovoPedido;
import br.com.fatec.ecommerce.pedido.dominio.EnderecoEntrega;
import br.com.fatec.ecommerce.pedido.dominio.PedidoRepository;
import br.com.fatec.ecommerce.seguranca.dominio.Perfil;
import br.com.fatec.ecommerce.seguranca.dominio.Usuario;
import br.com.fatec.ecommerce.seguranca.dominio.UsuarioRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Garante, apenas no perfil {@code dev}, os dados de demonstracao (ficticios, senha {@value #SENHA}): os usuarios
 * (um por perfil administrativo e um cliente com endereco principal), o catalogo e o estoque
 * ({@link CatalogoDeDemonstracao}) e um pedido de exemplo em {@code AGUARDANDO_PAGAMENTO} (temporario, ate existir
 * o checkout) com os SKUs reais do catalogo e o estoque reservado. Idempotente: so cria o que nao existe
 * (a chave e o e-mail; o pedido, a ausencia de pedidos do cliente), entao reiniciar a aplicacao nao duplica nada.
 *
 * <p>Fica em {@code infra.demo}, fora dos modulos, porque e composicao que cruza {@code seguranca} e
 * {@code cliente} e {@code pedido}; dentro de qualquer um deles criaria dependencia circular entre os modulos.
 */
@Component
@Profile("dev")
@Order(1)
public class UsuariosDeDemonstracao implements ApplicationRunner {

    static final String SENHA = "Demo1234";
    static final String EMAIL_CLIENTE = "cliente@demo.local";

    private static final Logger LOG = LoggerFactory.getLogger(UsuariosDeDemonstracao.class);

    private static final Map<String, Perfil> ADMINISTRATIVOS = Map.of(
            "admin@demo.local", Perfil.ADMIN,
            "comercial@demo.local", Perfil.OPERADOR_COMERCIAL,
            "estoque@demo.local", Perfil.ESTOQUE,
            "promocoes@demo.local", Perfil.PROMOCOES);

    private final br.com.fatec.ecommerce.entrega.dominio.RegistroProvedoresFrete fretes;
    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final CadastrarCliente cadastrarCliente;
    private final GerenciarEnderecos enderecos;
    private final ClienteRepository clientes;
    private final PedidoRepository pedidos;
    private final CriarPedido criarPedido;
    private final ConfirmarReservaDoPedido confirmarReserva;
    private final ReservarEstoque reservarEstoque;
    private final ItemVendavelRepository itens;
    private final CatalogoDeDemonstracao catalogo;
    private final TransactionTemplate transacao;

    public UsuariosDeDemonstracao(UsuarioRepository usuarios, PasswordEncoder passwordEncoder,
            CadastrarCliente cadastrarCliente, GerenciarEnderecos enderecos, ClienteRepository clientes,
            PedidoRepository pedidos, CriarPedido criarPedido, ConfirmarReservaDoPedido confirmarReserva,
            ReservarEstoque reservarEstoque, ItemVendavelRepository itens, CatalogoDeDemonstracao catalogo,
            PlatformTransactionManager gerenteDeTransacao,
            br.com.fatec.ecommerce.entrega.dominio.RegistroProvedoresFrete fretes) {
        this.fretes=fretes;
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.cadastrarCliente = cadastrarCliente;
        this.enderecos = enderecos;
        this.clientes = clientes;
        this.pedidos = pedidos;
        this.criarPedido = criarPedido;
        this.confirmarReserva = confirmarReserva;
        this.reservarEstoque = reservarEstoque;
        this.itens = itens;
        this.catalogo = catalogo;
        this.transacao = new TransactionTemplate(gerenteDeTransacao);
    }

    @Override
    public void run(ApplicationArguments args) {
        ADMINISTRATIVOS.forEach(this::criarAdministrativoSeNecessario);
        criarClienteSeNecessario();
        catalogo.semear();
        criarPedidoDeExemploSeNecessario();
    }

    private void criarAdministrativoSeNecessario(String email, Perfil perfil) {
        if (usuarios.existePorEmail(email)) {
            return;
        }
        usuarios.salvar(new Usuario(email, passwordEncoder.encode(SENHA), List.of(perfil)));
        LOG.info("Usuário de demonstração criado: {} ({})", email, perfil);
    }

    private void criarClienteSeNecessario() {
        if (usuarios.existePorEmail(EMAIL_CLIENTE)) {
            return;
        }
        // CPF ficticio com digitos verificadores validos.
        cadastrarCliente.executar("Cliente Demonstração", EMAIL_CLIENTE, SENHA, "529.982.247-25", "(11) 91234-5678");
        Long usuarioId = usuarios.buscarPorEmail(EMAIL_CLIENTE).orElseThrow().getId();
        enderecos.cadastrar(usuarioId, new DadosDoEndereco("Casa", "01310-100", "Avenida Paulista", "1000",
                "Apto 12", "Bela Vista", "São Paulo", "SP"));
        LOG.info("Usuário de demonstração criado: {} (CLIENTE)", EMAIL_CLIENTE);
    }

    /**
     * Pedido de exemplo (itens e valores ficticios) para exercitar o ciclo de vida antes de existir o checkout.
     * Criado so se o cliente de demonstracao ainda nao tem pedidos; usa os mesmos servicos que o checkout usara,
     * na mesma transacao, e copia o endereco principal do cliente.
     */
    private void criarPedidoDeExemploSeNecessario() {
        Long usuarioId = usuarios.buscarPorEmail(EMAIL_CLIENTE).orElseThrow().getId();
        Long clienteId = clientes.buscarPorUsuarioId(usuarioId).orElseThrow().getId();
        if (pedidos.existeDoCliente(clienteId)) {
            return;
        }
        List<Endereco> ativos = enderecos.listar(usuarioId);
        Endereco endereco = ativos.stream().filter(Endereco::isPrincipal).findFirst()
                .or(() -> ativos.stream().findFirst()).orElse(null);
        if (endereco == null) {
            LOG.warn("Pedido de demonstração não criado: {} não tem endereço ativo", EMAIL_CLIENTE);
            return;
        }
        try {
            transacao.executeWithoutResult(status -> {
                List<ComponenteAReservar> componentes = new ArrayList<>();
                DadosNovoPedido dados = pedidoDeExemplo(clienteId, endereco, componentes);
                Long pedidoId = criarPedido.executar(dados).getId();
                reservarEstoque.para(pedidoId, componentes);
                confirmarReserva.executar(pedidoId);
            });
            LOG.info("Pedido de demonstração criado para {} (AGUARDANDO_PAGAMENTO)", EMAIL_CLIENTE);
        } catch (br.com.fatec.ecommerce.compartilhado.dominio.ConflitoException
                | br.com.fatec.ecommerce.compartilhado.dominio.ServicoIndisponivelException e) {
            LOG.warn("Pedido de demonstração não criado: estoque ou entrega indisponível ({})", e.getMessage());
        }
    }

    /**
     * 2 x Camiseta Basica M + 1 x Caneca, com os precos do catalogo (R$ 59,90 e R$ 34,90: subtotal R$ 154,70); frete
     * economico simulado de R$ 25,00 em SP; total R$ 179,70. Preenche {@code componentes} com o estoque a reservar.
     */
    private DadosNovoPedido pedidoDeExemplo(Long clienteId, Endereco endereco, List<ComponenteAReservar> componentes) {
        List<DadosNovoPedido.Item> lista = List.of(
                item(CatalogoDeDemonstracao.SKU_CAMISETA_M, 2, componentes),
                item(CatalogoDeDemonstracao.SKU_CANECA, 1, componentes));
        Dinheiro subtotal = lista.stream().map(i -> i.precoUnitario().multiplicar(i.quantidade()))
                .reduce(Dinheiro.zero(), Dinheiro::somar);
        var opcao = fretes.selecionar(new br.com.fatec.ecommerce.entrega.dominio.ContextoFrete(
                endereco.getUf(), java.math.BigDecimal.ZERO, subtotal,
                lista.stream().mapToInt(DadosNovoPedido.Item::quantidade).sum()), "economica-simulada:ECONOMICA");
        Dinheiro frete = Dinheiro.de(opcao.custoBruto());
        EnderecoEntrega entrega = new EnderecoEntrega(endereco.getApelido(), endereco.getCep(),
                endereco.getLogradouro(), endereco.getNumero(), endereco.getComplemento(), endereco.getBairro(),
                endereco.getCidade(), endereco.getUf());
        return new DadosNovoPedido(clienteId, entrega, lista, List.of(), frete, subtotal, Dinheiro.zero(),
                subtotal.somar(frete), new br.com.fatec.ecommerce.pedido.dominio.DadosEntregaSelecionada(
                        opcao.codigo(),opcao.fornecedor(),opcao.modalidade(),opcao.prazoDias()));
    }

    /** Item do snapshot a partir do catalogo (nome, variacao e preco do momento) e o estoque que ele consome. */
    private DadosNovoPedido.Item item(String sku, int quantidade, List<ComponenteAReservar> componentes) {
        ItemVendavel vendavel = itens.buscarPorSku(sku).orElseThrow();
        vendavel.componentesDeEstoque(quantidade)
                .forEach(c -> componentes.add(new ComponenteAReservar(c.variacaoId(), c.quantidade())));
        VariacaoProduto variacao = (VariacaoProduto) vendavel;
        // Ordem por nome do atributo: o mapa carregado do banco nao garante a ordem de insercao.
        String descricao = new TreeMap<>(variacao.getAtributos()).entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue()).collect(Collectors.joining(", "));
        return new DadosNovoPedido.Item(sku, variacao.getProduto().getNome(), descricao, quantidade,
                variacao.preco());
    }
}
