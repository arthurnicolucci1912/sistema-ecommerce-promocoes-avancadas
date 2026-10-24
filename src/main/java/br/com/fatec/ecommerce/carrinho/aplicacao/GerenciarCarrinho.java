package br.com.fatec.ecommerce.carrinho.aplicacao;

import br.com.fatec.ecommerce.carrinho.dominio.Carrinho;
import br.com.fatec.ecommerce.carrinho.dominio.CarrinhoRepository;
import br.com.fatec.ecommerce.catalogo.aplicacao.ConsultarItensVendaveis;
import br.com.fatec.ecommerce.catalogo.aplicacao.ItemVendavelInfo;
import br.com.fatec.ecommerce.cliente.dominio.ClienteRepository;
import br.com.fatec.ecommerce.compartilhado.dominio.ConflitoException;
import br.com.fatec.ecommerce.compartilhado.dominio.DadoInvalidoException;
import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import br.com.fatec.ecommerce.compartilhado.dominio.RecursoNaoEncontradoException;
import br.com.fatec.ecommerce.preco.aplicacao.PipelinePreco;
import br.com.fatec.ecommerce.promocao.dominio.ContextoPreco;
import br.com.fatec.ecommerce.promocao.dominio.ItemPreco;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Casos de uso do carrinho proprio; a identidade vem do token tambem na fronteira de aplicacao. */
@Service
@Transactional
@PreAuthorize("hasRole('CLIENTE')")
public class GerenciarCarrinho {
    private final CarrinhoRepository carrinhos;
    private final ClienteRepository clientes;
    private final ConsultarItensVendaveis catalogo;
    private final ConsultarDisponibilidadeDoCarrinho disponibilidade;
    private final PipelinePreco pipeline;
    private final br.com.fatec.ecommerce.promocao.aplicacao.ContextoDeCompra contexto;

    public GerenciarCarrinho(CarrinhoRepository carrinhos, ClienteRepository clientes, ConsultarItensVendaveis catalogo,
            ConsultarDisponibilidadeDoCarrinho disponibilidade, PipelinePreco pipeline) {
        this(carrinhos, clientes, catalogo, disponibilidade, pipeline, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public GerenciarCarrinho(CarrinhoRepository carrinhos, ClienteRepository clientes, ConsultarItensVendaveis catalogo,
            ConsultarDisponibilidadeDoCarrinho disponibilidade, PipelinePreco pipeline,
            br.com.fatec.ecommerce.promocao.aplicacao.ContextoDeCompra contexto) {
        this.carrinhos = carrinhos;
        this.clientes = clientes;
        this.catalogo = catalogo;
        this.disponibilidade = disponibilidade;
        this.pipeline = pipeline;
        this.contexto = contexto;
    }

    @Transactional(readOnly = true)
    public ResumoCarrinho consultar() { return resumo(carregar()); }

    public ResumoCarrinho incluir(String sku, int quantidade) {
        validarQuantidade(quantidade);
        if (sku == null || sku.isBlank() || sku.trim().length() > 100) {
            throw new DadoInvalidoException("O SKU deve ter entre 1 e 100 caracteres.");
        }
        Carrinho carrinho = carregar();
        ItemVendavelInfo item = catalogo.porSku(sku);
        exigirVenda(item);
        carrinho.incluir(item.sku(), item.id(), item.nome(), item.preco(), quantidade);
        return salvarEResumir(carrinho);
    }

    public ResumoCarrinho alterarQuantidade(String sku, int quantidade) {
        validarQuantidade(quantidade);
        Carrinho carrinho = carregar();
        var atual = carrinho.item(sku);
        if (quantidade > atual.getQuantidade()) {
            exigirVenda(catalogo.porSku(atual.getSku()));
        }
        carrinho.alterarQuantidade(sku, quantidade);
        return salvarEResumir(carrinho);
    }

    public ResumoCarrinho remover(String sku) {
        Carrinho carrinho = carregar();
        carrinho.remover(sku);
        if (carrinho.getId() == null && carrinho.getItens().isEmpty()) { return resumo(carrinho); }
        return salvarEResumir(carrinho);
    }

    private Carrinho carregar() {
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long clienteId = clientes.buscarPorUsuarioId(Long.valueOf(jwt.getSubject()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado.")).getId();
        return carrinhos.buscarPorClienteId(clienteId).orElseGet(() -> new Carrinho(clienteId));
    }

    public ResumoCarrinho aplicarCupom(String codigo) {
        Carrinho carrinho = carregar();
        if (carrinho.getItens().isEmpty()) { throw new ConflitoException("O carrinho está vazio."); }
        carrinho.selecionarCupom(br.com.fatec.ecommerce.promocao.dominio.Cupom.normalizar(codigo));
        var resultado = resumo(carrinho);
        if (resultado.cupom() != null && !resultado.cupom().valido()) {
            throw new br.com.fatec.ecommerce.promocao.dominio.CupomRejeitadoException(resultado.cupom().motivo());
        }
        carrinhos.salvar(carrinho);
        return resultado;
    }

    public ResumoCarrinho removerCupom() {
        Carrinho carrinho = carregar(); carrinho.selecionarCupom(null);
        return carrinho.getId() == null ? resumo(carrinho) : salvarEResumir(carrinho);
    }

    private ResumoCarrinho salvarEResumir(Carrinho carrinho) {
        // Calcula antes do flush: falhas de validacao nao deixam alteracoes parciais.
        ResumoCarrinho resumo = resumo(carrinho, true);
        carrinhos.salvar(carrinho);
        return resumo;
    }

    private ResumoCarrinho resumo(Carrinho carrinho) {
        return resumo(carrinho, false);
    }

    private ResumoCarrinho resumo(Carrinho carrinho, boolean atualizarReferencias) {
        Map<String, ItemVendavelInfo> atuais = new LinkedHashMap<>();
        catalogo.existentesPorSkus(carrinho.getItens().stream().map(i -> i.getSku()).toList())
                .forEach(i -> atuais.put(i.sku(), i));
        var linhas = new ArrayList<ResumoCarrinho.Linha>();
        var itensPreco = new ArrayList<ItemPreco>();
        Dinheiro subtotal = Dinheiro.zero();
        for (var linha : carrinho.getItens()) {
            var atual = atuais.get(linha.getSku());
            if (atualizarReferencias && atual != null) {
                carrinho.atualizarReferencia(linha.getSku(), atual.id(), atual.nome(), atual.preco());
            }
            Dinheiro preco = atual == null ? linha.getPrecoReferencia() : atual.preco();
            Dinheiro valor = preco.multiplicar(linha.getQuantidade());
            subtotal = subtotal.somar(valor);
            linhas.add(new ResumoCarrinho.Linha(linha.getSku(), atual == null ? linha.getNome() : atual.nome(),
                    linha.getQuantidade(), preco.valor(), valor.valor(), atual != null && atual.disponivelParaVenda(),
                    atual == null));
            itensPreco.add(new ItemPreco(atual == null ? linha.getItemVendavelId() : atual.id(), linha.getSku(),
                    atual == null ? Set.of() : atual.categoriaIds(), linha.getQuantidade(), preco));
        }
        var dados = contexto == null ? new ContextoPreco(itensPreco, carrinho.getClienteId(), false, null, subtotal, Instant.now())
                : contexto.criar(itensPreco, carrinho.getClienteId(), contexto.ufPrincipal(carrinho.getClienteId()), subtotal, carrinho.getCupomCodigo());
        var calculo = pipeline.calcular(dados);
        return new ResumoCarrinho(linhas, calculo.subtotal().valor(), calculo.descontos().stream()
                .map(d -> new ResumoCarrinho.Desconto(d.nome(), d.valor().valor(), d.alvo().name(), d.origem().name(), d.referenciaId(), d.codigo())).toList(),
                calculo.frete().valor(), calculo.total().valor(), disponibilidade.consultar(carrinho, atuais), calculo.cupom());
    }

    private static void exigirVenda(ItemVendavelInfo item) {
        if (!item.disponivelParaVenda()) { throw new ConflitoException("O item não está disponível para venda."); }
    }

    private static void validarQuantidade(int quantidade) {
        if (quantidade <= 0) { throw new DadoInvalidoException("A quantidade deve ser um inteiro positivo."); }
    }
}
