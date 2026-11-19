package br.com.fatec.ecommerce.checkout.aplicacao;

import br.com.fatec.ecommerce.carrinho.aplicacao.ConsultarDisponibilidadeDoCarrinho.Insuficiencia;
import br.com.fatec.ecommerce.carrinho.dominio.CarrinhoRepository;
import br.com.fatec.ecommerce.catalogo.aplicacao.ConsultarItensVendaveis;
import br.com.fatec.ecommerce.catalogo.aplicacao.ItemVendavelInfo;
import br.com.fatec.ecommerce.cliente.dominio.EnderecoRepository;
import br.com.fatec.ecommerce.compartilhado.dominio.ConflitoException;
import br.com.fatec.ecommerce.compartilhado.dominio.DadoInvalidoException;
import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import br.com.fatec.ecommerce.compartilhado.dominio.RecursoNaoEncontradoException;
import br.com.fatec.ecommerce.estoque.aplicacao.ConsultarSaldo;
import br.com.fatec.ecommerce.estoque.dominio.ComponenteAReservar;
import br.com.fatec.ecommerce.pedido.dominio.DadosNovoPedido;
import br.com.fatec.ecommerce.pedido.dominio.EnderecoEntrega;
import br.com.fatec.ecommerce.preco.aplicacao.PipelinePreco;
import br.com.fatec.ecommerce.promocao.dominio.ContextoPreco;
import br.com.fatec.ecommerce.promocao.dominio.ItemPreco;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;

/**
 * Subsistema da Facade: revalida a compra sem escrita. Cliente do Composite do catalogo e da
 * Chain of Responsibility de preco; novos tipos/handlers nao exigem condicionais neste consumidor.
 */
@Service
public class RevalidarCheckout {
    private final br.com.fatec.ecommerce.entrega.dominio.RegistroProvedoresFrete fretes;
    private final CarrinhoRepository carrinhos;
    private final EnderecoRepository enderecos;
    private final ConsultarItensVendaveis catalogo;
    private final ConsultarSaldo saldos;
    private final PipelinePreco pipeline;
    private final br.com.fatec.ecommerce.promocao.aplicacao.ContextoDeCompra contexto;

    public RevalidarCheckout(CarrinhoRepository carrinhos, EnderecoRepository enderecos,
            ConsultarItensVendaveis catalogo, ConsultarSaldo saldos, PipelinePreco pipeline) {
        this(carrinhos, enderecos, catalogo, saldos, pipeline, null);
    }
    public RevalidarCheckout(CarrinhoRepository carrinhos, EnderecoRepository enderecos,
            ConsultarItensVendaveis catalogo, ConsultarSaldo saldos, PipelinePreco pipeline,
            br.com.fatec.ecommerce.promocao.aplicacao.ContextoDeCompra contexto) {
        this(carrinhos,enderecos,catalogo,saldos,pipeline,contexto,null);
    }
    @org.springframework.beans.factory.annotation.Autowired
    public RevalidarCheckout(CarrinhoRepository carrinhos, EnderecoRepository enderecos,
            ConsultarItensVendaveis catalogo, ConsultarSaldo saldos, PipelinePreco pipeline,
            br.com.fatec.ecommerce.promocao.aplicacao.ContextoDeCompra contexto,
            br.com.fatec.ecommerce.entrega.dominio.RegistroProvedoresFrete fretes) {
        this.fretes = fretes;
        this.contexto = contexto;
        this.carrinhos = carrinhos;
        this.enderecos = enderecos;
        this.catalogo = catalogo;
        this.saldos = saldos;
        this.pipeline = pipeline;
    }

    CompraRevalidada executar(Long clienteId, Long enderecoId) {
        return executar(clienteId, enderecoId, null);
    }

    List<br.com.fatec.ecommerce.entrega.dominio.OpcaoEntrega> cotar(Long clienteId, Long enderecoId) {
        // Mesma leitura validada de SKUs/endereco/quantidades; sem persistencia ou consumo de recursos.
        var leitura = executar(clienteId, enderecoId, null).previa().confirmacao();
        return fretes.cotar(new br.com.fatec.ecommerce.entrega.dominio.ContextoFrete(leitura.endereco().uf(),
                java.math.BigDecimal.ZERO, Dinheiro.de(leitura.subtotal()),
                leitura.itens().stream().mapToInt(ConfirmacaoCheckout.Linha::quantidade).sum()));
    }

    CompraRevalidada executar(Long clienteId, Long enderecoId, String opcaoEntrega) {
        var endereco = enderecos.buscarAtivo(enderecoId, clienteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Endereço não encontrado."));
        var carrinho = carrinhos.buscarPorClienteId(clienteId)
                .orElseThrow(() -> new ConflitoException("O carrinho está vazio."));
        if (carrinho.getItens().isEmpty()) { throw new ConflitoException("O carrinho está vazio."); }
        Map<String, ItemVendavelInfo> atuais = new LinkedHashMap<>();
        catalogo.existentesPorSkus(carrinho.getItens().stream().map(i -> i.getSku()).toList())
                .forEach(i -> atuais.put(i.sku(), i));
        List<String> indisponiveis = carrinho.getItens().stream()
                .filter(i -> !atuais.containsKey(i.getSku()) || !atuais.get(i.getSku()).disponivelParaVenda())
                .map(i -> i.getSku()).toList();
        if (!indisponiveis.isEmpty()) {
            throw new ConflitoException("Há itens indisponíveis para compra: " + String.join(", ", indisponiveis)) {
                @Override public Map<String, Object> propriedades() { return Map.of("skus", indisponiveis); }
            };
        }
        var linhas = new ArrayList<ConfirmacaoCheckout.Linha>();
        var itensPreco = new ArrayList<ItemPreco>();
        var itensPedido = new ArrayList<DadosNovoPedido.Item>();
        Map<Long, Long> demanda = new TreeMap<>();
        Dinheiro subtotal = Dinheiro.zero();
        int unidades = 0;
        try {
            for (var linha : carrinho.getItens()) {
                var item = atuais.get(linha.getSku());
                var valor = item.preco().multiplicar(linha.getQuantidade());
                subtotal = subtotal.somar(valor);
                unidades = Math.addExact(unidades, linha.getQuantidade());
                linhas.add(new ConfirmacaoCheckout.Linha(item.sku(), item.nome(), item.descricaoVariacao(),
                        linha.getQuantidade(), item.preco().valor(), valor.valor()));
                itensPreco.add(new ItemPreco(item.id(), item.sku(), item.categoriaIds(),
                        linha.getQuantidade(), item.preco()));
                itensPedido.add(new DadosNovoPedido.Item(item.sku(), item.nome(), item.descricaoVariacao(),
                        linha.getQuantidade(), item.preco()));
                for (var componente : item.componentes()) {
                    long quantidade = Math.multiplyExact((long) linha.getQuantidade(), componente.quantidade());
                    demanda.merge(componente.variacaoId(), quantidade, Math::addExact);
                }
            }
            if (demanda.values().stream().anyMatch(q -> q > Integer.MAX_VALUE)) {
                throw new ArithmeticException("Demanda acima da capacidade de reserva");
            }
        } catch (ArithmeticException erro) {
            throw new DadoInvalidoException("A demanda de componentes excede o limite permitido para compra.");
        }
        var componentes = demanda.entrySet().stream()
                .map(e -> new ComponenteAReservar(e.getKey(), Math.toIntExact(e.getValue()))).toList();
        if (componentes.isEmpty()) { throw new ConflitoException("A compra não possui componentes de estoque."); }
        Map<Long, Integer> disponiveis = new TreeMap<>();
        saldos.porVariacoes(demanda.keySet()).forEach(s -> disponiveis.put(s.getVariacaoId(), s.getDisponivel()));
        var insuficiencias = demanda.entrySet().stream()
                .filter(e -> e.getValue() > disponiveis.getOrDefault(e.getKey(), 0))
                .map(e -> new Insuficiencia(e.getKey(), e.getValue(), disponiveis.getOrDefault(e.getKey(), 0)))
                .toList();
        var opcao = opcaoEntrega == null ? null : fretes.selecionar(
                new br.com.fatec.ecommerce.entrega.dominio.ContextoFrete(endereco.getUf(), java.math.BigDecimal.ZERO, subtotal, unidades), opcaoEntrega);
        Dinheiro freteBruto = opcao == null ? Dinheiro.zero() : Dinheiro.de(opcao.custoBruto());
        var dados = contexto == null ? new ContextoPreco(itensPreco, clienteId, false, endereco.getUf(), subtotal, Instant.now(), null, java.util.Set.of(), freteBruto)
                : contexto.criar(itensPreco, clienteId, endereco.getUf(), subtotal, carrinho.getCupomCodigo(), freteBruto);
        var calculo = pipeline.calcular(dados);
        var descontos = calculo.descontos().stream().map(d -> new ConfirmacaoCheckout.Desconto(
                d.nome(), d.alvo().name(), d.valor().valor(), d.origem().name(), d.referenciaId(), d.codigo())).toList();
        var destino = new ConfirmacaoCheckout.Endereco(endereco.getApelido(), endereco.getCep(),
                endereco.getLogradouro(), endereco.getNumero(), endereco.getComplemento(),
                endereco.getBairro(), endereco.getCidade(), endereco.getUf());
        var confirmacao = new ConfirmacaoCheckout(carrinho.getVersao(), linhas, descontos,
                calculo.subtotal().valor(), calculo.frete().valor(), calculo.total().valor(), destino, calculo.cupom(), opcao == null ? null : new ConfirmacaoCheckout.Entrega(
                        opcao.codigo(),opcao.fornecedor(),opcao.modalidade(),opcao.prazoDias()), calculo.freteLiquido().valor());
        var entrega = new EnderecoEntrega(destino.apelido(), destino.cep(), destino.logradouro(),
                destino.numero(), destino.complemento(), destino.bairro(), destino.cidade(), destino.uf());
        var abatimentos = calculo.descontos().stream().map(d -> new DadosNovoPedido.Desconto(d.nome(),
                br.com.fatec.ecommerce.pedido.dominio.OrigemDesconto.valueOf(d.origem().name()),
                br.com.fatec.ecommerce.pedido.dominio.AlvoDoDesconto.valueOf(d.alvo().name()), d.valor(), d.referenciaId(), d.codigo())).toList();
        var totalDescontos = calculo.descontos().stream().map(d -> d.valor()).reduce(Dinheiro.zero(), Dinheiro::somar);
        var pedido = new DadosNovoPedido(clienteId, entrega, itensPedido, abatimentos, calculo.frete(),
                calculo.subtotal(), totalDescontos, calculo.total(), opcao == null ? null :
                        new br.com.fatec.ecommerce.pedido.dominio.DadosEntregaSelecionada(opcao.codigo(), opcao.fornecedor(), opcao.modalidade(), opcao.prazoDias()));
        return new CompraRevalidada(carrinho, new PreviaCheckout(confirmacao, insuficiencias), pedido, componentes);
    }
}
