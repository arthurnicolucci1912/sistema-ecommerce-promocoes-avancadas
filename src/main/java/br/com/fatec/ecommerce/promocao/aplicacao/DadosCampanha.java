package br.com.fatec.ecommerce.promocao.aplicacao;

import br.com.fatec.ecommerce.promocao.dominio.*;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.Map;

/** Configuracao administrativa validada pelos criadores registrados. */
public record DadosCampanha(Long versao, @NotBlank String nome, @NotBlank String tipo,
        @NotNull Instant inicio, @NotNull Instant fim, int prioridade, @NotNull ModoAcumulacao modo,
        Map<String,Object> parametros, CondicaoConfig condicao) { }
