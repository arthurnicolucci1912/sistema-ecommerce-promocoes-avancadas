package br.com.fatec.ecommerce.pedido.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.DadoInvalidoException;
import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import br.com.fatec.ecommerce.compartilhado.dominio.EventoDominio;
import br.com.fatec.ecommerce.pedido.dominio.estado.DadosDaTransicao;
import br.com.fatec.ecommerce.pedido.dominio.estado.EstadoPedido;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

/**
 * Agregado do pedido: snapshot comercial imutavel (itens, descontos, frete, totais e endereco de entrega) mais
 * a situacao no ciclo de vida, o historico das transicoes e os eventos ainda nao publicados.
 *
 * <p>Pattern: State - papel: contexto. Cada operacao do ciclo de vida delega ao {@link EstadoPedido} atual, que
 * decide se a transicao e valida e qual e o proximo estado; o pedido nao tem condicional por situacao. Uma
 * transicao rejeitada lanca excecao antes de qualquer alteracao.
 *
 * <p>Pattern: Observer - papel: acumula os eventos de dominio de cada transicao ({@link #consumirEventos()});
 * quem publica e o servico de aplicacao, depois de salvar.
 *
 * <p>Ver docs/04-design-patterns.md, secoes 3.4 (State) e 3.6 (Observer).
 *
 * <p>Concorrencia: {@code @Version} (lock otimista); a segunda de duas alteracoes simultaneas falha ao gravar.
 */
@Entity
@Table(name = "pedido")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cliente_id", nullable = false, updatable = false)
    private Long clienteId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SituacaoPedido situacao;

    @Embedded
    private EnderecoEntrega entrega;

    @Embedded
    private DadosEntregaSelecionada opcaoEntrega;

    @Column(nullable = false, updatable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "total_descontos", nullable = false, updatable = false, precision = 12, scale = 2)
    private BigDecimal totalDescontos;

    @Column(nullable = false, updatable = false, precision = 12, scale = 2)
    private BigDecimal frete;

    @Column(nullable = false, updatable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Version
    @Column(name = "versao", nullable = false)
    private Long versao;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "pedido_id", nullable = false, updatable = false)
    @Fetch(FetchMode.SUBSELECT)
    @OrderBy("id ASC")
    private List<ItemPedido> itens = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "pedido_id", nullable = false, updatable = false)
    @Fetch(FetchMode.SUBSELECT)
    @OrderBy("id ASC")
    private List<DescontoPedido> descontos = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "pedido_id", nullable = false, updatable = false)
    @Fetch(FetchMode.SUBSELECT)
    @OrderBy("id ASC")
    private List<EntradaHistorico> historico = new ArrayList<>();

    /** Objeto de estado correspondente a {@link #situacao}; carregado sob demanda, nunca persistido. */
    @Transient
    private EstadoPedido estado;

    /** Eventos acumulados pelas transicoes, montados so na publicacao (quando o id ja existe). */
    @Transient
    private List<Supplier<Optional<EventoDominio>>> eventosPendentes = new ArrayList<>();

    protected Pedido() {
    }

    private Pedido(DadosNovoPedido dados, Instant criadoEm, EstadoPedido estadoInicial) {
        this.clienteId = dados.clienteId();
        this.entrega = dados.endereco();
        this.opcaoEntrega = dados.opcaoEntrega();
        this.subtotal = dados.subtotal().valor();
        this.totalDescontos = dados.totalDescontos().valor();
        this.frete = dados.frete().valor();
        this.total = dados.total().valor();
        this.criadoEm = criadoEm;
        dados.itens().forEach(item -> itens.add(new ItemPedido(item)));
        dados.descontos().forEach(desconto -> descontos.add(new DescontoPedido(desconto)));
        this.estado = estadoInicial;
        this.situacao = estadoInicial.situacao();
        historico.add(new EntradaHistorico(null, situacao, criadoEm, Responsavel.sistema(), null));
    }

    /**
     * Cria o pedido em {@code CRIADO} a partir do snapshot, conferindo-o: itens validos, subtotal igual a soma
     * dos itens, total de descontos igual a soma dos descontos e total igual a subtotal menos descontos mais frete.
     * Snapshot inconsistente lanca {@link DadoInvalidoException} e nenhum pedido e criado. Esta fabrica e validacao
     * do agregado, nao um pattern.
     */
    public static Pedido criar(DadosNovoPedido dados, Instant criadoEm) {
        return criar(dados, criadoEm, SituacaoPedido.CRIADO.estado());
    }

    /** Ponto de entrada para testes: permite iniciar o ciclo de vida com um estado que nao e do enum. */
    static Pedido criar(DadosNovoPedido dados, Instant criadoEm, EstadoPedido estadoInicial) {
        validar(dados);
        return new Pedido(dados, criadoEm, estadoInicial);
    }

    private static void validar(DadosNovoPedido dados) {
        if (dados.itens().isEmpty()) {
            throw new DadoInvalidoException("O pedido precisa ter ao menos um item.");
        }
        Dinheiro somaItens = Dinheiro.zero();
        for (DadosNovoPedido.Item item : dados.itens()) {
            if (item.sku() == null || item.sku().isBlank() || item.nomeProduto() == null
                    || item.nomeProduto().isBlank() || item.precoUnitario() == null) {
                throw new DadoInvalidoException("Item do pedido sem identificação, nome ou preço.");
            }
            if (item.quantidade() <= 0) {
                throw new DadoInvalidoException("A quantidade de cada item deve ser maior que zero.");
            }
            if (item.precoUnitario().ehNegativo()) {
                throw new DadoInvalidoException("O preço unitário não pode ser negativo.");
            }
            somaItens = somaItens.somar(item.precoUnitario().multiplicar(item.quantidade()));
        }
        Dinheiro somaDescontos = Dinheiro.zero();
        for (DadosNovoPedido.Desconto desconto : dados.descontos()) {
            if (desconto.descricao() == null || desconto.descricao().isBlank() || desconto.origem() == null
                    || desconto.alvo() == null || desconto.valor() == null || desconto.valor().ehNegativo()) {
                throw new DadoInvalidoException("Desconto do pedido inválido.");
            }
            somaDescontos = somaDescontos.somar(desconto.valor());
        }
        if (dados.frete().ehNegativo()) {
            throw new DadoInvalidoException("O frete não pode ser negativo.");
        }
        if (!dados.subtotal().equals(somaItens)) {
            throw new DadoInvalidoException("Totais inconsistentes: o subtotal difere da soma dos itens.");
        }
        if (!dados.totalDescontos().equals(somaDescontos)) {
            throw new DadoInvalidoException("Totais inconsistentes: o total de descontos difere da soma dos descontos.");
        }
        Dinheiro esperado = dados.subtotal().subtrair(dados.totalDescontos()).somar(dados.frete());
        if (esperado.ehNegativo()) {
            throw new DadoInvalidoException("Os descontos não podem superar o subtotal mais o frete (total negativo).");
        }
        if (!dados.total().equals(esperado)) {
            throw new DadoInvalidoException(
                    "Totais inconsistentes: o total difere de subtotal menos descontos mais frete.");
        }
    }

    // --- Ciclo de vida: cada operacao delega ao estado atual ---

    public void aguardarPagamento(Responsavel responsavel, Instant momento) {
        transicionar(estado().aguardarPagamento(), responsavel, null, momento);
    }

    public void confirmarPagamento(Responsavel responsavel, String motivo, Instant momento) {
        transicionar(estado().confirmarPagamento(), responsavel, motivo, momento);
    }

    public void recusarPagamento(Responsavel responsavel, String motivo, Instant momento) {
        transicionar(estado().recusarPagamento(), responsavel, motivo, momento);
    }

    public void iniciarPreparacao(Responsavel responsavel, Instant momento) {
        transicionar(estado().iniciarPreparacao(), responsavel, null, momento);
    }

    public void enviar(Responsavel responsavel, Instant momento) {
        transicionar(estado().enviar(), responsavel, null, momento);
    }

    public void entregar(Responsavel responsavel, Instant momento) {
        transicionar(estado().entregar(), responsavel, null, momento);
    }

    /** O motivo e obrigatorio; sem ele lanca {@link DadoInvalidoException} antes de consultar o estado. */
    public void cancelar(Responsavel responsavel, String motivo, Instant momento) {
        if (motivo == null || motivo.isBlank()) {
            throw new DadoInvalidoException("O motivo do cancelamento é obrigatório.");
        }
        transicionar(estado().cancelar(responsavel), responsavel, motivo, momento);
    }

    /**
     * Aplica a transicao ja aprovada pelo estado: muda a situacao, registra o historico e acumula o evento que o
     * estado de destino anuncia. Se o estado rejeitou a transicao, a excecao ja interrompeu o fluxo.
     */
    private void transicionar(EstadoPedido proximo, Responsavel responsavel, String motivo, Instant momento) {
        if (responsavel == null) {
            // validado antes de mutar situacao/historico/eventos: transicao rejeitada nao altera o agregado
            throw new DadoInvalidoException("O responsável pela transição é obrigatório.");
        }
        SituacaoPedido de = this.situacao;
        this.estado = proximo;
        this.situacao = proximo.situacao();
        SituacaoPedido para = this.situacao;
        historico.add(new EntradaHistorico(de, para, momento, responsavel, motivo));
        eventosPendentes.add(() -> proximo.eventoDeChegada(
                new DadosDaTransicao(id, clienteId, de, para, momento, responsavel, motivo)));
    }

    @jakarta.persistence.PostLoad
    private void invalidarEstadoCarregado() {
        estado = null;
    }

    private EstadoPedido estado() {
        if (estado == null) {
            estado = situacao.estado();
        }
        return estado;
    }

    /** Entrega os eventos acumulados (e os esquece); o servico de aplicacao os publica depois de salvar. */
    public List<EventoDominio> consumirEventos() {
        List<EventoDominio> eventos = eventosPendentes.stream()
                .map(Supplier::get)
                .flatMap(Optional::stream)
                .toList();
        eventosPendentes.clear();
        return eventos;
    }

    public Long getId() {
        return id;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public SituacaoPedido getSituacao() {
        return situacao;
    }

    public EnderecoEntrega getEntrega() {
        return entrega;
    }

    public DadosEntregaSelecionada getOpcaoEntrega() { return opcaoEntrega; }

    /** Parcela liquida derivada para nao gravar um segundo valor redundante de frete. */
    public Dinheiro getFreteLiquido() {
        Dinheiro abatimentos = descontos.stream().filter(d -> d.getAlvo() == AlvoDoDesconto.FRETE)
                .map(DescontoPedido::getValor).reduce(Dinheiro.zero(), Dinheiro::somar);
        Dinheiro liquido = getFrete().subtrair(abatimentos);
        return liquido.ehNegativo() ? Dinheiro.zero() : liquido;
    }

    public Dinheiro getSubtotal() {
        return Dinheiro.de(subtotal);
    }

    public Dinheiro getTotalDescontos() {
        return Dinheiro.de(totalDescontos);
    }

    public Dinheiro getFrete() {
        return Dinheiro.de(frete);
    }

    public Dinheiro getTotal() {
        return Dinheiro.de(total);
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Long getVersao() {
        return versao;
    }

    public List<ItemPedido> getItens() {
        return Collections.unmodifiableList(itens);
    }

    public List<DescontoPedido> getDescontos() {
        return Collections.unmodifiableList(descontos);
    }

    /** Historico em ordem cronologica (a ordem de gravacao). */
    public List<EntradaHistorico> getHistorico() {
        return Collections.unmodifiableList(historico);
    }
}
