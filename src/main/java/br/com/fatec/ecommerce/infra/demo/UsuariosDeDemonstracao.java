package br.com.fatec.ecommerce.infra.demo;

import br.com.fatec.ecommerce.cliente.aplicacao.CadastrarCliente;
import br.com.fatec.ecommerce.cliente.aplicacao.DadosDoEndereco;
import br.com.fatec.ecommerce.cliente.aplicacao.GerenciarEnderecos;
import br.com.fatec.ecommerce.cliente.dominio.ClienteRepository;
import br.com.fatec.ecommerce.cliente.dominio.Endereco;
import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import br.com.fatec.ecommerce.pedido.aplicacao.ConfirmarReservaDoPedido;
import br.com.fatec.ecommerce.pedido.aplicacao.CriarPedido;
import br.com.fatec.ecommerce.pedido.dominio.DadosNovoPedido;
import br.com.fatec.ecommerce.pedido.dominio.EnderecoEntrega;
import br.com.fatec.ecommerce.pedido.dominio.PedidoRepository;
import br.com.fatec.ecommerce.seguranca.dominio.Perfil;
import br.com.fatec.ecommerce.seguranca.dominio.Usuario;
import br.com.fatec.ecommerce.seguranca.dominio.UsuarioRepository;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Garante, apenas no perfil {@code dev}, os usuarios de demonstracao (dados ficticios, senha {@value #SENHA}):
 * um por perfil administrativo e um cliente com endereco principal e um pedido de exemplo em
 * {@code AGUARDANDO_PAGAMENTO} (temporario, ate existir o checkout). Idempotente: so cria o que nao existe
 * (a chave e o e-mail; o pedido, a ausencia de pedidos do cliente), entao reiniciar a aplicacao nao duplica nada.
 *
 * <p>Fica em {@code infra.demo}, fora dos modulos, porque e composicao que cruza {@code seguranca} e
 * {@code cliente} e {@code pedido}; dentro de qualquer um deles criaria dependencia circular entre os modulos.
 */
@Component
@Profile("dev")
public class UsuariosDeDemonstracao implements ApplicationRunner {

    static final String SENHA = "Demo1234";
    static final String EMAIL_CLIENTE = "cliente@demo.local";

    private static final Logger LOG = LoggerFactory.getLogger(UsuariosDeDemonstracao.class);

    private static final Map<String, Perfil> ADMINISTRATIVOS = Map.of(
            "admin@demo.local", Perfil.ADMIN,
            "comercial@demo.local", Perfil.OPERADOR_COMERCIAL,
            "estoque@demo.local", Perfil.ESTOQUE,
            "promocoes@demo.local", Perfil.PROMOCOES);

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final CadastrarCliente cadastrarCliente;
    private final GerenciarEnderecos enderecos;
    private final ClienteRepository clientes;
    private final PedidoRepository pedidos;
    private final CriarPedido criarPedido;
    private final ConfirmarReservaDoPedido confirmarReserva;
    private final TransactionTemplate transacao;

    public UsuariosDeDemonstracao(UsuarioRepository usuarios, PasswordEncoder passwordEncoder,
            CadastrarCliente cadastrarCliente, GerenciarEnderecos enderecos, ClienteRepository clientes,
            PedidoRepository pedidos, CriarPedido criarPedido, ConfirmarReservaDoPedido confirmarReserva,
            PlatformTransactionManager gerenteDeTransacao) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.cadastrarCliente = cadastrarCliente;
        this.enderecos = enderecos;
        this.clientes = clientes;
        this.pedidos = pedidos;
        this.criarPedido = criarPedido;
        this.confirmarReserva = confirmarReserva;
        this.transacao = new TransactionTemplate(gerenteDeTransacao);
    }

    @Override
    public void run(ApplicationArguments args) {
        ADMINISTRATIVOS.forEach(this::criarAdministrativoSeNecessario);
        criarClienteSeNecessario();
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
        DadosNovoPedido dados = pedidoDeExemplo(clienteId, endereco);
        transacao.executeWithoutResult(status -> {
            Long pedidoId = criarPedido.executar(dados).getId();
            confirmarReserva.executar(pedidoId);
        });
        LOG.info("Pedido de demonstração criado para {} (AGUARDANDO_PAGAMENTO)", EMAIL_CLIENTE);
    }

    /** 2 x Camiseta Basica (R$ 59,90) + 1 x Caneca (R$ 34,90) = R$ 154,70; frete R$ 18,50; total R$ 173,20. */
    private static DadosNovoPedido pedidoDeExemplo(Long clienteId, Endereco endereco) {
        List<DadosNovoPedido.Item> itens = List.of(
                new DadosNovoPedido.Item("CAM-BAS-M", "Camiseta Básica", "Tamanho M", 2, Dinheiro.de("59.90")),
                new DadosNovoPedido.Item("CAN-CER-300", "Caneca", "Cerâmica 300 ml", 1, Dinheiro.de("34.90")));
        Dinheiro subtotal = itens.stream().map(i -> i.precoUnitario().multiplicar(i.quantidade()))
                .reduce(Dinheiro.zero(), Dinheiro::somar);
        Dinheiro frete = Dinheiro.de("18.50");
        EnderecoEntrega entrega = new EnderecoEntrega(endereco.getApelido(), endereco.getCep(),
                endereco.getLogradouro(), endereco.getNumero(), endereco.getComplemento(), endereco.getBairro(),
                endereco.getCidade(), endereco.getUf());
        return new DadosNovoPedido(clienteId, entrega, itens, List.of(), frete, subtotal, Dinheiro.zero(),
                subtotal.somar(frete));
    }
}
