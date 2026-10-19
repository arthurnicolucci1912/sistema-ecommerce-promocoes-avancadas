package br.com.fatec.ecommerce.catalogo.aplicacao;

import br.com.fatec.ecommerce.catalogo.dominio.ArvoreDeCategorias;
import br.com.fatec.ecommerce.catalogo.dominio.Categoria;
import br.com.fatec.ecommerce.catalogo.dominio.CategoriaRepository;
import br.com.fatec.ecommerce.catalogo.dominio.ComponenteEstoque;
import br.com.fatec.ecommerce.catalogo.dominio.Combo;
import br.com.fatec.ecommerce.catalogo.dominio.ItemVendavelRepository;
import br.com.fatec.ecommerce.catalogo.dominio.Produto;
import br.com.fatec.ecommerce.catalogo.dominio.ProdutoRepository;
import br.com.fatec.ecommerce.catalogo.dominio.VariacaoProduto;
import br.com.fatec.ecommerce.compartilhado.dominio.DadoInvalidoException;
import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import br.com.fatec.ecommerce.compartilhado.dominio.Pagina;
import br.com.fatec.ecommerce.compartilhado.dominio.RecursoNaoEncontradoException;
import br.com.fatec.ecommerce.estoque.aplicacao.ConsultarSaldo;
import br.com.fatec.ecommerce.estoque.dominio.SaldoEstoque;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consulta publica do catalogo (sem restricao de perfil, somente leitura): arvore de categorias, produtos com
 * filtros e paginacao, detalhe de produto e combos, com a disponibilidade vinda do estoque
 * ({@link ConsultarSaldo}; a dependencia catalogo para estoque e permitida, o inverso nao).
 *
 * <p>Visibilidade: so aparecem produtos ativos com ao menos uma variacao ativa, e combos disponiveis para venda
 * (combo e todos os componentes ativos). Produto inexistente, inativo ou sem variacao ativa e combo indisponivel
 * respondem 404. Os saldos sao buscados em lote (uma consulta por chamada), e variacao sem saldo conta como 0.
 *
 * <p>A disponibilidade de um combo e o minimo de {@code disponivel / quantidade} (divisao inteira) entre os
 * componentes de estoque de uma unidade ({@code componentesDeEstoque(1)}, ja agregados); por isso so usa o
 * contrato do Composite, inclusive para combo dentro de combo.
 */
@Service
@Transactional(readOnly = true)
public class ConsultarCatalogo {

    public static final int TAMANHO_PADRAO = 20;
    public static final int TAMANHO_MAXIMO = 100;

    private final CategoriaRepository categorias;
    private final ProdutoRepository produtos;
    private final ItemVendavelRepository itens;
    private final ConsultarSaldo saldos;

    public ConsultarCatalogo(CategoriaRepository categorias, ProdutoRepository produtos, ItemVendavelRepository itens,
            ConsultarSaldo saldos) {
        this.categorias = categorias;
        this.produtos = produtos;
        this.itens = itens;
        this.saldos = saldos;
    }

    /** As categorias raiz, cada uma com as suas subcategorias, ordenadas pelo nome em todos os niveis. */
    public List<NoDeCategoria> arvoreDeCategorias() {
        ArvoreDeCategorias arvore = new ArvoreDeCategorias(categorias.listarTodas());
        return nos(arvore, null);
    }

    /**
     * Produtos visiveis, ordenados pelo nome. Categoria inexistente no filtro resulta em pagina vazia (sem erro). A
     * pagina e validada como nas demais listagens (base 0, tamanho de 1 a {@value #TAMANHO_MAXIMO}).
     */
    public Pagina<ProdutoResumo> produtos(FiltroDeProdutos filtro, int pagina, int tamanho) {
        if (pagina < 0) {
            throw new DadoInvalidoException("A página não pode ser negativa.");
        }
        if (tamanho < 1) {
            throw new DadoInvalidoException("O tamanho da página deve ser de ao menos 1.");
        }
        int limite = Math.min(tamanho, TAMANHO_MAXIMO);
        Set<Long> categoriaIds = null;
        if (filtro.categoriaId() != null) {
            if (categorias.buscarPorId(filtro.categoriaId()).isEmpty()) {
                return new Pagina<>(List.of(), pagina, limite, 0);
            }
            ArvoreDeCategorias arvore = new ArvoreDeCategorias(categorias.listarTodas());
            Set<Long> daCategoria = new LinkedHashSet<>();
            daCategoria.add(filtro.categoriaId());
            arvore.descendentes(filtro.categoriaId()).forEach(c -> daCategoria.add(c.getId()));
            categoriaIds = daCategoria;
        }
        Pagina<Produto> encontrados = produtos.pesquisarVisiveis(categoriaIds, filtro.nome(), filtro.precoMinimo(),
                filtro.precoMaximo(), pagina, limite);
        return new Pagina<>(encontrados.conteudo().stream().map(ConsultarCatalogo::resumo).toList(),
                encontrados.pagina(), encontrados.tamanho(), encontrados.totalElementos());
    }

    public ProdutoDetalhe detalheProduto(Long id) {
        Produto produto = produtos.buscarPorId(id).filter(Produto::isAtivo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado."));
        List<VariacaoProduto> ativas = variacoesAtivas(produto);
        if (ativas.isEmpty()) {
            throw new RecursoNaoEncontradoException("Produto não encontrado.");
        }
        Map<Long, Integer> disponivel = disponivelPorVariacao(ativas.stream().map(VariacaoProduto::getId).toList());
        return new ProdutoDetalhe(produto.getId(), produto.getNome(), produto.getDescricao(), produto.getMarca(),
                produto.getCategoria().getId(),
                ativas.stream()
                        .map(v -> new ProdutoDetalhe.Variacao(v.getId(), v.getSku(), v.getNome(), v.preco(),
                                new LinkedHashMap<>(v.getAtributos()), disponivel.getOrDefault(v.getId(), 0)))
                        .toList());
    }

    /** Combos disponiveis para venda, com a disponibilidade de cada um (saldos buscados em uma unica consulta). */
    public List<ComboDetalhe> combos() {
        List<Combo> visiveis = itens.listarCombos().stream().filter(Combo::estaDisponivelParaVenda).toList();
        Map<Long, List<ComponenteEstoque>> componentes = new LinkedHashMap<>();
        visiveis.forEach(combo -> componentes.put(combo.getId(), combo.componentesDeEstoque(1)));
        Map<Long, Integer> disponivel = disponivelPorVariacao(componentes.values().stream()
                .flatMap(List::stream).map(ComponenteEstoque::variacaoId).distinct().toList());
        return visiveis.stream()
                .map(combo -> detalhe(combo, disponibilidade(componentes.get(combo.getId()), disponivel)))
                .toList();
    }

    public ComboDetalhe detalheCombo(Long id) {
        Combo combo = itens.buscarComboPorId(id).filter(Combo::estaDisponivelParaVenda)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Combo não encontrado."));
        List<ComponenteEstoque> componentes = combo.componentesDeEstoque(1);
        Map<Long, Integer> disponivel = disponivelPorVariacao(componentes.stream()
                .map(ComponenteEstoque::variacaoId).toList());
        return detalhe(combo, disponibilidade(componentes, disponivel));
    }

    private static List<NoDeCategoria> nos(ArvoreDeCategorias arvore, Long paiId) {
        return arvore.filhas(paiId).stream()
                .map(c -> new NoDeCategoria(c.getId(), c.getNome(), nos(arvore, c.getId())))
                .toList();
    }

    private static ProdutoResumo resumo(Produto produto) {
        List<Dinheiro> precos = variacoesAtivas(produto).stream().map(VariacaoProduto::preco).toList();
        return new ProdutoResumo(produto.getId(), produto.getNome(), produto.getMarca(),
                produto.getCategoria().getId(), precos.stream().min(Comparator.naturalOrder()).orElseThrow(),
                precos.stream().max(Comparator.naturalOrder()).orElseThrow());
    }

    private static List<VariacaoProduto> variacoesAtivas(Produto produto) {
        return produto.getVariacoes().stream().filter(VariacaoProduto::isAtivo).toList();
    }

    private static ComboDetalhe detalhe(Combo combo, int disponibilidade) {
        return new ComboDetalhe(combo.getId(), combo.getSku(), combo.getNome(), combo.preco(), disponibilidade,
                combo.getItens().stream()
                        .map(i -> new ComboDetalhe.Componente(i.getItem().getId(), i.getItem().getSku(),
                                i.getItem().getNome(), i.getQuantidade()))
                        .toList());
    }

    private static int disponibilidade(List<ComponenteEstoque> componentes, Map<Long, Integer> disponivel) {
        return componentes.stream()
                .mapToInt(c -> disponivel.getOrDefault(c.variacaoId(), 0) / c.quantidade())
                .min()
                .orElse(0);
    }

    /** Disponivel de cada variacao com saldo, em uma consulta; as sem saldo ficam de fora (contam como 0). */
    private Map<Long, Integer> disponivelPorVariacao(Collection<Long> variacaoIds) {
        Map<Long, Integer> resultado = new HashMap<>();
        for (SaldoEstoque saldo : saldos.porVariacoes(variacaoIds)) {
            resultado.put(saldo.getVariacaoId(), saldo.getDisponivel());
        }
        return resultado;
    }
}
