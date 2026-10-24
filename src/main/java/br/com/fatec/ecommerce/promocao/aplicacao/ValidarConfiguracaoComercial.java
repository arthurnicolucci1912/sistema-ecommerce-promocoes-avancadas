package br.com.fatec.ecommerce.promocao.aplicacao;

import br.com.fatec.ecommerce.catalogo.aplicacao.ConsultarItensVendaveis;
import br.com.fatec.ecommerce.catalogo.dominio.CategoriaRepository;
import br.com.fatec.ecommerce.compartilhado.dominio.*;
import br.com.fatec.ecommerce.promocao.dominio.*;
import java.util.Map;
import org.springframework.stereotype.Service;

/** Referencias de catalogo sao verificadas na aplicacao; criadores do dominio cuidam dos parametros do tipo. */
@Service
public class ValidarConfiguracaoComercial {
    private final ConsultarItensVendaveis catalogo;
    private final CategoriaRepository categorias;
    public ValidarConfiguracaoComercial(ConsultarItensVendaveis catalogo,CategoriaRepository categorias) { this.catalogo=catalogo; this.categorias=categorias; }
    public void referencias(Map<String,Object> p,CondicaoConfig condicao) {
        if(p.containsKey("categoriaId") && categorias.buscarPorId(Parametros.id(p,"categoriaId")).isEmpty()) {
            throw Parametros.invalido("categoriaId inexistente");
        }
        if(p.containsKey("sku")) {
            try { catalogo.porSku(Parametros.texto(p,"sku")); }
            catch(RecursoNaoEncontradoException erro) { throw Parametros.invalido("sku inexistente"); }
        }
        if(condicao!=null) { referencias(condicao.parametros(),null); condicao.filhos().forEach(f->referencias(Map.of(),f)); }
    }
    public static void pagina(int page,int size) {
        if(page<0 || size<1 || size>100) { throw new DadoInvalidoException("Paginação inválida (size entre 1 e 100)."); }
    }
}
