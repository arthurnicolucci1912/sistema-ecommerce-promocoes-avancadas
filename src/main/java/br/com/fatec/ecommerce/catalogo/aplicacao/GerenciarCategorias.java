package br.com.fatec.ecommerce.catalogo.aplicacao;

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

    private final CategoriaRepository categorias;
    private final ProdutoRepository produtos;

    public GerenciarCategorias(CategoriaRepository categorias, ProdutoRepository produtos) {
        this.categorias = categorias;
        this.produtos = produtos;
    }

    public Categoria criar(String nome, Long paiId) {
        Categoria pai = paiId == null ? null : paiInformado(paiId);
        return categorias.salvar(Categoria.criar(nome, pai, arvore()));
    }

    public Categoria renomear(Long id, String novoNome) {
        Categoria categoria = alvo(id);
        categoria.renomear(novoNome, arvore());
        return categorias.salvar(categoria);
    }

    /** Move a categoria para baixo do novo pai (ou para a raiz, se nulo); ciclo e nome repetido sao 409. */
    public Categoria mover(Long id, Long novoPaiId) {
        Categoria categoria = alvo(id);
        Categoria novoPai = novoPaiId == null ? null : paiInformado(novoPaiId);
        categoria.mover(novoPai, arvore());
        return categorias.salvar(categoria);
    }

    /** Remove a categoria; com subcategorias ou produtos (ativos ou inativos) a remocao e rejeitada (409). */
    public void remover(Long id) {
        Categoria categoria = alvo(id);
        if (!arvore().filhas(id).isEmpty()) {
            throw new ConflitoException("A categoria tem subcategorias e não pode ser removida.");
        }
        if (produtos.existePorCategoriaId(id)) {
            throw new ConflitoException("A categoria tem produtos e não pode ser removida.");
        }
        categorias.remover(categoria);
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
