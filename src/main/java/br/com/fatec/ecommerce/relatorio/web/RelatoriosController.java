package br.com.fatec.ecommerce.relatorio.web;

import br.com.fatec.ecommerce.compartilhado.dominio.DadoInvalidoException;
import br.com.fatec.ecommerce.relatorio.aplicacao.ConsultarRelatoriosGerenciais;
import br.com.fatec.ecommerce.relatorio.dominio.DescontoPorCampanha;
import br.com.fatec.ecommerce.relatorio.dominio.FiltroProdutosMaisVendidos;
import br.com.fatec.ecommerce.relatorio.dominio.PeriodoRelatorio;
import br.com.fatec.ecommerce.relatorio.dominio.ProdutoMaisVendido;
import br.com.fatec.ecommerce.relatorio.dominio.ResumoVendas;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Relatórios gerenciais somente leitura (UC-A10). Sem rotas de criação, edição ou exclusão. */
@RestController
@RequestMapping("/api/admin/relatorios")
@PreAuthorize("hasAnyRole('ADMIN','OPERADOR_COMERCIAL')")
@Tag(name = "Relatórios", description = "Indicadores gerenciais de um período, somente leitura. Venda: pedido pago no "
        + "período e não cancelado. ADMIN e OPERADOR_COMERCIAL; campanhas também PROMOCOES.")
public class RelatoriosController {
    private final ConsultarRelatoriosGerenciais relatorios;

    public RelatoriosController(ConsultarRelatoriosGerenciais relatorios) {
        this.relatorios = relatorios;
    }

    @GetMapping("/vendas")
    @Operation(summary = "Resumo de vendas do período",
            description = "Valor total, quantidade de pedidos, ticket médio (HALF_EVEN, 2 casas), total de descontos "
                    + "e quantidade de cancelamentos. Período inclusivo, ISO-8601 com fuso. Sem dados: valores zero.")
    @ApiResponse(responseCode = "200", description = "Indicadores do período, inclusive zerados")
    @ApiResponse(responseCode = "400", description = "Período ausente, malformado ou invertido")
    @ApiResponse(responseCode = "401", description = "Autenticação ausente ou inválida")
    @ApiResponse(responseCode = "403", description = "Perfil sem acesso")
    public ResumoVendas vendas(@Parameter(description = "Início inclusivo", required = true,
                    schema = @Schema(type = "string", format = "date-time")) @RequestParam(required = false) String inicio,
            @Parameter(description = "Fim inclusivo", required = true,
                    schema = @Schema(type = "string", format = "date-time")) @RequestParam(required = false) String fim) {
        return relatorios.vendas(periodo(inicio, fim));
    }

    @GetMapping("/produtos-mais-vendidos")
    @Operation(summary = "Produtos mais vendidos no período",
            description = "Agrupa por SKU, nome e variação do pedido; ordem quantidade DESC, SKU ASC. limite de 1 a 50 (padrão 10).")
    @ApiResponse(responseCode = "200", description = "Ranking, inclusive lista vazia")
    @ApiResponse(responseCode = "400", description = "Período ou limite inválidos")
    @ApiResponse(responseCode = "401", description = "Autenticação ausente ou inválida")
    @ApiResponse(responseCode = "403", description = "Perfil sem acesso")
    public List<ProdutoMaisVendido> produtosMaisVendidos(
            @Parameter(description = "Início inclusivo", required = true,
                    schema = @Schema(type = "string", format = "date-time")) @RequestParam(required = false) String inicio,
            @Parameter(description = "Fim inclusivo", required = true,
                    schema = @Schema(type = "string", format = "date-time")) @RequestParam(required = false) String fim,
            @Parameter(description = "Quantidade máxima de produtos",
                    schema = @Schema(type = "integer", minimum = "1", maximum = "50", defaultValue = "10"))
            @RequestParam(required = false) String limite) {
        return relatorios.produtosMaisVendidos(new FiltroProdutosMaisVendidos(periodo(inicio, fim), limite(limite)));
    }

    @GetMapping("/campanhas")
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR_COMERCIAL','PROMOCOES')")
    @Operation(summary = "Campanhas mais utilizadas e desconto concedido por campanha",
            description = "Desconto das vendas do período por campanha ou cupom; ordem pedidos DESC, valor DESC, descrição ASC. "
                    + "Acessível também ao perfil PROMOCOES.")
    @ApiResponse(responseCode = "200", description = "Campanhas, inclusive lista vazia")
    @ApiResponse(responseCode = "400", description = "Período ausente, malformado ou invertido")
    @ApiResponse(responseCode = "401", description = "Autenticação ausente ou inválida")
    @ApiResponse(responseCode = "403", description = "Perfil sem acesso")
    public List<DescontoPorCampanha> campanhas(
            @Parameter(description = "Início inclusivo", required = true,
                    schema = @Schema(type = "string", format = "date-time")) @RequestParam(required = false) String inicio,
            @Parameter(description = "Fim inclusivo", required = true,
                    schema = @Schema(type = "string", format = "date-time")) @RequestParam(required = false) String fim) {
        return relatorios.campanhas(periodo(inicio, fim));
    }

    private static PeriodoRelatorio periodo(String inicio, String fim) {
        return new PeriodoRelatorio(instante(inicio), instante(fim));
    }

    private static Instant instante(String valor) {
        if (valor == null) return null;
        try {
            return Instant.parse(valor);
        } catch (DateTimeParseException ex) {
            throw new DadoInvalidoException("inicio e fim devem ser instantes ISO-8601 com fuso horário.");
        }
    }

    private static int limite(String valor) {
        if (valor == null) return FiltroProdutosMaisVendidos.LIMITE_PADRAO;
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException ex) {
            throw new DadoInvalidoException("limite deve ser um número inteiro entre 1 e "
                    + FiltroProdutosMaisVendidos.LIMITE_MAXIMO + ".");
        }
    }
}
