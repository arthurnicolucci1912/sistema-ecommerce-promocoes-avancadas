package br.com.fatec.ecommerce.estoque.web;

import br.com.fatec.ecommerce.estoque.aplicacao.AjustarEstoque;
import br.com.fatec.ecommerce.estoque.aplicacao.ConsultarSaldo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Consulta e ajuste de estoque; a atomicidade e a recusa de saldo negativo ficam no caso de uso e no banco. */
@RestController
@RequestMapping("/api/admin/estoque")
@PreAuthorize("hasAnyRole('ADMIN', 'ESTOQUE')")
@Tag(name = "Estoque (administração)",
        description = "Exige ADMIN ou ESTOQUE. Variação sem saldo responde 404; ajuste que deixaria o disponível "
                + "negativo responde 409.")
public class EstoqueAdministrativoController {

    private final ConsultarSaldo consultar;
    private final AjustarEstoque ajustar;

    public EstoqueAdministrativoController(ConsultarSaldo consultar, AjustarEstoque ajustar) {
        this.consultar = consultar;
        this.ajustar = ajustar;
    }

    @GetMapping("/{variacaoId}")
    @Operation(summary = "Consulta o saldo de uma variação")
    public SaldoResponse consultar(@PathVariable Long variacaoId) {
        return SaldoResponse.de(consultar.porVariacao(variacaoId));
    }

    @GetMapping
    @Operation(summary = "Lista o estoque baixo",
            description = "Variações com disponível estritamente abaixo do limite informado, em ordem de variação.")
    public List<SaldoResponse> abaixoDe(@RequestParam int abaixoDe) {
        return consultar.abaixoDe(abaixoDe).stream().map(SaldoResponse::de).toList();
    }

    @PostMapping("/{variacaoId}/ajustes")
    @Operation(summary = "Ajusta o disponível de uma variação",
            description = "Quantidade positiva soma e negativa subtrai; motivo obrigatório; zero é rejeitado (400).")
    public SaldoResponse ajustar(@PathVariable Long variacaoId, @Valid @RequestBody AjusteDeEstoqueRequest request) {
        return SaldoResponse.de(ajustar.ajustar(variacaoId, request.quantidade(), request.motivo()));
    }
}
