package br.com.fatec.ecommerce.promocao.aplicacao;

import br.com.fatec.ecommerce.compartilhado.aplicacao.PublicarOperacaoAdministrativa;
import br.com.fatec.ecommerce.promocao.dominio.*;
import br.com.fatec.ecommerce.compartilhado.dominio.*;
import java.time.Clock;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Publica configuração/versão permitidas via Observer na transação original, sem credenciais. */
@Service @Transactional @PreAuthorize("hasAnyRole('ADMIN','PROMOCOES')")
public class GerenciarPromocoes {
    private final PublicarOperacaoAdministrativa auditoria;
    private final ConfiguracoesPromocionais repositorio;
    private final PromocaoFactoryRegistry fabricas;
    private final CondicaoRegistry condicoes;
    private final ValidarConfiguracaoComercial validar;
    private final Clock clock;
    public GerenciarPromocoes(ConfiguracoesPromocionais repositorio,PromocaoFactoryRegistry fabricas,
            CondicaoRegistry condicoes,ValidarConfiguracaoComercial validar,Clock clock, PublicarOperacaoAdministrativa auditoria) {
        this.auditoria = auditoria;
        this.repositorio=repositorio; this.fabricas=fabricas; this.condicoes=condicoes; this.validar=validar; this.clock=clock;
    }
    public CampanhaInfo criar(DadosCampanha d) {
        var c=new PromocaoConfig(d.nome(),d.tipo(),d.inicio(),d.fim(),d.prioridade(),d.modo(),d.parametros(),d.condicao(),clock.instant());
        conferir(c); var salvo = CampanhaInfo.de(repositorio.salvar(c));
        auditoria.publicar("CAMPANHA_CRIADA", "CAMPANHA", salvo.id(), null, auditoria.snapshot(salvo)); return salvo;
    }
    public CampanhaInfo alterar(Long id,DadosCampanha d) {
        var c=carregar(id); versao(c.versao(),d.versao()); var antes=auditoria.snapshot(CampanhaInfo.de(c));
        c.alterar(d.nome(),d.tipo(),d.inicio(),d.fim(),d.prioridade(),d.modo(),d.parametros(),d.condicao());
        conferir(c); var salvo=CampanhaInfo.de(repositorio.salvar(c));
        auditoria.publicar("CAMPANHA_ALTERADA", "CAMPANHA", id, antes, auditoria.snapshot(salvo)); return salvo;
    }
    public CampanhaInfo ativo(Long id,Long versao,boolean ativo) {
        var c=carregar(id); versao(c.versao(),versao); var antes=auditoria.snapshot(CampanhaInfo.de(c)); c.definirAtivo(ativo); var salvo=CampanhaInfo.de(repositorio.salvar(c));
        auditoria.publicar("CAMPANHA_ATIVACAO_ALTERADA", "CAMPANHA", id, antes, auditoria.snapshot(salvo)); return salvo;
    }
    @Transactional(readOnly=true) public CampanhaInfo consultar(Long id) { return CampanhaInfo.de(carregar(id)); }
    @Transactional(readOnly=true) public Pagina<CampanhaInfo> listar(int page,int size) {
        ValidarConfiguracaoComercial.pagina(page,size); var p=repositorio.promocoes(page,size);
        return new Pagina<>(p.conteudo().stream().map(CampanhaInfo::de).toList(),page,size,p.totalElementos());
    }
    public Map<String,List<String>> tipos() { return fabricas.tipos(); }
    public Map<String,List<String>> condicoes() { return condicoes.tipos(); }
    private PromocaoConfig carregar(Long id) { return repositorio.promocao(id).orElseThrow(()->new RecursoNaoEncontradoException("Promoção não encontrada.")); }
    private void conferir(PromocaoConfig c) { fabricas.criar(c); validar.referencias(c.parametros(),c.condicao()); }
    static void versao(Long atual,Long esperada) {
        if(!Objects.equals(atual,esperada)) { throw new ConflitoException("A configuração mudou. Consulte novamente."); }
    }
}
