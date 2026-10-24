package br.com.fatec.ecommerce.preco.dominio;

import br.com.fatec.ecommerce.promocao.dominio.*;

/** Chain of Responsibility: handler que delega ao motor Strategy, sem reconhecer tipos de campanha. */
public class EtapaPromocoes implements EtapaPreco {
    private final FonteDePromocoes fonte;
    private final MotorDePromocoes motor;
    public EtapaPromocoes(FonteDePromocoes fonte,MotorDePromocoes motor) { this.fonte=fonte; this.motor=motor; }
    @Override public void processar(CalculoPreco calculo) {
        motor.calcular(calculo.contexto(),fonte.vigentes(calculo.contexto().momento())).forEach(calculo::adicionarDesconto);
    }
}
