package br.com.fatec.ecommerce.catalogo;

import br.com.fatec.ecommerce.catalogo.dominio.evento.VariacaoCadastrada;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.event.EventListener;

/** Ouvinte de teste: guarda os VariacaoCadastrada publicados. Registrado so nos testes que o importam. */
@TestConfiguration
public class CapturaDeEventosDoCatalogo {

    private final List<VariacaoCadastrada> recebidos = new ArrayList<>();

    @EventListener
    void ao(VariacaoCadastrada evento) {
        recebidos.add(evento);
    }

    public List<VariacaoCadastrada> recebidos() {
        return List.copyOf(recebidos);
    }

    public void limpar() {
        recebidos.clear();
    }
}
