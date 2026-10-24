package br.com.fatec.ecommerce.compartilhado.dominio;

import java.time.Instant;
import java.util.*;

/** Fato administrativo com snapshot JSON imutável; nunca recebe entidades ou credenciais. */
public record OperacaoAdministrativa(Long usuarioId, Instant ocorridoEm, String operacao,
        String recursoTipo, Long recursoId, Map<String, Object> detalhes) implements EventoDominio {
    public OperacaoAdministrativa {
        if (usuarioId == null || usuarioId <= 0 || recursoId == null || recursoId <= 0
                || ocorridoEm == null || operacao == null || !operacao.matches("[A-Z][A-Z0-9_]{0,99}")
                || recursoTipo == null || !recursoTipo.matches("[A-Z][A-Z0-9_]{0,99}")) {
            throw new DadoInvalidoException("Identificação da operação administrativa inválida.");
        }
        Objects.requireNonNull(detalhes, "Detalhes obrigatórios");
        detalhes = copiarMapa(detalhes);
    }
    private static Map<String,Object> copiarMapa(Map<?,?> origem) {
        Map<String,Object> copia = new LinkedHashMap<>();
        origem.forEach((chave, valor) -> {
            if (!(chave instanceof String nome) || nome.isBlank()
                    || nome.toLowerCase(Locale.ROOT).matches(".*(senha|password|hash|token|authorization|credencial).*")) {
                throw new DadoInvalidoException("Campo não permitido na auditoria.");
            }
            copia.put(nome, copiar(valor));
        });
        return Collections.unmodifiableMap(copia);
    }
    private static Object copiar(Object valor) {
        if (valor == null || valor instanceof String || valor instanceof Boolean
                || valor instanceof Integer || valor instanceof Long || valor instanceof java.math.BigDecimal
                || valor instanceof java.math.BigInteger || valor instanceof Double) return valor;
        if (valor instanceof Map<?,?> mapa) return copiarMapa(mapa);
        if (valor instanceof List<?> lista) return lista.stream().map(OperacaoAdministrativa::copiar).toList();
        throw new DadoInvalidoException("Detalhe de auditoria deve ser um valor JSON.");
    }
}
