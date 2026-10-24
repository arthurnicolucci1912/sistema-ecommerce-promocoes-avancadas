package br.com.fatec.ecommerce.promocao.aplicacao;

import br.com.fatec.ecommerce.promocao.dominio.*;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Subsistema da Facade: consome uma vez por pedido na transacao da compra. */
@Service @Transactional
public class ConfirmarUsoCupom {
    private final UsoCupomRepository usos;
    private final Clock clock;
    public ConfirmarUsoCupom(UsoCupomRepository usos,Clock clock) { this.usos=usos; this.clock=clock; }
    public void executar(Long cupomId,Long versao,Long pedidoId,Long clienteId) { usos.consumir(cupomId,versao,pedidoId,clienteId,clock.instant()); }
    public void devolver(Long pedidoId) { usos.devolver(pedidoId,clock.instant()); }
}
