package br.com.fatec.ecommerce.catalogo.aplicacao;

import br.com.fatec.ecommerce.compartilhado.aplicacao.PublicarOperacaoAdministrativa;
import br.com.fatec.ecommerce.catalogo.dominio.ArvoreDeCategorias;
import br.com.fatec.ecommerce.catalogo.dominio.Categoria;
import br.com.fatec.ecommerce.catalogo.dominio.CategoriaRepository;
import br.com.fatec.ecommerce.catalogo.dominio.ProdutoRepository;
import br.com.fatec.ecommerce.compartilhado.dominio.ConflitoException;
import br.com.fatec.ecommerce.compartilhado.dominio.DadoInvalidoException;
import br.com.fatec.ecommerce.compartilhado.dominio.RecursoNaoEncontradoException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>Observer administrativo: publica snapshots permitidos na transação original; falha da trilha reverte a operação.
 *
 * Casos de uso de administracao de categorias (ADMIN e OPERADOR_COMERCIAL): criar, renomear, mover e remover.
 * As regras (nome unico entre irmas, ciclo) ficam em {@link Categoria} e {@link ArvoreDeCategorias}; aqui so se
 * resolvem as referencias e se aplicam as regras de remocao.
 *
 * <p>Convencao de erro para referencias: a categoria alvo da operacao (no caminho da rota) inexistente e 404; a
 * categoria pai informada no corpo (criar e mover) inexistente e dado invalido (400), como a categoria de um produto.
 */
@Service
@Transactional
@PreAuthorize("hasAnyRole('ADMIN','OPERADOR_COMERCIAL')")
public class GerenciarCategorias {

    private final PublicarOperacaoAdministrativa auditoria;
    private final CategoriaRepository categorias;
    private final ProdutoRepository produtos;

    public GerenciarCategorias(CategoriaRepository categorias, ProdutoRepository produtos, PublicarOperacaoAdministrativa auditoria) {
        this.auditoria = auditoria;
        this.categorias = categorias;
        this.produtos = produtos;
    }

    public Categoria criar(String nome, Long paiId) {
        Categoria pai = paiId == null ? null : paiInformado(paiId);
        Categoria salvo = categorias.salvar(Categoria.criar(nome, pai, arvore()));
        auditar("CATEGORIA_CRIADA", salvo, null);
        return salvo;
    }

    public Categoria renomear(Long id, String novoNome) {
        Categoria categoria = alvo(id);
        var antes = dados(categoria);
        categoria.renomear(novoNome, arvore());
        Categoria salvo = categorias.salvar(categoria);
        auditar("CATEGORIA_ALTERADA", salvo, antes);
        return salvo;
    }

    /** Move a categoria para baixo do novo pai (ou para a raiz, se nulo); ciclo e nome repetido sao 409. */
    public Categoria mover(Long id, Long novoPaiId) {
        Categoria categoria = alvo(id);
        var antes = dados(categoria);
        Categoria novoPai = novoPaiId == null ? null : paiInformado(novoPaiId);
        categoria.mover(novoPai, arvore());
        Categoria salvo = categorias.salvar(categoria);
        auditar("CATEGORIA_ALTERADA", salvo, antes);
        return salvo;
    }

    /** Remove a categoria; com subcategorias ou produtos (ativos ou inativos) a remocao e rejeitada (409). */
    /** Altera nome e pai na mesma transação: se uma das duas regras falhar, nada é gravado. */
    public Categoria alterar(Long id, String novoNome, Long novoPaiId) {
        Categoria categoria = alvo(id);
        var antes = dados(categoria);
        Categoria novoPai = novoPaiId == null ? null : paiInformado(novoPaiId);
        categoria.renomear(novoNome, arvore());
        categoria.mover(novoPai, arvore());
        Categoria salvo = categorias.salvar(categoria);
        auditar("CATEGORIA_ALTERADA", salvo, antes);
        return salvo;
    }

    public void remover(Long id) {
        Categoria categoria = alvo(id);
        var antes = dados(categoria);
        if (!arvore().filhas(id).isEmpty()) {
            throw new ConflitoException("A categoria tem subcategorias e não pode ser removida.");
        }
        if (produtos.existePorCategoriaId(id)) {
            throw new ConflitoException("A categoria tem produtos e não pode ser removida.");
        }
        categorias.remover(categoria);
        auditoria.publicar("CATEGORIA_REMOVIDA", "CATEGORIA", id, antes, null);
    }

    private java.util.Map<String,Object> dados(Categoria c) {
        var d = new java.util.LinkedHashMap<String,Object>();
        d.put("nome", c.getNome()); d.put("paiId", c.getPaiId()); return d;
    }
    private void auditar(String operacao, Categoria c, java.util.Map<String,Object> antes) {
        auditoria.publicar(operacao, "CATEGORIA", c.getId(), antes, dados(c));
    }
    private Categoria alvo(Long id) {
        return categorias.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada."));
    }

    private Categoria paiInformado(Long paiId) {
        return categorias.buscarPorId(paiId)
                .orElseThrow(() -> new DadoInvalidoException("A categoria pai informada não existe."));
    }

    private ArvoreDeCategorias arvore() {
        return new ArvoreDeCategorias(categorias.listarTodas());
    }
}
