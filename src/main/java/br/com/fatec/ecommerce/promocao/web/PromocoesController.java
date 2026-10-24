package br.com.fatec.ecommerce.promocao.web;

import br.com.fatec.ecommerce.promocao.aplicacao.*;
import br.com.fatec.ecommerce.compartilhado.dominio.Pagina;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.net.URI;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Fronteira administrativa; validacao de tipos e parametros vem dos registros do dominio. */
@RestController @RequestMapping("/api/admin/promocoes")
@PreAuthorize("hasAnyRole('ADMIN','PROMOCOES')") @Tag(name="Promocoes")
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="400",description="Configuração ou paginação inválida")
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="401",description="Autenticação necessária")
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="403",description="Exige ADMIN ou PROMOCOES")
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="404",description="Configuração inexistente")
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="409",description="Versão, código ou limites em conflito")
public class PromocoesController {
    public record Versao(@NotNull @PositiveOrZero Long versao) { }
    private final GerenciarPromocoes servico;
    public PromocoesController(GerenciarPromocoes servico) { this.servico=servico; }
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="201", description="Configuração criada",
            content=@io.swagger.v3.oas.annotations.media.Content(schema=@io.swagger.v3.oas.annotations.media.Schema(implementation=CampanhaInfo.class)))
    @PostMapping public ResponseEntity<CampanhaInfo> criar(@Valid @RequestBody DadosCampanha dados) {
        var criado=servico.criar(dados); return ResponseEntity.created(URI.create("/api/admin/promocoes/"+criado.id())).body(criado);
    }
    @PutMapping("/{id}") public CampanhaInfo alterar(@PathVariable Long id,@Valid @RequestBody DadosCampanha dados) { return servico.alterar(id,dados); }
    @GetMapping("/{id}") public CampanhaInfo consultar(@PathVariable Long id) { return servico.consultar(id); }
    @GetMapping public Pagina<CampanhaInfo> listar(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return servico.listar(page,size); }
    @GetMapping("/tipos") public Map<String,List<String>> tipos() { return servico.tipos(); }
    @GetMapping("/condicoes") public Map<String,List<String>> condicoes() { return servico.condicoes(); }
    @PostMapping("/{id}/ativacao") public CampanhaInfo ativar(@PathVariable Long id,@Valid @RequestBody Versao dados) { return servico.ativo(id,dados.versao(),true); }
    @PostMapping("/{id}/inativacao") public CampanhaInfo inativar(@PathVariable Long id,@Valid @RequestBody Versao dados) { return servico.ativo(id,dados.versao(),false); }
}
