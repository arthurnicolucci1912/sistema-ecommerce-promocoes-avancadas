package br.com.fatec.ecommerce.promocao.aplicacao;

import br.com.fatec.ecommerce.promocao.dominio.CondicaoConfig;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.Map;

/** Codigo e imutavel apos criacao; limites sao protegidos junto com usos ativos. */
public record DadosCupom(Long versao, @NotBlank String codigo, @NotBlank String nome, @NotBlank String tipo,
        @NotNull Instant inicio, @NotNull Instant fim, boolean acumulavel, @Positive int limiteTotal,
        @Positive Integer limiteCliente, Map<String,Object> parametros, CondicaoConfig condicao) { }
