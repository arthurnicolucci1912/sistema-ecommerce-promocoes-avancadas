package br.com.fatec.ecommerce.promocao.dominio;

import java.time.Instant;
import java.util.List;
/** Porta de leitura de estrategias configuradas, implementada em infraestrutura. */
public interface FonteDePromocoes { List<Promocao> vigentes(Instant momento); }
