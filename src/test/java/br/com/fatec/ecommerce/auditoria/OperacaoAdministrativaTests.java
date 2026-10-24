package br.com.fatec.ecommerce.auditoria;

import static org.assertj.core.api.Assertions.*;
import br.com.fatec.ecommerce.compartilhado.dominio.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

class OperacaoAdministrativaTests {
    OperacaoAdministrativa evento(Map<String,Object> detalhes) {
        return new OperacaoAdministrativa(2L, Instant.now(), "PRODUTO_ALTERADO", "PRODUTO", 3L, detalhes);
    }
    @Test void copiaProfundaDoSnapshot() {
        var valores = new ArrayList<>(List.of("antigo"));
        var interno = new LinkedHashMap<String,Object>(); interno.put("valores", valores);
        var original = new LinkedHashMap<String,Object>(); original.put("antes", interno);
        var e = evento(original); valores.add("novo"); interno.clear(); original.clear();
        assertThat(e.detalhes()).isEqualTo(Map.of("antes", Map.of("valores", List.of("antigo"))));
        assertThatThrownBy(() -> e.detalhes().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> ((Map<?,?>) e.detalhes().get("antes")).clear()).isInstanceOf(UnsupportedOperationException.class);
    }
    @Test void rejeitaCredenciaisMesmoAninhadasEObjetosArbitrarios() {
        for (String campo : List.of("senha", "senhaHash", "token", "Authorization", "password")) {
            assertThatThrownBy(() -> evento(Map.of("depois", Map.of(campo, "segredo")))).isInstanceOf(DadoInvalidoException.class);
        }
        assertThatThrownBy(() -> evento(Map.of("entidade", new Object()))).isInstanceOf(DadoInvalidoException.class);
    }
    @Test void exigeIdentificacoesInstanteEDetalhes() {
        assertThatThrownBy(() -> new OperacaoAdministrativa(null, Instant.now(), "X", "X", 1L, Map.of())).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> new OperacaoAdministrativa(1L, null, "X", "X", 1L, Map.of())).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> new OperacaoAdministrativa(1L, Instant.now(), " ", "X", 1L, Map.of())).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> new OperacaoAdministrativa(1L, Instant.now(), "X", "", 1L, Map.of())).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> new OperacaoAdministrativa(1L, Instant.now(), "X", "X", 0L, Map.of())).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> evento(null)).isInstanceOf(NullPointerException.class);
    }
}
