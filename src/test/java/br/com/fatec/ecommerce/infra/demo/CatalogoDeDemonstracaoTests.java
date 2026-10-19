package br.com.fatec.ecommerce.infra.demo;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.fatec.ecommerce.TesteDeIntegracao;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Perfis "dev" e "test": o catalogo e o estoque de demonstracao ja foram semeados na subida do contexto. */
@TesteDeIntegracao
@ActiveProfiles({"dev", "test"})
class CatalogoDeDemonstracaoTests {

    @Autowired
    UsuariosDeDemonstracao runner;
    @Autowired
    JdbcTemplate jdbc;

    private long contar(String tabela) {
        return jdbc.queryForObject("select count(*) from " + tabela, Long.class);
    }

    private Map<String, Object> saldo(String sku) {
        return jdbc.queryForMap("select s.disponivel, s.reservado from saldo_estoque s "
                + "join item_vendavel i on i.id = s.variacao_id where i.sku = ?", sku);
    }

    @Test
    void semeiaCategoriasProdutosVariacoesECombo() {
        List<String> categorias = jdbc.queryForList("select coalesce(p.nome || ' > ', '') || c.nome from categoria c "
                + "left join categoria p on p.id = c.pai_id order by 1", String.class);
        assertThat(categorias).containsExactlyInAnyOrder("Roupas", "Roupas > Camisetas", "Casa", "Casa > Cozinha",
                "Eletrônicos", "Eletrônicos > Periféricos");

        assertThat(jdbc.queryForList("select nome from produto order by nome", String.class)).containsExactly(
                "Café Especial 250 g", "Camiseta Básica", "Caneca de Cerâmica 300 ml", "Teclado Mecânico");
        assertThat(jdbc.queryForList("select sku from item_vendavel where tipo = 'VARIACAO' order by sku",
                String.class)).containsExactly("CAF-ESP-250", "CAM-BAS-G", "CAM-BAS-M", "CAM-BAS-P", "CAN-CER-300",
                "TEC-MEC-01");
        assertThat(jdbc.queryForObject("select count(*) from variacao_atributo a join item_vendavel i on "
                + "i.id = a.variacao_id where i.sku = 'CAM-BAS-M' and ((a.nome = 'Tamanho' and a.valor = 'M') "
                + "or (a.nome = 'Cor' and a.valor = 'Azul'))", Long.class)).isEqualTo(2);

        assertThat(jdbc.queryForObject("select preco from item_vendavel where sku = 'KIT-CAFE'",
                java.math.BigDecimal.class)).isEqualByComparingTo("79.90");
        assertThat(jdbc.queryForList("select i.sku || 'x' || ci.quantidade from combo_item ci join item_vendavel c "
                + "on c.id = ci.combo_id join item_vendavel i on i.id = ci.item_id where c.sku = 'KIT-CAFE' "
                + "order by 1", String.class)).containsExactly("CAF-ESP-250x2", "CAN-CER-300x1");
    }

    @Test
    void estoqueInicialEOsSaldosAposAReservaDoPedidoDeExemplo() {
        assertThat(saldo("CAM-BAS-P")).containsEntry("disponivel", 20).containsEntry("reservado", 0);
        assertThat(saldo("CAM-BAS-M")).containsEntry("disponivel", 18).containsEntry("reservado", 2);
        assertThat(saldo("CAM-BAS-G")).containsEntry("disponivel", 20).containsEntry("reservado", 0);
        assertThat(saldo("CAN-CER-300")).containsEntry("disponivel", 14).containsEntry("reservado", 1);
        assertThat(saldo("CAF-ESP-250")).containsEntry("disponivel", 30).containsEntry("reservado", 0);
        assertThat(saldo("TEC-MEC-01")).containsEntry("disponivel", 1).containsEntry("reservado", 0);
    }

    @Test
    void pedidoDeExemploTemReservaAtivaDosSeusItens() {
        List<String> reservas = jdbc.queryForList("select i.sku || 'x' || r.quantidade || ':' || r.situacao "
                + "from reserva_estoque r join item_vendavel i on i.id = r.variacao_id order by 1", String.class);
        assertThat(reservas).containsExactly("CAM-BAS-Mx2:ATIVA", "CAN-CER-300x1:ATIVA");
    }

    @Test
    void executarORunnerDuasVezesNaoDuplicaNemSomaEstoque() {
        long categorias = contar("categoria");
        long produtos = contar("produto");
        long itens = contar("item_vendavel");
        long combos = contar("combo_item");
        long saldos = contar("saldo_estoque");
        long pedidos = contar("pedido");
        long reservas = contar("reserva_estoque");

        runner.run(null);
        runner.run(null);

        assertThat(contar("categoria")).isEqualTo(categorias).isEqualTo(6);
        assertThat(contar("produto")).isEqualTo(produtos).isEqualTo(4);
        assertThat(contar("item_vendavel")).isEqualTo(itens).isEqualTo(7);
        assertThat(contar("combo_item")).isEqualTo(combos).isEqualTo(2);
        assertThat(contar("saldo_estoque")).isEqualTo(saldos).isEqualTo(6);
        assertThat(contar("pedido")).isEqualTo(pedidos).isEqualTo(1);
        assertThat(contar("reserva_estoque")).isEqualTo(reservas).isEqualTo(2);
        assertThat(saldo("CAM-BAS-M")).containsEntry("disponivel", 18).containsEntry("reservado", 2);
        assertThat(saldo("TEC-MEC-01")).containsEntry("disponivel", 1).containsEntry("reservado", 0);
    }
}
