package br.com.fatec.ecommerce.auditoria.web;

import br.com.fatec.ecommerce.auditoria.aplicacao.ConsultarAuditoria;
import br.com.fatec.ecommerce.auditoria.dominio.FiltroAuditoria;
import br.com.fatec.ecommerce.auditoria.dominio.RegistroAuditoriaInfo;
import br.com.fatec.ecommerce.compartilhado.dominio.DadoInvalidoException;
import br.com.fatec.ecommerce.compartilhado.web.PaginaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Consulta somente leitura; não oferece criação, edição ou exclusão de registros. */
@RestController
@RequestMapping("/api/admin/auditoria")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Auditoria", description = "Trilha administrativa somente leitura, exclusiva de ADMIN.")
public class AuditoriaController {
    private final ConsultarAuditoria consultar;

    public AuditoriaController(ConsultarAuditoria consultar) {
        this.consultar = consultar;
    }

    @GetMapping
    @Operation(summary = "Consulta a trilha administrativa",
            description = "Filtros opcionais combinados por AND. Período inclusivo em ISO-8601 com fuso; resposta UTC. "
                    + "Ordem ocorridoEm DESC, id DESC. page começa em 0; size padrão 20, entre 1 e 100. "
                    + "Operação desconhecida bem formada retorna página vazia. Não há rotas de edição ou exclusão.")
    @ApiResponse(responseCode = "200", description = "Página e total de registros correspondentes, inclusive lista vazia")
    @ApiResponse(responseCode = "400", description = "Filtro ou paginação inválidos, instante malformado ou intervalo invertido")
    @ApiResponse(responseCode = "401", description = "Autenticação ausente ou inválida")
    @ApiResponse(responseCode = "403", description = "Perfil diferente de ADMIN")
    public PaginaResponse<RegistroAuditoriaInfo> listar(
            @Parameter(description = "Identificador positivo do usuário responsável", schema = @Schema(type = "integer", format = "int64", minimum = "1"))
            @RequestParam(required = false) String usuarioId,
            @Parameter(description = "Nome exato da operação", schema = @Schema(pattern = "[A-Z][A-Z0-9_]{0,99}", maxLength = 100))
            @RequestParam(required = false) String operacao,
            @Parameter(description = "Início inclusivo", schema = @Schema(type = "string", format = "date-time"))
            @RequestParam(required = false) String inicio,
            @Parameter(description = "Fim inclusivo", schema = @Schema(type = "string", format = "date-time"))
            @RequestParam(required = false) String fim,
            @Parameter(schema = @Schema(minimum = "0")) @RequestParam(defaultValue = "0") int page,
            @Parameter(schema = @Schema(minimum = "1", maximum = "100")) @RequestParam(defaultValue = "20") int size) {
        var filtro = new FiltroAuditoria(identificador(usuarioId), operacao, instante(inicio), instante(fim), page, size);
        return PaginaResponse.de(consultar.consultar(filtro), registro -> registro);
    }

    private static Long identificador(String valor) {
        if (valor == null) return null;
        try {
            if (!valor.matches("[0-9]+")) throw new NumberFormatException();
            return Long.valueOf(valor);
        } catch (NumberFormatException ex) {
            throw new DadoInvalidoException("usuarioId deve ser um identificador positivo válido.");
        }
    }

    private static Instant instante(String valor) {
        if (valor == null) return null;
        try {
            return Instant.parse(valor);
        } catch (DateTimeParseException ex) {
            throw new DadoInvalidoException("inicio e fim devem ser instantes ISO-8601 com fuso horário.");
        }
    }
}
