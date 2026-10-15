package br.com.fatec.ecommerce.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fatec.ecommerce.TesteDeIntegracao;
import br.com.fatec.ecommerce.seguranca.UsuariosParaTeste;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/** A migracao V3 cria as tabelas do catalogo e do estoque e o banco rejeita valores fora das restricoes. */
@TesteDeIntegracao
class MigracaoV3Tests {

    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    UsuariosParaTeste helper;

    @BeforeEach
    void preparar() {
        helper.limpar();
    }

    private long inserirCategoria(String nome, Long paiId) {
        return jdbc.queryForObject("INSERT INTO categoria (nome, pai_id) VALUES (?, ?) RETURNING id", Long.class,
                nome, paiId);
    }

    private long inserirItem(String sku, String preco, String tipo) {
        return jdbc.queryForObject("INSERT INTO item_vendavel (sku, nome, preco, tipo) VALUES (?, 'Item', ?::numeric, ?) "
                + "RETURNING id", Long.class, sku, preco, tipo);
    }

    @Test
    void flywayAplicaV3ECriaAsTabelasDeCatalogoEEstoque() {
        List<String> tabelas = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'", String.class);
        assertThat(tabelas).contains("categoria", "produto", "item_vendavel", "variacao_produto", "variacao_atributo",
                "combo", "combo_item", "saldo_estoque", "reserva_estoque");
        assertThat(jdbc.queryForObject("SELECT success FROM flyway_schema_history WHERE version = '3'",
                Boolean.class)).isTrue();
    }

    @Test
    void skuDuplicadoEmItemVendavelERejeitado() {
        inserirItem("SKU-1", "10.00", "VARIACAO");

        assertThatThrownBy(() -> inserirItem("SKU-1", "20.00", "COMBO"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void precoMenorOuIgualAZeroERejeitado() {
        assertThatThrownBy(() -> inserirItem("SKU-0", "0", "VARIACAO"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> inserirItem("SKU-NEG", "-1", "VARIACAO"))
                .isInstanceOf(DataIntegrityViolationException.class);
        inserirItem("SKU-OK", "0.01", "VARIACAO");
    }

    @Test
    void tipoDeItemForaDeVariacaoOuComboERejeitado() {
        assertThatThrownBy(() -> inserirItem("SKU-T", "10", "OUTRO")).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void nomeDeCategoriaERejeitadoSeRepetidoSobOMesmoPaiSemDiferenciarCaixa() {
        long roupas = inserirCategoria("Roupas", null);
        inserirCategoria("Camisetas", roupas);

        assertThatThrownBy(() -> inserirCategoria("camisetas", roupas))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> inserirCategoria("ROUPAS", null)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void nomeDeCategoriaPodeSeRepetirSobPaisDiferentes() {
        long roupas = inserirCategoria("Roupas", null);
        long casa = inserirCategoria("Casa", null);

        inserirCategoria("Outros", roupas);
        inserirCategoria("outros", casa);
    }

    @Test
    void saldoNaoAceitaDisponivelNemReservadoNegativos() {
        String insercao = "INSERT INTO saldo_estoque (variacao_id, disponivel, reservado) VALUES (?, ?, ?)";
        jdbc.update(insercao, 1L, 5, 0);

        assertThatThrownBy(() -> jdbc.update(insercao, 2L, -1, 0)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(insercao, 3L, 0, -1)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void saldoAceitaVariacaoSemExistirNoCatalogo() {
        jdbc.update("INSERT INTO saldo_estoque (variacao_id) VALUES (999999)");

        assertThat(jdbc.queryForObject("SELECT disponivel FROM saldo_estoque WHERE variacao_id = 999999",
                Integer.class)).isZero();
    }

    @Test
    void reservaRestringeSituacaoEQuantidade() {
        String insercao = "INSERT INTO reserva_estoque (pedido_id, variacao_id, quantidade, situacao) "
                + "VALUES (?, ?, ?, ?)";
        long pedido = 100;
        long variacao = 1;
        for (String situacao : List.of("ATIVA", "LIBERADA", "BAIXADA", "REPOSTA")) {
            jdbc.update(insercao, pedido++, variacao, 1, situacao);
        }

        assertThatThrownBy(() -> jdbc.update(insercao, 200L, 1L, 1, "INEXISTENTE"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(insercao, 201L, 1L, 0, "ATIVA"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void reservaDuplicadaParaPedidoEVariacaoERejeitada() {
        String insercao = "INSERT INTO reserva_estoque (pedido_id, variacao_id, quantidade, situacao) "
                + "VALUES (?, ?, 1, 'ATIVA')";
        jdbc.update(insercao, 300L, 1L);
        jdbc.update(insercao, 300L, 2L);
        jdbc.update(insercao, 301L, 1L);

        assertThatThrownBy(() -> jdbc.update(insercao, 300L, 1L)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void comboItemExigeQuantidadePositivaEUnicidadeENaoContemASiMesmo() {
        long combo = inserirItem("COMBO-1", "50", "COMBO");
        jdbc.update("INSERT INTO combo (id) VALUES (?)", combo);
        long item = inserirItem("VAR-1", "10", "VARIACAO");
        String insercao = "INSERT INTO combo_item (combo_id, item_id, quantidade) VALUES (?, ?, ?)";
        jdbc.update(insercao, combo, item, 2);

        assertThatThrownBy(() -> jdbc.update(insercao, combo, item, 1)).isInstanceOf(DataIntegrityViolationException.class);
        long outro = inserirItem("VAR-2", "10", "VARIACAO");
        assertThatThrownBy(() -> jdbc.update(insercao, combo, outro, 0)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(insercao, combo, combo, 1)).isInstanceOf(DataIntegrityViolationException.class);
    }
}
