package br.com.fatec.ecommerce.carrinho.infra;

import static org.assertj.core.api.Assertions.*;
import br.com.fatec.ecommerce.TesteDeIntegracao;
import br.com.fatec.ecommerce.carrinho.dominio.Carrinho;
import br.com.fatec.ecommerce.carrinho.dominio.CarrinhoRepository;
import br.com.fatec.ecommerce.cliente.ClientesParaTeste;
import br.com.fatec.ecommerce.compartilhado.dominio.ConflitoException;
import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import br.com.fatec.ecommerce.seguranca.UsuariosParaTeste;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@TesteDeIntegracao
class CarrinhoPersistenciaTests {
    @Autowired CarrinhoRepository repositorio;
    @Autowired ClientesParaTeste clientes;
    @Autowired UsuariosParaTeste usuarios;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transacoes;
    Long clienteId;

    @BeforeEach
    void preparar() {
        usuarios.limpar();
        clienteId = clientes.criar("carrinho@teste.local", 401).cliente().getId();
    }

    private Carrinho carregar() {
        return new TransactionTemplate(transacoes).execute(t -> {
            Carrinho carrinho = repositorio.buscarPorClienteId(clienteId).orElseThrow();
            carrinho.getItens().size();
            return carrinho;
        });
    }

    private Carrinho criar() {
        Carrinho carrinho = new Carrinho(clienteId);
        carrinho.incluir("SKU", 10L, "Item", Dinheiro.de("12.50"), 2);
        return new TransactionTemplate(transacoes).execute(t -> repositorio.salvar(carrinho));
    }

    @Test
    void migraEPersisteEntreSessoesComRestricoesNoBanco() {
        Carrinho criado = criar();
        assertThat(jdbc.queryForObject("SELECT success FROM flyway_schema_history WHERE version = '4'", Boolean.class))
                .isTrue();
        Carrinho recarregado = carregar();
        assertThat(recarregado.getId()).isEqualTo(criado.getId());
        assertThat(recarregado.item("SKU").getQuantidade()).isEqualTo(2);
        assertThat(recarregado.item("SKU").getPrecoReferencia()).isEqualTo(Dinheiro.de("12.50"));
        assertThatThrownBy(() -> jdbc.update("INSERT INTO carrinho(cliente_id) VALUES (?)", clienteId))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO carrinho(cliente_id) VALUES (-1)"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE item_carrinho SET quantidade = 0 WHERE carrinho_id = ?", criado.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO item_carrinho SELECT * FROM item_carrinho"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void alteracoesConcorrentesSomenteNasLinhasNaoSeSobrescrevem() throws Exception {
        criar();
        Carrinho primeira = carregar();
        Carrinho segunda = carregar();
        primeira.alterarQuantidade("SKU", 3);
        segunda.alterarQuantidade("SKU", 4);
        CyclicBarrier inicio = new CyclicBarrier(2);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            var resultados = java.util.List.of(primeira, segunda).stream().map(c -> executor.submit(() -> {
                inicio.await(10, TimeUnit.SECONDS);
                try {
                    new TransactionTemplate(transacoes).execute(t -> repositorio.salvar(c));
                    return true;
                } catch (ConflitoException erro) {
                    return false;
                }
            })).toList();
            assertThat(java.util.List.of(resultados.get(0).get(20, TimeUnit.SECONDS),
                    resultados.get(1).get(20, TimeUnit.SECONDS))).containsExactlyInAnyOrder(true, false);
        }
        assertThat(carregar().item("SKU").getQuantidade()).isIn(3, 4);
    }
}
