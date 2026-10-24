package br.com.fatec.ecommerce.promocao.dominio;

import java.util.*;
/** Criador extensivel de beneficio de cupom. */
public interface BeneficioCupomFactory {
    String tipo();
    List<String> parametros();
    BeneficioCupom criar(Map<String,Object> parametros);
}
