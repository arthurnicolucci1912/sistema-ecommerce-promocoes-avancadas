package br.com.fatec.ecommerce.pedido;

import br.com.fatec.ecommerce.compartilhado.dominio.EventoDominio;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.event.EventListener;

/**
 * Ouvinte de teste: guarda todo evento de dominio do pedido publicado. Registrado so nos testes que o importam;
 * mostra que quem se inscreve nao altera o modulo pedido.
 */
@TestConfiguration
public class CapturaDeEventosDoPedido {

    private final List<EventoDominio> recebidos = new ArrayList<>();

    @EventListener
    void ao(EventoDominio evento) {
        if (evento.getClass().getPackageName().startsWith("br.com.fatec.ecommerce.pedido")) {
            recebidos.add(evento);
        }
    }

    public List<EventoDominio> recebidos() {
        return List.copyOf(recebidos);
    }

    public void limpar() {
        recebidos.clear();
    }
}
