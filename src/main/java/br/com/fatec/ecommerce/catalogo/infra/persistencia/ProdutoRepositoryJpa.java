package br.com.fatec.ecommerce.catalogo.infra.persistencia;

import br.com.fatec.ecommerce.catalogo.dominio.Produto;
import br.com.fatec.ecommerce.catalogo.dominio.ProdutoRepository;
import br.com.fatec.ecommerce.catalogo.dominio.VariacaoProduto;
import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import br.com.fatec.ecommerce.compartilhado.dominio.Pagina;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

/** Adaptador JPA da porta {@link ProdutoRepository}. */
@Repository
class ProdutoRepositoryJpa implements ProdutoRepository {

    private final ProdutoJpaRepository jpa;

    ProdutoRepositoryJpa(ProdutoJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Produto salvar(Produto produto) {
        try {
            return jpa.saveAndFlush(produto);
        } catch (DataIntegrityViolationException erro) {
            throw ViolacoesDeUnicidade.deSku(erro);
        }
    }

    @Override
    public Optional<Produto> buscarPorId(Long id) {
        return jpa.findById(id);
    }

    @Override
    public Optional<Produto> buscarPorVariacaoId(Long variacaoId) {
        return jpa.buscarPorVariacaoId(variacaoId);
    }

    @Override
    public boolean existePorCategoriaId(Long categoriaId) {
        return jpa.existsByCategoriaId(categoriaId);
    }

    @Override
    public Pagina<Produto> pesquisarVisiveis(Collection<Long> categoriaIds, String trechoDoNome,
            Dinheiro precoMinimo, Dinheiro precoMaximo, int pagina, int tamanho) {
        Specification<Produto> filtro = (raiz, consulta, cb) -> {
            List<Predicate> condicoes = new ArrayList<>();
            condicoes.add(cb.isTrue(raiz.get("ativo")));
            if (categoriaIds != null) {
                condicoes.add(categoriaIds.isEmpty() ? cb.disjunction()
                        : raiz.get("categoria").get("id").in(categoriaIds));
            }
            if (trechoDoNome != null && !trechoDoNome.isBlank()) {
                condicoes.add(cb.like(cb.lower(raiz.get("nome")), "%" + escaparLike(trechoDoNome.trim().toLowerCase())
                        + "%", '\\'));
            }
            // O produto so e visivel se alguma variacao ativa (dentro da faixa de preco, quando ha) existe.
            Subquery<Long> variacoes = consulta.subquery(Long.class);
            Root<VariacaoProduto> variacao = variacoes.from(VariacaoProduto.class);
            List<Predicate> daVariacao = new ArrayList<>();
            daVariacao.add(cb.equal(variacao.get("produto"), raiz));
            daVariacao.add(cb.isTrue(variacao.get("ativo")));
            if (precoMinimo != null) {
                daVariacao.add(cb.greaterThanOrEqualTo(variacao.<BigDecimal>get("preco"), precoMinimo.valor()));
            }
            if (precoMaximo != null) {
                daVariacao.add(cb.lessThanOrEqualTo(variacao.<BigDecimal>get("preco"), precoMaximo.valor()));
            }
            variacoes.select(variacao.<Long>get("id")).where(daVariacao.toArray(Predicate[]::new));
            condicoes.add(cb.exists(variacoes));
            return cb.and(condicoes.toArray(Predicate[]::new));
        };
        Page<Produto> resultado = jpa.findAll(filtro,
                PageRequest.of(pagina, tamanho, Sort.by(Sort.Order.asc("nome"), Sort.Order.asc("id"))));
        return new Pagina<>(resultado.getContent(), resultado.getNumber(), resultado.getSize(),
                resultado.getTotalElements());
    }

    private static String escaparLike(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
