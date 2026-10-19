package br.com.fatec.ecommerce.catalogo.aplicacao;

import br.com.fatec.ecommerce.catalogo.dominio.Combo;
import br.com.fatec.ecommerce.catalogo.dominio.ItemVendavel;
import br.com.fatec.ecommerce.catalogo.dominio.ItemVendavelRepository;
import br.com.fatec.ecommerce.compartilhado.dominio.ConflitoException;
import br.com.fatec.ecommerce.compartilhado.dominio.DadoInvalidoException;
import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import br.com.fatec.ecommerce.compartilhado.dominio.RecursoNaoEncontradoException;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de administracao de combos (ADMIN e OPERADOR_COMERCIAL): cadastrar, alterar o preco, substituir os
 * componentes e inativar. Os componentes sao enderecados pelo SKU e tratados so pelo contrato
 * {@link ItemVendavel} (variacao ou outro combo), sem distinguir o tipo.
 *
 * <p>Convencao de erro: SKU do combo repetido (contra variacoes e combos) e 409; combo sem componentes, quantidade
 * invalida e SKU de componente inexistente sao dados invalidos (400); combo que contenha a si mesmo, direta ou
 * indiretamente, e 409 (regra de {@link Combo}); combo inexistente no caminho da rota e 404.
 */
@Service
@Transactional
@PreAuthorize("hasAnyRole('ADMIN','OPERADOR_COMERCIAL')")
public class GerenciarCombos {

    private final ItemVendavelRepository itens;

    public GerenciarCombos(ItemVendavelRepository itens) {
        this.itens = itens;
    }

    public Combo cadastrar(String sku, String nome, Dinheiro preco, List<ComponenteDeCombo> componentes) {
        exigirComponentes(componentes);
        if (sku != null && itens.existePorSku(sku.trim())) {
            throw new ConflitoException("Já existe um item do catálogo com o SKU " + sku.trim() + ".");
        }
        Combo combo = new Combo(sku, nome, preco);
        adicionar(combo, componentes);
        return itens.salvar(combo);
    }

    public Combo alterarPreco(Long comboId, Dinheiro novoPreco) {
        Combo combo = combo(comboId);
        combo.alterarPreco(novoPreco);
        return itens.salvar(combo);
    }

    /** Troca todos os componentes pela nova lista; um ciclo (combo contido em um de seus componentes) e 409. */
    public Combo substituirComponentes(Long comboId, List<ComponenteDeCombo> componentes) {
        exigirComponentes(componentes);
        Combo combo = combo(comboId);
        // Remove e grava antes de readicionar: o Hibernate insere antes de apagar no flush, o que violaria a
        // unicidade (combo, item) se um componente fosse mantido.
        combo.removerItens();
        itens.salvar(combo);
        adicionar(combo, componentes);
        return itens.salvar(combo);
    }

    /**
     * Altera preço e/ou componentes na mesma transação ({@code null} mantém o valor atual): se uma das regras
     * falhar, nada é gravado.
     */
    public Combo alterar(Long comboId, Dinheiro novoPreco, List<ComponenteDeCombo> componentes) {
        if (novoPreco == null && componentes == null) {
            throw new DadoInvalidoException("Informe o preço e/ou os componentes do combo.");
        }
        Combo combo = componentes == null ? combo(comboId) : substituirComponentes(comboId, componentes);
        if (novoPreco != null) {
            combo.alterarPreco(novoPreco);
        }
        return itens.salvar(combo);
    }

    public void inativar(Long comboId) {
        Combo combo = combo(comboId);
        combo.inativar();
        itens.salvar(combo);
    }

    private void adicionar(Combo combo, List<ComponenteDeCombo> componentes) {
        for (ComponenteDeCombo componente : componentes) {
            if (componente == null || componente.sku() == null || componente.sku().isBlank()) {
                throw new DadoInvalidoException("O SKU do componente do combo é obrigatório.");
            }
            ItemVendavel item = itens.buscarPorSku(componente.sku().trim()).orElseThrow(
                    () -> new DadoInvalidoException("O componente " + componente.sku().trim() + " não existe."));
            combo.adicionarItem(item, componente.quantidade());
        }
    }

    private static void exigirComponentes(List<ComponenteDeCombo> componentes) {
        if (componentes == null || componentes.isEmpty()) {
            throw new DadoInvalidoException("O combo precisa de ao menos um componente.");
        }
    }

    private Combo combo(Long id) {
        return itens.buscarComboPorId(id).orElseThrow(() -> new RecursoNaoEncontradoException("Combo não encontrado."));
    }
}
