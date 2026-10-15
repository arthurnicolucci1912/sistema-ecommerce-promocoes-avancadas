package br.com.fatec.ecommerce.catalogo.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.ConflitoException;
import br.com.fatec.ecommerce.compartilhado.dominio.RecursoNaoEncontradoException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Servico de dominio sobre a lista completa de categorias: ancestrais, descendentes e as regras que dependem da
 * arvore inteira (movimento sem ciclo e nome unico entre irmas). Calcula em memoria porque a quantidade de
 * categorias e pequena; se crescer, troca-se por consulta recursiva atras desta mesma classe. Sem pattern.
 */
public final class ArvoreDeCategorias {

    private final Map<Long, Categoria> porId = new HashMap<>();
    private final Map<Long, List<Categoria>> filhasPorPai = new HashMap<>();

    public ArvoreDeCategorias(Collection<Categoria> todas) {
        for (Categoria categoria : todas) {
            porId.put(categoria.getId(), categoria);
            filhasPorPai.computeIfAbsent(categoria.getPaiId(), pai -> new ArrayList<>()).add(categoria);
        }
    }

    /** Ancestrais da categoria, da mais proxima (o pai) ate a raiz; nao inclui a propria categoria. */
    public List<Categoria> ancestrais(Long categoriaId) {
        Categoria atual = buscar(categoriaId);
        List<Categoria> resultado = new ArrayList<>();
        Set<Long> visitadas = new HashSet<>();
        visitadas.add(categoriaId);
        Long paiId = atual.getPaiId();
        while (paiId != null && visitadas.add(paiId)) {
            Categoria pai = buscar(paiId);
            resultado.add(pai);
            paiId = pai.getPaiId();
        }
        return resultado;
    }

    /** A propria categoria seguida dos seus ancestrais (e o que o catalogo informa de um item: "Camisetas, Roupas"). */
    public List<Categoria> categoriaEAncestrais(Long categoriaId) {
        List<Categoria> resultado = new ArrayList<>();
        resultado.add(buscar(categoriaId));
        resultado.addAll(ancestrais(categoriaId));
        return resultado;
    }

    /** Todas as descendentes (filhas, netas...), em largura; nao inclui a propria categoria. */
    public List<Categoria> descendentes(Long categoriaId) {
        buscar(categoriaId);
        List<Categoria> resultado = new ArrayList<>();
        Set<Long> visitadas = new HashSet<>();
        visitadas.add(categoriaId);
        Deque<Long> fila = new ArrayDeque<>();
        fila.add(categoriaId);
        while (!fila.isEmpty()) {
            for (Categoria filha : filhasPorPai.getOrDefault(fila.poll(), List.of())) {
                if (visitadas.add(filha.getId())) {
                    resultado.add(filha);
                    fila.add(filha.getId());
                }
            }
        }
        return resultado;
    }

    /**
     * Valida mover a categoria para baixo de {@code novoPaiId} ({@code null} = raiz). Mover para si mesma ou para
     * uma descendente lanca {@link ConflitoException}.
     */
    public void validarMovimento(Long categoriaId, Long novoPaiId) {
        buscar(categoriaId);
        if (novoPaiId == null) {
            return;
        }
        buscar(novoPaiId);
        boolean ciclo = novoPaiId.equals(categoriaId)
                || descendentes(categoriaId).stream().anyMatch(d -> d.getId().equals(novoPaiId));
        if (ciclo) {
            throw new ConflitoException("Uma categoria não pode ser movida para dentro de si mesma ou de uma descendente.");
        }
    }

    /**
     * Garante que nenhuma irma sob {@code paiId} ({@code null} = raiz) tem o mesmo nome, sem diferenciar maiusculas.
     * {@code ignorarId} exclui a propria categoria (renomear ou mover).
     */
    public void validarNomeUnico(String nome, Long paiId, Long ignorarId) {
        boolean repetido = filhasPorPai.getOrDefault(paiId, List.of()).stream()
                .anyMatch(irma -> !irma.getId().equals(ignorarId) && irma.getNome().equalsIgnoreCase(nome));
        if (repetido) {
            throw new ConflitoException("Já existe uma categoria com esse nome no mesmo nível.");
        }
    }

    private Categoria buscar(Long id) {
        Categoria categoria = porId.get(id);
        if (categoria == null) {
            throw new RecursoNaoEncontradoException("Categoria não encontrada.");
        }
        return categoria;
    }
}
